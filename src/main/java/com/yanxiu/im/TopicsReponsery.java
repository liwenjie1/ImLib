package com.yanxiu.im;

import android.content.Context;
import android.support.annotation.UiThread;
import android.text.TextUtils;
import android.util.Log;

import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.bean.net_bean.ImMember_new;
import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.im.business.topiclist.sorter.ImTopicSorter;
import com.yanxiu.im.business.utils.ImServerDataChecker;
import com.yanxiu.im.business.utils.ProcessUtils;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.db.DbTopic;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.manager.HttpRequestManager;
import com.yanxiu.im.manager.MqttConnectManager;
import com.yanxiu.im.manager.RequestQueueManager;

import java.util.ArrayList;
import java.util.List;

/**
 * create by 朱晓龙 2018/8/23 下午4:28
 * 单例仓库 维持 位移数据对象供上层调用
 * 1、初步为了 整合 msglistactivity 获取 topicitem 对象
 */
public class TopicsReponsery {

    private final String TAG = getClass().getSimpleName();
    private static TopicsReponsery INSTANCE;

    public static TopicsReponsery getInstance() {
        if (INSTANCE == null) INSTANCE = new TopicsReponsery();

        return INSTANCE;
    }



    /*mqtt 服务连接状态*/

    private TopicsReponsery() {
        final Context context = ImApplication.getContext().getApplicationContext();
        final String processName = ProcessUtils.getProcessName(context);
        Log.i("TopicsReponsery", "getInstance: " + processName);
        mQueueManager = new RequestQueueManager();

        needUpdateMemberTopics = new ArrayList<>();
        needUpdateMsgTopics = new ArrayList<>();
        mHttpRequestManager = new HttpRequestManager();

    }


    public void releaseResource() {
        MqttConnectManager.getInstance().disconnectMqttServer();
        if (topicInMemory != null) {
            topicInMemory.clear();
        }
        INSTANCE = null;
    }


    private ArrayList<TopicItemBean> topicInMemory = new ArrayList<>();

    /**
     * 优化请求队列
     */
    private ArrayList<TopicItemBean> needUpdateMemberTopics;
    private ArrayList<TopicItemBean> needUpdateMsgTopics;

    private RequestQueueManager mQueueManager;

    private HttpRequestManager mHttpRequestManager;

    private android.os.Handler uiHandler = new android.os.Handler();

    /**
     * 将 topiclist 加入内存中
     */
    private void addAllToMemory(ArrayList<TopicItemBean> topicItemBeans) {
        synchronized (this) {
            if (topicInMemory == null) {
                topicInMemory = new ArrayList<>();
            }

            if (topicItemBeans == null || topicItemBeans.size() == 0) {
                return;
            }
            ArrayList<TopicItemBean> toAdd = new ArrayList<>();
            for (TopicItemBean addBean : topicItemBeans) {
                boolean has = false;
                for (TopicItemBean memoryBean : topicInMemory) {
                    if (memoryBean.getTopicId() == addBean.getTopicId()) {
                        has = true;
                        break;
                    }
                }
                if (!has) {
                    toAdd.add(addBean);
                }
            }
            topicInMemory.addAll(toAdd);
        }
    }

    private void addToMemory(TopicItemBean bean) {
        synchronized (this) {
            if (topicInMemory == null) {
                topicInMemory = new ArrayList<>();
            }
            topicInMemory.add(bean);
            ImTopicSorter.sortByLatestTime(topicInMemory);
        }
    }

    private void deleteFromMemory(TopicItemBean bean) {
        synchronized (this) {
            if (topicInMemory == null) {
                return;
            }
            TopicInMemoryUtils.removeTopicFromListById(bean.getTopicId(), topicInMemory);
        }
    }

    private void deleteFromMemory(ArrayList<TopicItemBean> beans) {
        for (TopicItemBean bean : beans) {
            deleteFromMemory(bean);
        }
    }

    public ArrayList<TopicItemBean> getTopicInMemory() {
        return topicInMemory;
    }

    public ArrayList<TopicItemBean> getLocalTopicList(long imId) {
        //异步获取 数据库数据 topic列表

        final List<TopicItemBean> fromDb = (ArrayList<TopicItemBean>) DatabaseManager.topicsFromDb();
        addAllToMemory((ArrayList<TopicItemBean>) fromDb);
        return topicInMemory;
    }

    /**
     * 请求获取服务器最新的 topic list
     * 新的 topic list 需要的信息 为 最新的 member 信息  + 最新的 msg 信息
     */
    public void getServerTopicList(final String imToken, final TopicListUpdateCallback<TopicItemBean> callback) {
        //清空 更新对象列表
        needUpdateMemberTopics.clear();
        needUpdateMsgTopics.clear();

        mHttpRequestManager.requestUserTopicList(imToken, new HttpRequestManager.GetTopicListCallback<ImTopic_new>() {
            @Override
            public void onGetTopicList(List<ImTopic_new> topicList) {
                //查找已经删除的
                ArrayList<TopicItemBean> toBeDel = new ArrayList<>();
                for (TopicItemBean localBean : topicInMemory) {
                    boolean has = false;
                    for (ImTopic_new imTopicNew : topicList) {
                        if (imTopicNew.topicId == localBean.getTopicId()) {
                            has = true;
                            break;
                        }
                    }
                    //服务器上不存在 并且 也不是 mocktopic
                    if (!has && !DatabaseManager.isMockTopic(localBean)) {
                        DatabaseManager.deleteTopicById(localBean.getTopicId());
                        toBeDel.add(localBean);
                    }
                }
                deleteFromMemory(toBeDel);
                Log.i(TAG, "onSuccess: 完成列表内删除 -" + toBeDel.size());

                //查找新的 和需要更新的 这里没有 member 信息  没办法进行 mock 合并 所以合并放在 member 信息获取中
                for (ImTopic_new imTopicNew : topicList) {
                    //保存到数据库
                    final TopicItemBean savedBean = DatabaseManager.updateDbTopicWithImTopic(imTopicNew);
                    boolean has = false;
                    for (TopicItemBean localTopic : topicInMemory) {
                        if (localTopic.getTopicId() == imTopicNew.topicId) {
                            has = true;
                            //是否有 member 更新
                            if (isMemberUpdate(imTopicNew, localTopic)) {
                                needUpdateMemberTopics.add(localTopic);
                            }
                            //检查是否有 msg 更新
                            if (isMsgUpdate(imTopicNew, localTopic)) {
                                needUpdateMsgTopics.add(localTopic);
                            }
                            //更新已有的
                            updateBeanInfo(localTopic, savedBean);
                            break;
                        }
                    }
                    if (!has) {
                        //添加新增的
                        addToMemory(savedBean);
                        //新的 topic member 与 msg 都需要更新
                        needUpdateMsgTopics.add(savedBean);
                        needUpdateMemberTopics.add(savedBean);
                    }
                }
                Log.i(TAG, "onSuccess: 完成列表内新增和更新 ");
                /*对列表的长度以及 信息 更新已经完成 可以回调 给 ui 列表更新完毕*/
                Log.i(TAG, "onGetTopicList: 回调 UI 更新列表显示");
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onListUpdated(topicInMemory);
                    }
                });
            }

            @Override
            public void onGetFailure() {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onListUpdated(topicInMemory);
                    }
                });
            }
        });
    }

    private boolean isMsgUpdate(ImTopic_new imTopicNew, TopicItemBean localTopic) {
        final long lid = localTopic.getLatestMsgId();
        final long sid = imTopicNew.latestMsgId;
        return lid < sid;
    }

    private boolean isMemberUpdate(ImTopic_new imTopicNew, TopicItemBean localTopic) {
        if (localTopic.getMembers() == null || localTopic.getMembers().size() < 2) {
            //如果本地没有 member 信息
            return true;
        }
        final String lc = localTopic.getChange();
        final String sc = imTopicNew.topicChange;

        final int localChange = Integer.parseInt(lc);
        final int serverChange = Integer.parseInt(sc);

        return localChange < serverChange;
    }


    /**
     * 新加入 topic
     * 首先本地 检查是否已经加入目标 topic
     * 然后 向服务器请求 topic 信息
     */
    public void addToTopic(long topicId, final AddToTopicCallback<TopicItemBean> callback) {
        //首先检查本地是否已经存在 realtopic
        TopicItemBean targetTopic = getTopicFromMemory(topicId);
        if (targetTopic != null) {
            //本地已经有了 说明已经添加过 那就是  有新的 member 用户被添加 是否需要跟新 member 信息？
            return;
        }
        //网络请求 topic 信息  withmembers
        mHttpRequestManager.requestTopicInfo(Constants.imToken, topicId + "", new HttpRequestManager.GetTopicInfoCallback<ImTopic_new>() {
            @Override
            public void onGetTopicInfo(ImTopic_new data) {
                //获取到 imTopic 信息  with members
                TopicItemBean resultTopic = mergeOrInsertTopic(data);
                //新加入的
                addToMemory(resultTopic);
                //请求消息列表
                requestLastestMsgPageFromServer(resultTopic, new GetTopicItemBeanCallback() {
                    @Override
                    public void onGetTopicItemBean(final TopicItemBean bean) {
                        if (callback != null) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onAdded(bean);
                                }
                            });
                        }
                    }
                });
            }

            @Override
            public void onRequestFailure(final String msg) {
                if (callback != null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(msg);
                        }
                    });

                }
            }
        });


    }

    /**
     * 检查 imtopic 是否能与本地的 mocktopic 合并
     * 能合并 返回 原有 topicBean 不能合并 返回新插入的 topicbean
     */
    private TopicItemBean mergeOrInsertTopic(ImTopic_new data) {
        boolean hasMerged = false;
        TopicItemBean resultTopic = null;
        if (TextUtils.equals("1", data.topicType)) {
            //如果是私聊
            for (TopicItemBean localTopic : topicInMemory) {
                if (DatabaseManager.isMockTopic(localTopic)) {
                    long otherMemberId = -1;
                    for (DbMember mockMember : localTopic.getMembers()) {
                        if (mockMember.getImId() != Constants.imId) {
                            otherMemberId = mockMember.getImId();
                            break;
                        }
                    }
                    long imMemberId = -2;
                    for (ImTopic_new.Member imMember : data.members) {
                        if (imMember.memberId != Constants.imId) {
                            imMemberId = imMember.memberId;
                            break;
                        }
                    }
                    if (otherMemberId == imMemberId) {
                        hasMerged = true;
                        DatabaseManager.migrateMockTopicToRealTopic(localTopic, data);
                        resultTopic = localTopic;
                    }
                }
            }
        }
        if (!hasMerged) {//如果不是 mocktopic 进行新的 topic 插入操作
            //保存数据库
            resultTopic = DatabaseManager.updateDbTopicWithImTopic(data);
        }
        return resultTopic;
    }

    public interface AddToTopicCallback<E> {
        void onAdded(E topicBean);

        void onFailure(String msg);
    }


    public interface TopicListUpdateCallback<E> {
        void onListUpdated(ArrayList<E> dataList);
    }


    /**
     * 获取本地 topic by topicid
     * 首先内存  其次 数据库
     */
    public void getLocalTopic(long topicId, GetTopicItemBeanCallback callback) {
        //首先 在内存中获取
        TopicItemBean resultTopic = getTopicFromMemory(topicId);
        //如果内存没有 尝试 重新读取数据库？
        if (resultTopic == null) {
            resultTopic = getTopicFromDb(topicId);
        }
        //如果 通过 内存或 数据库获取到了目标 topic 回调结果
        callback.onGetTopicItemBean(resultTopic);
    }

    /**
     * 内存获取指定 topicBean
     */
    private TopicItemBean getTopicFromMemory(long topicId) {
        final TopicItemBean topicByTopicId = TopicInMemoryUtils.findTopicByTopicId(topicId, topicInMemory);
        return topicByTopicId;
    }

    /**
     * 数据库获取指定 topicbean
     */
    public TopicItemBean getTopicFromDb(long topicId) {
        final DbTopic dbTopic = DatabaseManager.getTopicById(topicId);
        if (dbTopic == null) {
            return null;
        }
        final TopicItemBean topicItemBean = DatabaseManager.changeDbTopicToTopicItemBean(dbTopic);
        return topicItemBean;
    }

    /**
     * 创建一个临时 topicBean
     */
    public TopicItemBean createTempTopicBean(long topicId, String topicName) {
        TopicItemBean tempBean = new TopicItemBean();
        tempBean.setTopicId(topicId);
        tempBean.setGroup(topicName);
        tempBean.setMsgList(new ArrayList<MsgItemBean>());
        tempBean.setMembers(new ArrayList<DbMember>());
        tempBean.setChange("0");
        DatabaseManager.updateTopicWithTopicItemBean(tempBean);
        return tempBean;
    }


    /**
     * 更新所有当前 topic 的 member 与 msg
     */
    public void updateAllTopicInfo(final GetTopicItemBeanCallback callback) {
        if (topicInMemory != null && topicInMemory.size() > 0) {
            for (TopicItemBean bean : topicInMemory) {
                updateTopicInfo(bean, callback);
            }
        }
    }

    /**
     * 对一个 topicbean 进行数据更新
     */
    public void updateTopicInfo(final TopicItemBean bean, final GetTopicItemBeanCallback callback) {
        //数据更新一定请求服务器数据
        if (bean == null) {
            callback.onGetTopicItemBean(null);
            return;
        }
        //网络请求 指定 topic 的 member 信息
        if (!checkShouldUpdateMember(bean, callback)) {
            Log.i(TAG, "requestTopicMemberInfoFromServer: 不需要更新 member");
            //检查 msg 是否需要更新
            if (checkShouldUpdateMsg(bean, callback)) {
                requestLastestMsgPageFromServer(bean, callback);
            } else {
                callback.onGetTopicItemBean(bean);
            }
        } else {//需要更新 member
            requestTopicMemberInfoFromServer(bean, new GetTopicItemBeanCallback() {
                @Override
                public void onGetTopicItemBean(TopicItemBean beanCreateFromDB) {
                    Log.i(TAG, "onGetTopicItemBean: ");
                    if (beanCreateFromDB != null) {
                        //更新 target 的 info
                        updateMemberInfo(bean, beanCreateFromDB);
                        updateBeanInfo(bean, beanCreateFromDB);
                        //获取最新 member 信息后 通知更新
                        //继续请求 msg 信息 之后回调 ui
                        if (checkShouldUpdateMsg(bean, callback)) {
                            requestLastestMsgPageFromServer(bean, callback);
                        }
                    }
                }
            });
        }
    }

    /**
     * 获取 topic 的 member 列表
     */
    public void updateTopicMemberInfoFromServer(final TopicItemBean bean, final GetTopicItemBeanCallback callback) {
        requestTopicMemberInfoFromServer(bean, new GetTopicItemBeanCallback() {
            @Override
            public void onGetTopicItemBean(TopicItemBean beanCreateFromDB) {
                if (beanCreateFromDB != null) {
                    //更新 target 的 info
                    updateMemberInfo(bean, beanCreateFromDB);
                    updateBeanInfo(bean, beanCreateFromDB);
                    callback.onGetTopicItemBean(bean);
                }
            }
        });
    }


    private void updateMemberInfo(TopicItemBean target, TopicItemBean infoBean) {
        target.getMembers().clear();
        target.getMembers().addAll(infoBean.getMembers());
    }

    /**
     * 用 infoBean 对 target 进行内容的更新
     */
    private void updateBeanInfo(TopicItemBean target, TopicItemBean infoBean) {
        target.setTopicId(infoBean.getTopicId());
        target.setGroup(infoBean.getGroup());
        target.setChange(infoBean.getChange());
        target.setTopicId(infoBean.getTopicId());
        target.setType(infoBean.getType());
        target.setLatestMsgTime(infoBean.getLatestMsgTime());
        target.setLatestMsgId(infoBean.getLatestMsgId());
        //免打扰和禁言
        target.setSilence(infoBean.isSilence());
        target.setBlockNotice(infoBean.isBlockNotice());
        //删除历史记录标志
        target.setLatestMsgIdWhenDeletedLocalTopic(infoBean.getLatestMsgIdWhenDeletedLocalTopic());
        target.setAlreadyDeletedLocalTopic(infoBean.isAlreadyDeletedLocalTopic());
    }


    /**
     * 本地没有 指定 topic 数据
     * 向服务器请求
     * 如果返回了 topic 数据
     */
    private void requestTopicMemberInfoFromServer(final TopicItemBean bean, final GetTopicItemBeanCallback callback) {
        //member 是否有变化？  有变化的 topic 保存在 updatemember 集合中 检查是否在记录中

        Log.i(TAG, "requestTopicMemberInfoFromServer: 需要更新 member");

        mHttpRequestManager.requestTopicMemberList(Constants.imToken, Long.toString(bean.getTopicId()), new HttpRequestManager.GetTopicMemberListCallback<ImTopic_new>() {
            @Override
            public void onGetTopicMembers(ImTopic_new topicWithMembers) {
                Log.i(TAG, "onGetTopicMembers: ");
                //本地一定存在 topic 只获取更新的数据载体
                final TopicItemBean resultTopic = DatabaseManager.updateDbTopicWithImTopic(topicWithMembers);
                synchronized (needUpdateMemberTopics) {
                    needUpdateMemberTopics.remove(bean);
                }
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onGetTopicItemBean(resultTopic);
                    }
                });


            }

            @Override
            public void onGetFailure() {
                Log.i(TAG, "topic memberonGetFailure: ");
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onGetTopicItemBean(null);
                    }
                });
            }
        });
    }

    private boolean checkShouldUpdateMember(TopicItemBean bean, GetTopicItemBeanCallback callback) {
        if (needUpdateMemberTopics == null) {
            return true;
        }
        return needUpdateMemberTopics.contains(bean);
    }

    /**
     * 获取最新一页 msg
     */
    public void requestLastestMsgPageFromServer(final TopicItemBean itemBean, final GetTopicItemBeanCallback callback) {
        itemBean.setShowDot(true);
        //请求最新一页  直接设置 最大 Long.value
        mHttpRequestManager.requestTopicMsgList(Constants.imToken, Long.MAX_VALUE, itemBean.getTopicId(), new HttpRequestManager.GetTopicMsgListCallback<ImMsg_new>() {
            @Override
            public void onGetTopicMsgList(List<ImMsg_new> msgList) {
                Log.i(TAG, "onGetTopicMsgList: ");
                ArrayList<MsgItemBean> msgPages = new ArrayList<>();
                for (ImMsg_new imMsgNew : msgList) {
                    //获取 msglist 后  首先  保存数据库
                    final MsgItemBean msgItemBean = DatabaseManager.updateDbMsgWithImMsg(imMsgNew, Constants.imId);
//                    msgPages.add(msgItemBean);
                }
                final ArrayList<MsgItemBean> dbMsgs = DatabaseManager.getTopicMsgs(itemBean.getTopicId(), DatabaseManager.minMsgId, DatabaseManager.pagesize);
                msgPages.addAll(dbMsgs);

                List<MsgItemBean> msgBean = itemBean.getMsgList();
                if (msgBean == null) {
                    msgBean = new ArrayList<>();
                    itemBean.setMsgList(msgBean);
                }
                TopicInMemoryUtils.duplicateRemoval(msgPages, itemBean.getMsgList());
                itemBean.getMsgList().addAll(msgPages);

                //保存红点状态
                DatabaseManager.updateTopicWithTopicItemBean(itemBean);
                //在记录列表中移除
                synchronized (needUpdateMsgTopics) {
                    needUpdateMsgTopics.remove(itemBean);
                }
                /*本地 mock topic 检查 此时 topic 已经被加入到 列表当中了*/

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onGetTopicItemBean(itemBean);
                    }
                });
            }

            @Override
            public void onGetFailure(String msg) {
                Log.i(TAG, "onGetLatestMsgPageFailure: " + msg);
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onGetTopicItemBean(null);
                    }
                });
            }
        });
    }

    private boolean checkShouldUpdateMsg(TopicItemBean itemBean, GetTopicItemBeanCallback callback) {
        if (needUpdateMsgTopics == null) {
            return true;
        }

        return needUpdateMsgTopics.contains(itemBean);
    }

    /**
     * 通过 member 获取目标 topicitembean
     */
    public void getPrivateTopicByMemberid(long memberId, long fromTopic, GetPrivateTopicCallback<TopicItemBean> callback) {
        TopicItemBean resultBean = null;
        //内存检查
        resultBean = TopicInMemoryUtils.findPrivateTopicByMemberId(memberId, topicInMemory);
        //数据库检查
        if (resultBean == null) {
            final List<TopicItemBean> dbTopics = DatabaseManager.topicsFromDb();
            resultBean = TopicInMemoryUtils.findPrivateTopicByMemberId(memberId, dbTopics);
        }
        if (resultBean == null) {
            //创建 mocktopic 设计是在发送消息的时候才会进行 mocktopic 的创建 所以这里不能创建
            //没有找到目标私聊  为了显示 ui 获取 member 信息 本地可能没有 member 信息
            final DbMember memberById = DatabaseManager.getMemberById(memberId);
            if (memberById != null) {
                callback.onNoTargetTopic(memberById.getName());
            } else {
                callback.onNoTargetTopic(null);
            }
        } else {
            callback.onFindRealPrivateTopic(resultBean);
        }
    }

    public interface GetPrivateTopicCallback<E> {
        void onFindRealPrivateTopic(E bean);

        void onNoTargetTopic(String memberName);
    }

    /**
     * 在本地 彻底清除 topic 信息
     */
    public TopicItemBean removeTopic(long topicId) {
        //首先判断是否存在
        final TopicItemBean topicFromMemory = getTopicFromMemory(topicId);
        final DbTopic topicById = DatabaseManager.getTopicById(topicId);
        TopicItemBean removedTopic = null;
        if (topicFromMemory != null) {
            removedTopic = topicFromMemory;
            topicInMemory.remove(topicFromMemory);
        }
        if (topicById != null) {
            DatabaseManager.deleteTopicById(topicId);
        }
        return removedTopic;
    }


    /**
     * 执行删除 topic 操作
     */
    public void deleteTopicHistory(TopicItemBean topicItemBean, final DeleteTopicCallback callback) {
        //删除步骤 1、请求服务器删除 topic成功后  2、 删除数据库 3、同步内存（删除内存中的实例）
        //服务器删除成功
        DatabaseManager.deleteLocalMsgByTopicId(topicItemBean);
        callback.onTopicDeleted();
        topicItemBean.setDeleteFlag();
        TopicInMemoryUtils.removeTopicFromListById(topicItemBean.getTopicId(), topicInMemory);
    }

    public interface DeleteTopicCallback {
        @UiThread
        void onTopicDeleted();
    }

    public interface GetTopicItemBeanCallback {
        @UiThread
        void onGetTopicItemBean(TopicItemBean bean);
    }

    /**
     * 加载一页
     * 下拉加载
     */
    public void loadPageMsg(final TopicItemBean targetTopic, final long startId, final GetMsgPageCallback callback) {
        Log.i(TAG, "loadPageMsg: ");
        //首先有网络获取
        mHttpRequestManager.requestTopicMsgList(Constants.imToken, startId, targetTopic.getTopicId(), new HttpRequestManager.GetTopicMsgListCallback<ImMsg_new>() {
            @Override
            public void onGetTopicMsgList(List<ImMsg_new> msgList) {
                Log.i(TAG, "onGetTopicMsgList: ");
                //保存入数据库
                for (ImMsg_new msgNew : msgList) {
                    /*检查 有服务器返回的msg 数据格式 防止空指针*/
                    if (ImServerDataChecker.imMsgCheck(msgNew)) {
                        DatabaseManager.updateDbMsgWithImMsg(msgNew, Constants.imId);
                    }
                }
                final ArrayList<MsgItemBean> topicMsgs = DatabaseManager.getTopicMsgs(targetTopic.getTopicId(), startId, DatabaseManager.pagesize);
                TopicInMemoryUtils.duplicateRemoval(topicMsgs, targetTopic.getMsgList());
                targetTopic.getMsgList().addAll(topicMsgs);
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onGetPage(topicMsgs);
                    }
                });
            }

            @Override
            public void onGetFailure(String msg) {
                Log.i(TAG, "onGetMsgListFailure: " + msg);
            }
        });
    }

    public interface GetMsgPageCallback {
        void onGetPage(ArrayList<MsgItemBean> msgs);
    }

    /**
     * 创建一个 mocktopic
     */
    public TopicItemBean createMockTopic(long fromId, long memberId, String memberName) {
        Log.i(TAG, "createMockTopic: ");
        //首先 检查目标 member 是否存在数据库中
        DbMember mockMember = DatabaseManager.getMemberById(memberId);
        if (mockMember == null) {
            DatabaseManager.createMockMemberForMockTopic(memberId, memberName);
        }
        final TopicItemBean mockTopic = DatabaseManager.createMockTopic(memberId, fromId);
        mockTopic.setMsgList(new ArrayList<MsgItemBean>());

        addToMemory(mockTopic);
        ImTopicSorter.sortByLatestTime(topicInMemory);
        return mockTopic;
    }

    public interface CreateTopicCallback {
        void onTopicCreatedSuccess(TopicItemBean topicItemBean);

        void onTopicCreateFailed();
    }

    /**
     * 获取 member 信息 1、网络获取 2、本地获取
     */
    public void getImMemberInfo(long memberId, final GetMemberInfoCallback<DbMember> callback) {
        Log.i(TAG, "getImMemberInfo: ");
        //首先从 db 获取
        final DbMember memberById = DatabaseManager.getMemberById(memberId);
        if (memberById != null) {
            //本地有数据
            callback.onGetMemberInfo(memberById);
        } else {
            //本地没有数据 网络获取
            mHttpRequestManager.requestMemberInfo(memberId, new HttpRequestManager.RequestMemberInfoCallback<ImMember_new>() {
                @Override
                public void onGetMemberInfo(ImMember_new info) {
                    //服务器获取了 member
                    Log.i(TAG, "onGetMemberInfo: ");
                    //更新数据库
                    final DbMember dbMember = DatabaseManager.updateDbMemberWithImMember(info);
                    if (callback != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onGetMemberInfo(dbMember);
                            }
                        });
                    }
                }

                @Override
                public void onGetMemberInfoFailure(String msg) {
                    //服务器获取 member 信息失败
                    Log.i(TAG, "onGetMemberInfoFailure: ");
                    if (callback != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (memberById != null) {
                                    callback.onGetMemberInfo(memberById);
                                } else {
                                    callback.onGetMemberInfoFailure("没有找到 member 信息");
                                }
                            }
                        });
                    }
                }
            });
        }

    }

    public interface GetMemberInfoCallback<E> {
        void onGetMemberInfo(E data);

        void onGetMemberInfoFailure(String msg);
    }


    /**
     * 向服务器请求创建一个新的聊天 topic
     */
    public void createNewTopic(final TopicItemBean mockTopic, String fromTopicId, final CreateTopicCallback callback) {
        Log.i(TAG, "createNewTopic: ");
        //获取 member 信息
        long memberId = -1;
        for (DbMember memberNew : mockTopic.getMembers()) {
            if (memberNew.getImId() != Constants.imId) {
                memberId = memberNew.getImId();
                break;
            }
        }
        requestCreateTopic(mockTopic, fromTopicId, memberId, callback);
    }

    private void requestCreateTopic(final TopicItemBean mockTopic, String fromTopicId, long memberId, final CreateTopicCallback callback) {
        Log.i(TAG, "requestCreateTopic: ");
        mHttpRequestManager.requestCreateNewPrivateTopic(Constants.imToken, fromTopicId, memberId, Constants.imId, new HttpRequestManager.CreatePrivateTopicCallback<ImTopic_new>() {
            @Override
            public void onCreated(ImTopic_new topic) {
                Log.i(TAG, "onCreated: ");
                //创建成功 返回完整的 topic 信息 这里进行 mocktopic 的合并操作 这里的信息不包含 msg 列表  与 获取更新 topic member 信息的接口类似
                DatabaseManager.migrateMockTopicToRealTopic(mockTopic, topic);
                //回调给上层
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onTopicCreatedSuccess(mockTopic);
                    }
                });
            }

            @Override
            public void onFailure() {
                //创建失败 修改数据库状态
                DatabaseManager.topicCreateFailed(mockTopic);
                if (callback != null) {
                    callback.onTopicCreateFailed();
                }
            }
        });
    }


    /**
     * 请求更改 topic 的 配置
     * 禁言
     */

    public void updatePublicConfig(final TopicItemBean bean, int speak, final UpdateConfigCallback<TopicItemBean> callback) {
        Log.i(TAG, "updatePublicConfig: ");
        mHttpRequestManager.requestUpdatePublicConfig(bean.getTopicId(), speak, new HttpRequestManager.UpdateTopicConfigCallback<ImTopic_new>() {
            @Override
            public void onUpdated(ImTopic_new imTopic) {
                //请求成功返回 imtopic 的最新 info
                Log.i(TAG, "onUpdated: ");
                //更新数据库
                final TopicItemBean infoBean = DatabaseManager.updateDbTopicWithImTopic(imTopic);
                //更新内存
                updateBeanInfo(bean, infoBean);
                callback.onTopicConfigUpdated(bean);
            }

            @Override
            public void onFilure(String msg) {
                Log.i(TAG, "onUpdatePublicConfigFilure: ");
            }
        });
    }

    /**
     * 请求更改 topic 个人设置
     * 免打扰
     */
    public void updatePersonalConfig(final TopicItemBean bean, int quite, final UpdateConfigCallback<TopicItemBean> callback) {
        mHttpRequestManager.requestUpdatePersonalConfig(bean.getTopicId(), quite, new HttpRequestManager.UpdateTopicConfigCallback<ImTopic_new>() {
            @Override
            public void onUpdated(ImTopic_new imTopic) {
                //请求成功返回 imtopic 的最新 info
                Log.i(TAG, "onUpdated: ");
                //更新数据库
                final TopicItemBean infoBean = DatabaseManager.updateDbTopicWithImTopic(imTopic);
                //更新内存
                updateBeanInfo(bean, infoBean);
                callback.onTopicConfigUpdated(bean);
            }

            @Override
            public void onFilure(String msg) {
                Log.i(TAG, "onUpdatePersonalConfigFilure: " + msg);
            }
        });
    }

    public interface UpdateConfigCallback<E> {
        void onTopicConfigUpdated(E topicBean);

    }


    private boolean checkAndMergeMockTopic(ImTopic_new imTopicNew) {
        return false;
    }

}
