package com.yanxiu.im;

import android.content.Context;
import android.support.annotation.UiThread;
import android.util.Log;

import com.test.yanxiu.common_base.utils.SharedSingleton;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.im.business.topiclist.sorter.ImTopicSorter;
import com.yanxiu.im.business.utils.ProcessUtils;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.db.DbTopic;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.manager.RequestQueueManager;
import com.yanxiu.im.net.GetTopicMsgsRequest_new;
import com.yanxiu.im.net.GetTopicMsgsResponse_new;
import com.yanxiu.im.net.TopicCreateTopicRequest_new;
import com.yanxiu.im.net.TopicCreateTopicResponse_new;
import com.yanxiu.im.net.TopicGetMemberTopicsResponse_new;
import com.yanxiu.im.net.TopicGetTopicsResponse_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;

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

    public TopicsReponsery() {
        final Context context = ImApplication.getContext().getApplicationContext();
        final String processName = ProcessUtils.getProcessName(context);
        Log.i("TopicsReponsery", "getInstance: " + processName);
        mQueueManager = new RequestQueueManager();

        needUpdateMemberTopics = new ArrayList<>();
        needUpdateMsgTopics = new ArrayList<>();

    }

    private ArrayList<TopicItemBean> topicInMemory;

    /**
     * 优化请求队列
     */
    private ArrayList<TopicItemBean> needUpdateMemberTopics;
    private ArrayList<TopicItemBean> needUpdateMsgTopics;

    private RequestQueueManager mQueueManager;

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
        DatabaseManager.useDbForUser(Long.toString(imId) + "_db");//todo:应该放在config里面去
        final List<TopicItemBean> fromDb = (ArrayList<TopicItemBean>) DatabaseManager.topicsFromDb();
        addAllToMemory((ArrayList<TopicItemBean>) fromDb);
        return topicInMemory;
    }

    /**
     * 请求获取服务器最新的 topic list
     * 新的 topic list 需要的信息 为 最新的 member 信息  + 最新的 msg 信息
     */
    public void getServerTopicList(final String imToken, final TopicListUpdateCallback<TopicItemBean> callback) {
        needUpdateMemberTopics.clear();
        needUpdateMsgTopics.clear();

        com.yanxiu.im.net.TopicGetMemberTopicsRequest_new getMemberTopicsRequest = new com.yanxiu.im.net.TopicGetMemberTopicsRequest_new();
        getMemberTopicsRequest.imToken = imToken;
        mQueueManager.addRequest(getMemberTopicsRequest, TopicGetMemberTopicsResponse_new.class, new IYXHttpCallback<TopicGetMemberTopicsResponse_new>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, TopicGetMemberTopicsResponse_new ret) {
                if (ret.code != 0 || ret.data == null || ret.data.topic == null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onListUpdated(topicInMemory);
                        }
                    });
                    return;
                }
                Log.i(TAG, "onSuccess: 获取服务器最新 topic 列表成功");
                //获取了服务器上最新的 topic 列表后
                /*1：对 topic 进行划分 分为  新添加  更新  已删除*/

                //查找新的 和需要更新的
                for (ImTopic_new imTopicNew : ret.data.topic) {
                    final TopicItemBean savedBean = DatabaseManager.updateDbTopicWithImTopic(imTopicNew);
                    boolean has = false;
                    for (TopicItemBean localTopic : topicInMemory) {
                        if (localTopic.getTopicId() == imTopicNew.topicId) {
                            has = true;
                            //更新已有的
                            updateBeanInfo(localTopic, savedBean);
                            break;
                        }
                    }
                    if (!has) {
                        //添加新增的
                        addToMemory(savedBean);
                        needUpdateMsgTopics.add(savedBean);
                        needUpdateMemberTopics.add(savedBean);
                    }
                }
                Log.i(TAG, "onSuccess: 完成列表内新增和更新 ");
                //查找已经删除的
                ArrayList<TopicItemBean> toBeDel = new ArrayList<>();
                for (TopicItemBean localBean : topicInMemory) {
                    boolean has = false;
                    for (ImTopic_new imTopicNew : ret.data.topic) {
                        if (imTopicNew.topicId == localBean.getTopicId()) {
                            has = true;
                            break;
                        }
                    }
                    if (!has) {
                        DatabaseManager.deleteTopicById(localBean.getTopicId());
                        toBeDel.add(localBean);
                    }
                }
                deleteFromMemory(toBeDel);
                Log.i(TAG, "onSuccess: 完成列表内删除 -" + toBeDel.size());
                /*对列表的长度以及 信息 更新已经完成 可以回调 给 ui 列表更新完毕*/
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onListUpdated(topicInMemory);
                    }
                });

            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
            }
        });
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
        final ArrayList<TopicItemBean> topics = SharedSingleton.getInstance().<ArrayList<TopicItemBean>>get(SharedSingleton.KEY_TOPIC_LIST);
        if (topics == null) {
            return null;
        }
        final TopicItemBean topicByTopicId = TopicInMemoryUtils.findTopicByTopicId(topicId, topics);
        return topicByTopicId;
    }

    /**
     * 数据库获取指定 topicbean
     */
    private TopicItemBean getTopicFromDb(long topicId) {
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

        requestTopicMemberInfoFromServer(bean, new GetTopicItemBeanCallback() {
            @Override
            public void onGetTopicItemBean(TopicItemBean beanCreateFromDB) {
                Log.i(TAG, "onGetTopicItemBean: ");
                if (beanCreateFromDB != null) {
                    //更新 target 的 info
                    updateMemberInfo(bean, beanCreateFromDB);
                    callback.onGetTopicItemBean(bean);
                    //
                }
                //继续请求 msg 信息 之后回调 ui
                requestLastestMsgPageFromServer(bean, callback);
            }
        });
    }


    private void updateMemberInfo(TopicItemBean target, TopicItemBean infoBean){
        target.getMembers().clear();
        target.getMembers().addAll(infoBean.getMembers());
    }
    /**
     * 用 infoBean 对 target 进行内容的更新
     */
    private void updateBeanInfo(TopicItemBean target, TopicItemBean infoBean) {
        //红点
        boolean showUpdateMsg = target.getLatestMsgId() < infoBean.getLatestMsgId();
        if (showUpdateMsg) needUpdateMsgTopics.add(target);
        final String localChange = target.getChange();
        final String serverChange = infoBean.getChange();
        if (Integer.valueOf(localChange) < Integer.valueOf(serverChange)||target.getMembers()==null||target.getMembers().size()<2) {
            needUpdateMemberTopics.add(target);
        }
        target.setShowDot(showUpdateMsg);
        target.setGroup(infoBean.getGroup());
        target.setChange(infoBean.getChange());
        target.setTopicId(infoBean.getTopicId());
        target.setType(infoBean.getType());
        target.setLatestMsgTime(infoBean.getLatestMsgTime());
        target.setLatestMsgId(infoBean.getLatestMsgId());
    }


    /**
     * 本地没有 指定 topic 数据
     * 向服务器请求
     * 如果返回了 topic 数据
     */
    private void requestTopicMemberInfoFromServer(final TopicItemBean bean, final GetTopicItemBeanCallback callback) {
        if (checkShouldUpdateMember(bean, callback)) return;

        com.yanxiu.im.net.TopicGetTopicsRequest_new getTopicsRequest = new com.yanxiu.im.net.TopicGetTopicsRequest_new();
        getTopicsRequest.imToken = Constants.imToken;
        getTopicsRequest.topicIds = String.valueOf(bean.getTopicId());
        mQueueManager.addRequest(getTopicsRequest, com.yanxiu.im.net.TopicGetTopicsResponse_new.class, new IYXHttpCallback<TopicGetTopicsResponse_new>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, com.yanxiu.im.net.TopicGetTopicsResponse_new ret) {
                Log.i(TAG, "request topic server info onSuccess: ");
                /*没有 topic 信息*/
                if (ret.code != 0||ret.data == null || ret.data.topic == null || ret.data.topic.size() == 0) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onGetTopicItemBean(bean);
                        }
                    });
                    return;
                }
                ImTopic_new imTopic = ret.data.topic.get(0);
                //将服务器返回的结果保存到数据库 并生成 topicitembean
                final TopicItemBean dbTopic = DatabaseManager.updateDbTopicWithImTopic(imTopic);
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onGetTopicItemBean(dbTopic);
                    }
                });

            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                callback.onGetTopicItemBean(null);
            }
        });
    }

    private boolean checkShouldUpdateMember(TopicItemBean bean, GetTopicItemBeanCallback callback) {
        //检查是否需要进行 member 更新
//        final TopicItemBean topicByTopicId = TopicInMemoryUtils.findTopicByTopicId(bean.getTopicId(), needUpdateMemberTopics);
//        if (topicByTopicId == null) {
//            //没在更新 member 的列表中 直接返回 bean 不尽兴 网路请求
//            callback.onGetTopicItemBean(bean);
//            return true;
//        }
        return false;
    }

    public void requestLastestMsgPageFromServer(final TopicItemBean itemBean, final GetTopicItemBeanCallback callback) {
        if (checkShouldUpdateMsg(itemBean, callback)) return;


        //更新msg
        GetTopicMsgsRequest_new getTopicMsgsRequest = new GetTopicMsgsRequest_new();
        getTopicMsgsRequest.imToken = Constants.imToken;
        getTopicMsgsRequest.topicId = Long.toString(itemBean.getTopicId());
        //如果是最新加入的topic 没有消息记录 赋予latestmsgid 为long最大值
        //由于是获取最新消息 所以 请求startid 采用Long.MAXVALUE
        getTopicMsgsRequest.startId = String.valueOf(Long.MAX_VALUE);
        getTopicMsgsRequest.order = "desc";
        mQueueManager.addRequest(getTopicMsgsRequest, GetTopicMsgsResponse_new.class, new IYXHttpCallback<GetTopicMsgsResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetTopicMsgsResponse_new ret) {
                if (ret.code != 0 || ret.data == null || ret.data.topicMsg == null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onGetTopicItemBean(itemBean);
                        }
                    });

                    return;
                }
                ArrayList<MsgItemBean> msgPages = new ArrayList<>();
                for (ImMsg_new imMsgNew : ret.data.topicMsg) {
                    //获取 msglist 后  首先  保存数据库
                    final MsgItemBean msgItemBean = DatabaseManager.updateDbMsgWithImMsg(imMsgNew, Constants.imId);
                    msgPages.add(msgItemBean);
                }

                List<MsgItemBean> msgList = itemBean.getMsgList();
                if (msgList == null) {
                    msgList = new ArrayList<>();
                    itemBean.setMsgList(msgList);
                }
                TopicInMemoryUtils.duplicateRemoval(msgPages, itemBean.getMsgList());
                itemBean.getMsgList().addAll(msgPages);
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onGetTopicItemBean(itemBean);
                    }
                });

            }

            @Override
            public void onFail(YXRequestBase request, Error error) {

            }
        });
    }

    private boolean checkShouldUpdateMsg(TopicItemBean itemBean, GetTopicItemBeanCallback callback) {
//        //检查是否需要更新 msg
//        final TopicItemBean topicByTopicId = TopicInMemoryUtils.findTopicByTopicId(itemBean.getTopicId(), needUpdateMsgTopics);
//        if (topicByTopicId == null) {
//            callback.onGetTopicItemBean(itemBean);
//            return true;
//        }
        return false;
    }

    /**
     * 通过 member 获取目标 topicitembean
     */
    public void getPrivateTopicByMemberid(long memberId, long fromTopic, GetPrivateTopicCallback<TopicItemBean> callback) {
        TopicItemBean resultBean = null;
        //内存检查
        final ArrayList<TopicItemBean> topics = SharedSingleton.getInstance().<ArrayList<TopicItemBean>>get(SharedSingleton.KEY_TOPIC_LIST);
        resultBean = TopicInMemoryUtils.findPrivateTopicByMemberId(memberId, topics);
        //数据库检查
        if (resultBean == null) {
            final List<TopicItemBean> dbTopics = DatabaseManager.topicsFromDb();
            resultBean = TopicInMemoryUtils.findPrivateTopicByMemberId(memberId, dbTopics);
        }
        if (resultBean == null) {
            //创建 mocktopic 设计是在发送消息的时候才会进行 mocktopic 的创建 所以这里不能创建
            //没有找到目标私聊  为了显示 ui 获取 member 信息
            final DbMember memberById = DatabaseManager.getMemberById(memberId);
            callback.onNoTargetTopic(memberById.getName());
        } else {
            callback.onFindRealPrivateTopic(resultBean);
        }
    }

    public interface GetPrivateTopicCallback<E> {
        void onFindRealPrivateTopic(E bean);

        void onNoTargetTopic(String memberName);
    }


    /**
     * 创建 mockTopic
     */
    private TopicItemBean createMockTopicForMsg(long memberId, long fromTopic) {
        List<TopicItemBean> topics = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        TopicItemBean mockTopic = DatabaseManager.createMockTopic(memberId, fromTopic);
        if (topics != null) {
            topics.add(mockTopic);
            ImTopicSorter.sortByLatestTime(topics);
        }
        return mockTopic;
    }

    /**
     * 执行创建 topic
     */
    public void createPrivateTopic(long memberId, long fromTopic, GetTopicItemBeanCallback callback) {
        //创建 topic  1、 创建本地 mocktopic 2、网络请求服务器创建 topic 3、服务器创建成功返回 realtopic  后续会发送 mqtt 通知
        TopicItemBean resultBean = null;
        final TopicItemBean mockTopic = createMockTopicForMsg(memberId, fromTopic);
        resultBean = mockTopic;
    }

    /**
     * 执行删除 topic 操作
     */
    public void deleteTopicHistory(final long topicId, final DeleteTopicCallback callback) {
        //删除步骤 1、请求服务器删除 topic成功后  2、 删除数据库 3、同步内存（删除内存中的实例）
        requestDeletedTopic(topicId, new DeleteTopicCallback() {
            @Override
            public void onTopicDeleted(boolean success, String msg) {
                if (success) {
                    //服务器删除成功
                    DatabaseManager.deleteTopicById(topicId);
                    final ArrayList<TopicItemBean> topicItemBeans = SharedSingleton.getInstance().<ArrayList<TopicItemBean>>get(SharedSingleton.KEY_TOPIC_LIST);
                    TopicInMemoryUtils.removeTopicFromListById(topicId, topicItemBeans);
                    callback.onTopicDeleted(true, "移除成功");

                } else {
                    //失败 服务器清空失败
                    final ArrayList<TopicItemBean> topicItemBeans = SharedSingleton.getInstance().<ArrayList<TopicItemBean>>get(SharedSingleton.KEY_TOPIC_LIST);
                    TopicInMemoryUtils.removeTopicFromListById(topicId, topicItemBeans);
                    callback.onTopicDeleted(true, "本地移除成功");
                }

            }
        });

    }

    private void requestDeletedTopic(long topicId, DeleteTopicCallback callback) {
        callback.onTopicDeleted(true, "删除成功");
    }

    /**
     * 请求服务器创建私聊
     */
    private void requestCreateTopic(final TopicItemBean currentTopic, long memberId, long fromTopic, final GetTopicItemBeanCallback callback) {
        TopicCreateTopicRequest_new createTopicRequest = new TopicCreateTopicRequest_new();
        createTopicRequest.imToken = Constants.imToken;
        createTopicRequest.topicType = "1"; // 私聊
        createTopicRequest.imMemberIds = Long.toString(Constants.imId) + "," + Long.toString(memberId);
        createTopicRequest.fromGroupTopicId = fromTopic + "";
        createTopicRequest.startRequest(TopicCreateTopicResponse_new.class, new IYXHttpCallback<TopicCreateTopicResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, final TopicCreateTopicResponse_new ret) {
                ImTopic_new imTopic = null;
                if (ret != null && ret.data != null && ret.data.topic != null && !ret.data.topic.isEmpty()) {
                    // 应该只有一个imTopic
                    imTopic = ret.data.topic.get(0);
                }
                if (imTopic == null) { //视为失败
                    DatabaseManager.topicCreateFailed(currentTopic);
                    return;
                }
                DatabaseManager.migrateMockTopicToRealTopic(currentTopic, imTopic);
                //创建topic 成功 开始发送消息
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onGetTopicItemBean(currentTopic);
                    }
                });


            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                DatabaseManager.topicCreateFailed(currentTopic);
            }
        });
    }


    public interface DeleteTopicCallback {
        @UiThread
        void onTopicDeleted(boolean success, String msg);
    }

    public interface GetTopicItemBeanCallback {
        @UiThread
        void onGetTopicItemBean(TopicItemBean bean);
    }

}
