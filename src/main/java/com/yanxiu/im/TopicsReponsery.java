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
import com.yanxiu.im.business.utils.ImServerDataChecker;
import com.yanxiu.im.business.utils.ProcessUtils;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.db.DbTopic;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.manager.HttpRequestManager;
import com.yanxiu.im.manager.MqttConnectManager;
import com.yanxiu.im.manager.RequestQueueManager;
import com.yanxiu.im.net.GetTopicMsgsRequest_new;
import com.yanxiu.im.net.GetTopicMsgsResponse_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
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
        needUpdateMemberTopics.clear();
        needUpdateMsgTopics.clear();
        mHttpRequestManager.requestUserTopicList(imToken, new HttpRequestManager.GetTopicListCallback<ImTopic_new>() {
            @Override
            public void onGetTopicList(List<ImTopic_new> topicList) {
                //查找新的 和需要更新的
                for (ImTopic_new imTopicNew : topicList) {
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
                    for (ImTopic_new imTopicNew : topicList) {
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
                    //继续请求 msg 信息 之后回调 ui
                    requestLastestMsgPageFromServer(bean, callback);
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
        //红点
        boolean showUpdateMsg = target.getLatestMsgId() < infoBean.getLatestMsgId();
        if (showUpdateMsg) needUpdateMsgTopics.add(target);
        final String localChange = target.getChange();
        final String serverChange = infoBean.getChange();
        if (Integer.valueOf(localChange) < Integer.valueOf(serverChange) || target.getMembers() == null || target.getMembers().size() < 2) {
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

        mHttpRequestManager.requestTopicMemberList(Constants.imToken, Long.toString(bean.getTopicId()), new HttpRequestManager.GetTopicMemberListCallback<ImTopic_new>() {
            @Override
            public void onGetTopicMembers(ImTopic_new topicWithMembers) {
                final TopicItemBean dbTopic = DatabaseManager.updateDbTopicWithImTopic(topicWithMembers);
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onGetTopicItemBean(dbTopic);
                    }
                });

            }

            @Override
            public void onGetFailure() {
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
        return false;
    }

    public void requestLastestMsgPageFromServer(final TopicItemBean itemBean, final GetTopicItemBeanCallback callback) {
        if (checkShouldUpdateMsg(itemBean, callback)) return;

        mHttpRequestManager.requestTopicMsgList(Constants.imToken, itemBean.getTopicId(), new HttpRequestManager.GetTopicMsgListCallback<ImMsg_new>() {
            @Override
            public void onGetTopicMsgList(List<ImMsg_new> msgList) {
                ArrayList<MsgItemBean> msgPages = new ArrayList<>();
                for (ImMsg_new imMsgNew : msgList) {
                    //获取 msglist 后  首先  保存数据库
                    final MsgItemBean msgItemBean = DatabaseManager.updateDbMsgWithImMsg(imMsgNew, Constants.imId);
                    msgPages.add(msgItemBean);
                }

                List<MsgItemBean> msgBean = itemBean.getMsgList();
                if (msgBean == null) {
                    msgBean = new ArrayList<>();
                    itemBean.setMsgList(msgBean);
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
            public void onGetFailure() {
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
        return false;
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
     * 执行删除 topic 操作
     */
    public void deleteTopicHistory(TopicItemBean topicItemBean, final DeleteTopicCallback callback) {
        //删除步骤 1、请求服务器删除 topic成功后  2、 删除数据库 3、同步内存（删除内存中的实例）
        //服务器删除成功
        DatabaseManager.deleteLocalMsgByTopicId(topicItemBean);
        callback.onTopicDeleted();
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
     */
    public void loadPageMsg(TopicItemBean targetTopic, GetMsgPageCallback callback) {
        //首先有网络获取
        requestMsgListFromServer(targetTopic, callback);
    }


    private void requestMsgListFromServer(final TopicItemBean targetTopic, final GetMsgPageCallback callback) {
        GetTopicMsgsRequest_new getMsgsRequest = new GetTopicMsgsRequest_new();
        getMsgsRequest.imToken = Constants.imToken;
        final long startId = TopicInMemoryUtils.getMinMsgBeanRealIdInList(targetTopic.getMsgList());
        getMsgsRequest.startId = String.valueOf(startId);
        getMsgsRequest.topicId = targetTopic.getTopicId() + "";

        getMsgsRequest.startRequest(GetTopicMsgsResponse_new.class, new IYXHttpCallback<GetTopicMsgsResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, GetTopicMsgsResponse_new ret) {
                //请求异常
                if (ret == null || ret.code != 0 || ret.data == null || ret.data.topicMsg == null) {
                    final ArrayList<MsgItemBean> topicMsgs = DatabaseManager.getTopicMsgs(targetTopic.getTopicId(), startId, DatabaseManager.pagesize);
                    TopicInMemoryUtils.duplicateRemoval(topicMsgs, targetTopic.getMsgList());
                    targetTopic.getMsgList().addAll(topicMsgs);
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onGetPage(topicMsgs);
                        }
                    });
                    return;
                }
                //保存入数据库
                for (ImMsg_new msgNew : ret.data.topicMsg) {
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
            public void onFail(YXRequestBase request, Error error) {
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
        });
    }

    public interface GetMsgPageCallback {
        void onGetPage(ArrayList<MsgItemBean> msgs);
    }

    /**
     * 创建一个 mocktopic
     */
    public TopicItemBean createMockTopic(long fromId, long memberId) {
        final TopicItemBean mockTopic = DatabaseManager.createMockTopic(memberId, fromId);
        addToMemory(mockTopic);
        ImTopicSorter.sortByLatestTime(topicInMemory);
        return mockTopic;
    }

    public interface CreateTopicCallback {
        void onTopicCreatedSuccess(TopicItemBean topicItemBean);

        void onTopicCreateFailed();
    }


    /**
     * 向服务器请求创建一个新的聊天 topic
     */
    public void createNewTopic(final TopicItemBean mockTopic, String fromTopicId, final CreateTopicCallback callback) {
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
        mHttpRequestManager.requestCreateNewPrivateTopic(Constants.imToken, fromTopicId, memberId, Constants.imId, new HttpRequestManager.CreatePrivateTopicCallback<ImTopic_new>() {
            @Override
            public void onCreated(ImTopic_new topic) {
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


}
