//package com.yanxiu.im.activity;
//
//import android.content.ComponentName;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.IBinder;
//import android.text.TextUtils;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//
//import com.test.yanxiu.common_base.ui.FaceShowBaseFragment;
//import com.test.yanxiu.common_base.utils.SrtLogger;
//import com.yanxiu.im.Constants;
//import com.yanxiu.im.R;
//import com.yanxiu.im.db.DbMember;
//import com.yanxiu.im.event.MigrateMockTopicEvent;
//import com.yanxiu.im.manager.RequestQueueManager;
//import com.yanxiu.im.bean.MsgItemBean;
//import com.yanxiu.im.bean.TopicItemBean;
//import com.yanxiu.im.bean.net_bean.ImMsg_new;
//import com.yanxiu.im.bean.net_bean.ImTopic_new;
//import com.yanxiu.im.db.DbTopic;
//import com.yanxiu.im.db.ImSpManager;
//import com.yanxiu.im.event.NewMsgEvent;
//import com.yanxiu.im.event.TopicChangEvent;
//import com.yanxiu.im.manager.DatabaseManager;
//import com.yanxiu.im.manager.MqttProtobufManager;
//import com.yanxiu.im.service.MqttService;
//import com.yanxiu.im.net.GetTopicMsgsRequest_new;
//import com.yanxiu.im.net.GetTopicMsgsResponse_new;
//import com.yanxiu.im.net.PolicyConfigRequest_new;
//import com.yanxiu.im.net.PolicyConfigResponse_new;
//import com.yanxiu.im.net.TopicGetMemberTopicsRequest_new;
//import com.yanxiu.im.net.TopicGetMemberTopicsResponse_new;
//import com.yanxiu.im.net.TopicGetTopicsRequest_new;
//import com.yanxiu.im.net.TopicGetTopicsResponse_new;
//import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
//import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;
//
//import org.greenrobot.eventbus.EventBus;
//import org.greenrobot.eventbus.Subscribe;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Timer;
//import java.util.TimerTask;
//
//import okhttp3.Request;
//
//import static android.content.Context.BIND_AUTO_CREATE;
//
///**
// * topic列表页
// * Created by 戴延枫 on 2018/5/8.
// */
//
//public class ImTopicListFragment_new extends FaceShowBaseFragment {
//
//    public static final int ACTIVITY_RESULT_REMOVED_USER = 0X33;
//    /**
//     * topic数据
//     */
//    private ArrayList<TopicItemBean> topicList = new ArrayList<>();
//
//    private List<TopicItemBean> maybeNeedUpdateMsgTopicList;//可能需要更新msg的topic
//
//    public ArrayList<TopicItemBean> getTopicList() {
//        return topicList;
//    }
//
//    Handler mHandler = new Handler();
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View v = inflater.inflate(R.layout.im_fragment_topic_list, container, false);
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                startMqttService();
////                setupData();
//                getTopicsFromDb();
//            }
//        }, 3000);//此处延时只是为了断点查看信息，完全可以无视
//        return v;
//    }
//
//    @Override
//    public void onDestroyView() {
//        stopMqttService();
//        super.onDestroyView();
//    }
//
//
//    //MQTT start
//    private Timer reconnectTimer = new Timer();
//    private MqttService.MqttBinder binder = null;
//    private DbTopic curTopic = null;                                    // 当前界面上开启msgs的topic，curTopic为空说明是新建私聊
//    private ArrayList<DbTopic> msgShownTopics = new ArrayList<>();      // 因为需要可以从群聊点击头像进入私聊，多级msgs界面
//
//    /**
//     * 开启mqtt的service
//     */
//    private void startMqttService() {
//        EventBus.getDefault().register(this);
//        getImHostRequest(new GetImHostCallBack() {
//            @Override
//            public void onSuccess(String host) {
//                Intent intent = new Intent(getActivity(), MqttService.class);
//                intent.putExtra("host", host);
//                getActivity().bindService(intent, mqttServiceConnection, BIND_AUTO_CREATE);
//            }
//        });
//    }
//
//    /**
//     * qmtt的服务
//     */
//    public ServiceConnection mqttServiceConnection = new ServiceConnection() {
//
//
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
//            SrtLogger.log("im mqtt", "service connectted");
//            binder = (MqttService.MqttBinder) iBinder;
//
//            binder.getService().setmMqttServiceCallback(new MqttService.MqttServiceCallback() {
//                @Override
//                public void onDisconnect() {
//                    SrtLogger.log("frc", "service onDisconnect");
//                    // 每30秒重试一次
//                    if (reconnectTimer != null) {
//                        reconnectTimer.cancel();
//                        reconnectTimer.purge();
//                        reconnectTimer = null;
//                    }
//
//                    reconnectTimer = new Timer();
//                    reconnectTimer.schedule(new TimerTask() {
//                        @Override
//                        public void run() {
//                            // 重连必须重新给一个clientId，否则直接失败
//                            binder.init();
//                            binder.connect();
//                        }
//                    }, 30 * 1000);
//                }
//
//                @Override
//                public void onConnect() {
//                    SrtLogger.log("frc", "service onConnect");
//                    if (reconnectTimer != null) {
//                        reconnectTimer.cancel();
//                        reconnectTimer.purge();
//                        reconnectTimer = null;
//                    }
//
//                    // 为统一处理，移到此处
//                    if (mTask != null && !mTask.isCancelled()) {
//                        mTask.cancel(true);
//                    }
//                    mTask = new MyTask();
//                    mTask.execute();
////                    updateTopicsFromHttp();
//
//                    binder.subscribeMember(Constants.imId);
//
//                    for (TopicItemBean dbTopic : topicList) {
//                        binder.subscribeTopic(Long.toString(dbTopic.getTopicId()));
//                    }
//                }
//            });
//
//            binder.init();
//            binder.connect();
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            SrtLogger.log("im mqtt", "service disconnectted");
//
//            if (reconnectTimer != null) {
//                reconnectTimer.cancel();
//                reconnectTimer.purge();
//                reconnectTimer = null;
//            }
//        }
//    };
//
//    private void stopMqttService() {
//        // 已经在MqttService的unbind中处理
//    }
//
//    private interface GetImHostCallBack {
//        void onSuccess(String host);
//
//    }
//
//    //MQTT end
//
//
//    /**
//     * 这是个异步过程可能会存在的问题
//     * 在何处进行网络请求-->目前可以放在登录完毕后或者进入主页中 但是考虑到module的完整性并且此处做了mqtt连接失败会重连的策略  其实可以放在此处获取host的
//     * 但是如果一些极端现象：现在server从host1切花到host2  但是host1仍然可用，这种情况下用户第一次进来还是连的host1  只有退出后再进入才会连host2  考虑到目前切换的频率极低所以就先这样写
//     * 数据存储于Sp中：本来sp应用用统一的管理，但是这是个独立的module并且公用的spManager在App中没有放到CommonBase中,而且此处存储的host只在此处获取，就不去动app module中的SPManager了
//     */
//    private void getImHostRequest(final GetImHostCallBack iGetImHostCallBack) {
//        PolicyConfigRequest_new policyConfigRequest = new PolicyConfigRequest_new();
//        policyConfigRequest.startRequest(PolicyConfigResponse_new.class, new IYXHttpCallback<PolicyConfigResponse_new>() {
//            /**
//             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
//             *
//             * @param request OkHttp Request
//             */
//            @Override
//            public void onRequestCreated(Request request) {
//
//            }
//
//            @Override
//            public void onSuccess(YXRequestBase request, PolicyConfigResponse_new ret) {
//                if (ret.code == 0 && ret.data != null) {
////                    saveHost2Sp(ret.data.getMqttServer());
//                    ImSpManager.getInstance().setImHost(ret.data.getMqttServer());
//                    if (iGetImHostCallBack != null) {
//                        iGetImHostCallBack.onSuccess(ret.data.getMqttServer());
//                    }
//                } else {
////                    String oldHost = getHostBySp();
//                    String oldHost = ImSpManager.getInstance().getImHost();
//                    if (TextUtils.isEmpty(oldHost)) {
//                        getImHostRequest(iGetImHostCallBack);
//                    } else {
//                        if (iGetImHostCallBack != null) {
//                            iGetImHostCallBack.onSuccess(oldHost);
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onFail(YXRequestBase request, Error error) {
////                String oldHost = getHostBySp();
//                String oldHost = ImSpManager.getInstance().getImHost();
//                if (TextUtils.isEmpty(oldHost)) {
//                    getImHostRequest(iGetImHostCallBack);
//                } else {
//                    if (iGetImHostCallBack != null) {
//                        iGetImHostCallBack.onSuccess(oldHost);
//                    }
//                }
//            }
//        });
//    }
//
//    //拉取数据 start
//
//    /**
//     * 拉取数据步骤：
//     * 1.从db获取本地数据：updateTopicsFromDb()
//     * 2.http拉取：
//     * 2.1 updateTopicsFromDb()
//     * 2.2 updateTopicsFromHttpWithoutMembers()
//     */
//
//
//    /**
//     * 1.
//     * 从db获取本地数据
//     */
//    private void getTopicsFromDb() {
//        //初始化db
//        DatabaseManager.useDbForUser(Long.toString(Constants.imId) + "_db");//todo:应该放在config里面去
//        List<TopicItemBean> list = DatabaseManager.topicsFromDb();
//        if (list != null && !list.isEmpty())
//            topicList.addAll(list);
//    }
//
//
//    private MyTask mTask;
//
//    private class MyTask extends AsyncTask<Object, Integer, Object> {
//        //onPreExecute方法用于在执行后台任务前做一些UI操作
//        @Override
//        protected void onPreExecute() {
//        }
//
//        //doInBackground方法内部执行后台任务,不可在此方法内修改UI
//        @Override
//        protected String doInBackground(Object... params) {
//            updateTopicsFromHttp();
//            return null;
//        }
//
//        //onProgressUpdate方法用于更新进度信息
//        @Override
//        protected void onProgressUpdate(Integer... progresses) {
//        }
//
//        //onPostExecute方法用于在执行完后台任务后更新UI,显示结果
//        @Override
//        protected void onPostExecute(Object result) {
//        }
//
//        //onCancelled方法用于在取消执行中的任务时更改UI
//        @Override
//        protected void onCancelled() {
//        }
//    }
//
//
//    /**
//     * 2.
//     * 从Http获取用户的topic列表，不包含members，完成后继续从Http获取需要更新的topic的信息
//     */
//    private void updateTopicsFromHttp() {
//        TopicGetMemberTopicsRequest_new getMemberTopicsRequest = new TopicGetMemberTopicsRequest_new();
//        getMemberTopicsRequest.imToken = Constants.imToken;
//        getMemberTopicsRequest.startRequest(TopicGetMemberTopicsResponse_new.class, new IYXHttpCallback<TopicGetMemberTopicsResponse_new>() {
//            /**
//             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
//             *
//             * @param request OkHttp Request
//             */
//            @Override
//            public void onRequestCreated(Request request) {
//
//            }
//
//            @Override
//            public void onSuccess(YXRequestBase request, TopicGetMemberTopicsResponse_new ret) {
//                // 3
//                //获取用户服务器上所有的topic
//                for (ImTopic_new imTopic : ret.data.topic) {
//                    binder.subscribeTopic(Long.toString(imTopic.topicId));
//                }
//                //检查用户是否在离线的时候被topic 删除 对象分别为数据库topics 与 ret.data.topic
////                checkBeenRemoveFromAnyTopic(ret);
//                //
//                //检查是否需要更新member
//                checkTopicNeedUpdateMembers(ret);
//                //除了新topic和有topicchagne的topic，也需要请求msg
//                if (maybeNeedUpdateMsgTopicList != null && !maybeNeedUpdateMsgTopicList.isEmpty())
//                    updateEachTopicMsgs(maybeNeedUpdateMsgTopicList);
//
//            }
//
//            @Override
//            public void onFail(YXRequestBase request, Error error) {
//
//            }
//        });
//    }
//
//    /**
//     * 3.1
//     * 判断topic是否需要更新member
//     * 从Http获取需要更新的topic的信息，完成后写入DB，更新UI
//     * 需要更新的两种情况：
//     * 1.新的topic
//     * 2.topicChange有变化的topic
//     */
//    private void checkTopicNeedUpdateMembers(TopicGetMemberTopicsResponse_new ret) {
//
//        List<String> idTopicsNeedUpdateMember = new ArrayList<>(); // 因为可能有新的，所以只能用topicId
//        List<TopicItemBean> topicsNotNeedUpdateMember = new ArrayList<>();//不需要更新的列表
//
//        //用http返回的topic去遍历本地的topic，所有不在DB中的，以及所有在DB中但change不等于topicChange的topics，都需要更新
//        for (ImTopic_new imTopic : ret.data.topic) {
//            boolean needUpdateMembers = true;
//            for (TopicItemBean dbTopic : topicList) {
//                if (dbTopic.getTopicId() == imTopic.topicId) { //已经存在的topic
//                    dbTopic.setLatestMsgId(imTopic.latestMsgId);
//                    dbTopic.setLatestMsgTime(imTopic.latestMsgTime);
//                    if (dbTopic.getChange().equals(imTopic.topicChange)) { //已经存在的topic，且topicchange无变化的，不需更新
//                        needUpdateMembers = false;
//                        topicsNotNeedUpdateMember.add(dbTopic);
//                    }
//                    break;
//                }
//            }
//            if (needUpdateMembers) { //需要更新的topic
//                idTopicsNeedUpdateMember.add(Long.toString(imTopic.topicId));
//            }
//        }
//        //更新memeber 信息
//        if (idTopicsNeedUpdateMember.size() > 0) {
//            for (String topicId : idTopicsNeedUpdateMember) {
//                // 由于server限制，改成一个个取
//                updateTopicsWithMembers(topicId);
//            }
//        }
//        //排除了新topic和有topicChange的topic，需要判断是否需要请求msg
//        maybeNeedUpdateMsgTopicList = topicsNotNeedUpdateMember;
//    }
//
//    /**
//     * 3.2
//     * 更新members
//     * http, mqtt 公用
//     */
//    private void updateTopicsWithMembers(String topicIds) {
//        TopicGetTopicsRequest_new getTopicsRequest = new TopicGetTopicsRequest_new();
//        getTopicsRequest.imToken = Constants.imToken;
//        getTopicsRequest.topicIds = topicIds;
//        rqManager.addRequest(getTopicsRequest, TopicGetTopicsResponse_new.class, new IYXHttpCallback<TopicGetTopicsResponse_new>() {
//            /**
//             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
//             *
//             * @param request OkHttp Request
//             */
//            @Override
//            public void onRequestCreated(Request request) {
//
//            }
//
//            @Override
//            public void onSuccess(YXRequestBase request, TopicGetTopicsResponse_new ret) {
//                // 更新数据库
//                List<TopicItemBean> topicsNeedUpdateMember = new ArrayList<>();
//
//                for (ImTopic_new imTopic : ret.data.topic) {
//                    TopicItemBean tempTopic = null;//需要更新的topic，为保证对象一致，该topic必须是topicList里的对象。
//                    TopicItemBean dbTopic = DatabaseManager.updateDbTopicWithImTopic(imTopic);//更新数据库
//                    boolean hasThisTopic = false;
//                    // 更新UI使用的topicList
//                    for (TopicItemBean uiTopic : topicList) {
//                        if (uiTopic.getTopicId() == imTopic.topicId) {
//                            hasThisTopic = true;
//                            uiTopic.setTopicId(imTopic.topicId);
//                            uiTopic.setName(imTopic.topicName);
//                            uiTopic.setType(imTopic.topicType);
//                            uiTopic.setChange(imTopic.topicChange);
//                            uiTopic.setGroup(imTopic.topicGroup);
//                            uiTopic.setMembers(dbTopic.getMembers());//TODO：member对象变了，应该没有影响
//                            tempTopic = uiTopic;
//                            break;
//                        }
//                    }
//                    if (!hasThisTopic) {
//                        //新topic 进入UI 记录本地操作时间
//                        topicList.add(dbTopic);
//                        dbTopic.setLatestMsgTime(imTopic.latestMsgTime);
//                        //关于 topic.latestMsgId=0  在请求时进行判断 如果为0 赋予 Long的最大值
//                        dbTopic.setLatestMsgId(imTopic.latestMsgId);
//                        tempTopic = dbTopic;
//                    }
//                    topicsNeedUpdateMember.add(tempTopic);
//                }
//                // 4，对于需要更新members的topic，等待更新完members，再去取msgs
//                updateEachTopicMsgs(topicsNeedUpdateMember);
//            }
//
//            @Override
//            public void onFail(YXRequestBase request, Error error) {
//
//            }
//        });
//    }
//
//    //http拉取 end
//
//
//    // 4，依次更新topic的最新一页数据，并更新数据库，然后更新UI
//    private int totalRetryTimes;
//    private RequestQueueManager rqManager = new RequestQueueManager();
//
//
//    /**
//     * 跟新topic的最新一页msg
//     *
//     * @param topics
//     */
//    private void updateEachTopicMsgs(List<TopicItemBean> topics) {
//        totalRetryTimes = 10;
//        //由于 topics 按时间顺序 逆序排列  如果需要置顶 需要逆序判断 需要将时间较远的先置顶 时间近的后置顶
//        for (int i = topics.size() - 1; i >= 0; i--) {
//            TopicItemBean dbTopic = topics.get(i);
//            doGetTopicMsgsRequest(dbTopic);
//        }
//        DatabaseManager.checkAndMigrateMockTopic();
//    }
//
//    /**
//     * 获取最新一页的msglist
//     */
//    private void doGetTopicMsgsRequest(final TopicItemBean topicItemBean) {
//        //判断是否需要更新msg
//        if ((topicItemBean.getMsgList() != null) && (topicItemBean.getMsgList().size() > 0)) {
//            MsgItemBean dbMsg = topicItemBean.getMsgList().get(0);
//            if (dbMsg.getMsgId() >= topicItemBean.getLatestMsgId()) {
//                // 数据库中已有最新的msg，不用更新
//                topicItemBean.setLatestMsgId(dbMsg.getMsgId());
//                return;
//            }
//        }
//        //更新msg
//        GetTopicMsgsRequest_new getTopicMsgsRequest = new GetTopicMsgsRequest_new();
//        getTopicMsgsRequest.imToken = Constants.imToken;
//        getTopicMsgsRequest.topicId = Long.toString(topicItemBean.getTopicId());
//        //如果是可空的topic 没有消息记录 赋予latestmsgid 为long最大值
//        if (topicItemBean.getLatestMsgId() == 0) {
//            getTopicMsgsRequest.startId = String.valueOf(Long.MAX_VALUE);
//        } else {
//            getTopicMsgsRequest.startId = Long.toString(topicItemBean.getLatestMsgId());
//        }
////        getTopicMsgsRequest.order = "desc";
//
//        rqManager.addRequest(getTopicMsgsRequest, GetTopicMsgsResponse_new.class, new IYXHttpCallback<GetTopicMsgsResponse_new>() {
//            /**
//             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
//             *
//             * @param request OkHttp Request
//             */
//            @Override
//            public void onRequestCreated(Request request) {
//
//            }
//
//            @Override
//            public void onSuccess(YXRequestBase request, GetTopicMsgsResponse_new ret) {
//                // 新建topic成功后topicMsg.size为0
//                if (ret.data.topicMsg == null || ret.data.topicMsg.size() == 0) {
//                    //判断获取的消息数量是否为0 或空  此时 不显示红点
//                    topicItemBean.setShowDot(false);
//                    DatabaseManager.updateTopicWithTopicItemBean(topicItemBean);
//                    return;
//                }
//
//                // 有新消息，UI上应该显示红点
//                topicItemBean.setShowDot(true);
//                DatabaseManager.updateTopicWithTopicItemBean(topicItemBean);
//
//                // 处理新消息
//                mergeMsgHttpAndLocal(topicItemBean, ret.data.topicMsg);
//                //通知imMsgListActivity刷新列表消息
////                MqttProtobufManager.onTopicUpdate(topicItemBean.getTopicId());
//                topicUpdate(topicItemBean.getTopicId());
//
//            }
//
//            @Override
//            public void onFail(YXRequestBase request, Error error) {
//                // 重试
//                if (totalRetryTimes-- <= 0) {
//                    return;
//                }
//                doGetTopicMsgsRequest(topicItemBean);
//            }
//        });
//    }
//
//    /**
//     * 处理http来的信息
//     * 1.去重
//     * 2.创建并更新msg数据库
//     * 3.更新ui数据
//     *
//     * @param topicBean  当前获取新消息列表的topic
//     * @param newMsgList 服务器返回的最新消息列表
//     */
//    private void mergeMsgHttpAndLocal(TopicItemBean topicBean, List<ImMsg_new> newMsgList) {
//        //TODO:该算法会导致anr，原因是547行循环逻辑有误，后期可以再修改一下。 start
////        //倒序
////        for (int i = newMsgList.size() - 1; i >= 0; i--) {
////            ImMsg_new imMsg = newMsgList.get(i);
////            List<MsgItemBean> uiMsgList = topicBean.getMsgList();
////            if (uiMsgList == null || uiMsgList.isEmpty()) { //本地消息数量为0，新来的msg都加入进去
////                MsgItemBean newMsg = DatabaseManager.updateDbMsgWithImMsg(imMsg, Constants.imId);
////                topicBean.getMsgList().add(0, newMsg);
////                if (newMsg.getMsgId() > topicBean.getLatestMsgId()) {
////                    topicBean.setLatestMsgId(newMsg.getMsgId());
////                    topicBean.setLatestMsgTime(newMsg.getSendTime());
////                }
////            } else { //本地有消息，已存在的消息不加入数据库，但是需要把mymsg的状态更改为成功，防止mymsg状态漏操作
////                for (int j = 0; j < uiMsgList.size(); j++) {
////                    if (imMsg.reqId.equals(uiMsgList.get(j).getReqId())) { //已经存在的消息
////                        DatabaseManager.updateMyMsgDBToSuccess(imMsg);//如果是mymsg，应该把状态改为成功，防止本地漏操作。
////                    } else { //新消息
////                        MsgItemBean newMsg = DatabaseManager.updateDbMsgWithImMsg(imMsg, Constants.imId);
////                        topicBean.getMsgList().add(0, newMsg);
////                        if (newMsg.getMsgId() > topicBean.getLatestMsgId()) {
////                            topicBean.setLatestMsgId(newMsg.getMsgId());
////                            topicBean.setLatestMsgTime(newMsg.getSendTime());
////                        }
////                    }
////                }
////            }
////        }
//        //TODO:该算法会导致anr，原因是547行循环逻辑有误，后期可以再修改一下。 end
//
//        //本地已有的，移除
//        Iterator<ImMsg_new> imMsgIterator = newMsgList.iterator();
//        while (imMsgIterator.hasNext()) {
//            ImMsg_new imMsg = imMsgIterator.next();
//            //1、判断 是否本地已有数据
//            for (MsgItemBean dbMsg : topicBean.getMsgList()) {
//                if (dbMsg.getReqId().equals(imMsg.reqId)) {
//                    //去重
//                    imMsgIterator.remove();
//                    break;
//                }
//            }
//        }
//        //2、进行新数据的插入 上面已经完成去重工作  这个应该根据msgId进行排序
//        for (int i = newMsgList.size() - 1; i >= 0; i--) {
//            MsgItemBean newMsg = DatabaseManager.updateDbMsgWithImMsg(newMsgList.get(i), Constants.imId);
//            topicBean.getMsgList().add(0, newMsg);
//            if (newMsg.getMsgId() > topicBean.getLatestMsgId()) {
//                topicBean.setLatestMsgId(newMsg.getMsgId());
//                topicBean.setLatestMsgTime(newMsg.getSendTime());
//            }
//        }
//    }
//
//    //eventbus start
//
//    /**
//     * http拉取topic后，有mockTopic同realTopic合并完成
//     *
//     * @param event
//     */
//    @Subscribe
//    private void migrateMockTopicToRealTopic(MigrateMockTopicEvent event) {
//        topicUpdate(event.topicId);
//    }
//
//    /**
//     * topic有更新
//     * 每次http更新完topic的msg后，回调
//     * 该方法里主要是两个逻辑：
//     * 1.更新本页面topic
//     * 2.发送eventbus通知ImTopicListFragment_new页面
//     */
//
//    private void topicUpdate(long topicId) {
//        //1.更新本页面该topic逻辑
//        //TODO:朱晓龙
//
//        //2.发送enentbus，通知聊天页面更新
//        MqttProtobufManager.onTopicUpdate(topicId);
//    }
//
//    /**
//     * topic被删除
//     */
//    private void onTopicDelete(long topicId) {
//        TopicItemBean topicItemBean = findTopicWithTopicId(topicId);
//        if (topicItemBean != null) { //如果为空，说明在内存中没有找到对应的topic，那么，就无需再执行后续操作
//            //清空内存里的成员
//            topicItemBean.getMembers().clear();
//            // 在数据库中删除
//            DatabaseManager.deleteTopicById(topicId);
//            //TODO:xiaolong
//
//        }
//    }
//
//    /**
//     * topic中，有非自己的member被删除
//     */
//    private void onOtherMemberDelete(long topicId) {
//
//    }
//
//    /**
//     * 收到mqtt的新消息
//     *
//     * @param event
//     */
//    @Subscribe
//    public void onMqttNewMsg(NewMsgEvent event) {
//        MsgItemBean newMsg = event.msg;
//        //TODO:xiaolong
//    }
//
//    /**
//     * 收到mqtt的topic有change
//     *
//     * @param event
//     */
//    @Subscribe
//    public void onMqttTopicChange(TopicChangEvent event) {
//        if (event.type == MqttProtobufManager.TopicChange.AddTo) { //有新topic或者topic添加新成员
//            binder.subscribeTopic(Long.toString(event.topicId));
//            updateTopicsWithMembers(Long.toString(event.topicId));//新topic流程
//        } else if (event.type == MqttProtobufManager.TopicChange.RemoveFrom) { //topic删除某个成员
//            //检查当前用户是否 被某个 topic 除名
//            checkUserRemove(event);
//        }
//
//    }
//    //eventbus end
//
//    /**
//     * 检测用户是否被移除
//     * 通过请求该topic的members，来判断当前用户是否被删除。
//     * 1.如果删除，则相当于topic删除。
//     * 2.如果是别的用户被删除，在topic里的memberlist移除。
//     */
//    private void checkUserRemove(TopicChangEvent event) {
//        TopicGetTopicsRequest_new getTopicsRequest = new TopicGetTopicsRequest_new();
//        getTopicsRequest.imToken = Constants.imToken;
//        getTopicsRequest.topicIds = event.topicId + "";
//        rqManager.addRequest(getTopicsRequest, TopicGetTopicsResponse_new.class, new IYXHttpCallback<TopicGetTopicsResponse_new>() {
//            /**
//             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
//             *
//             * @param request OkHttp Request
//             */
//            @Override
//            public void onRequestCreated(Request request) {
//
//            }
//
//            @Override
//            public void onSuccess(YXRequestBase request, TopicGetTopicsResponse_new ret) {
//                // 更新数据库
//                List<TopicItemBean> topicsNeedUpdateMember = new ArrayList<>();
//                ImTopic_new imTopic = null;
//                List<ImTopic_new.Member> imMemberList = null;
//                if (ret.data.topic == null || ret.data.topic.isEmpty()) {
//                    //TODO:疑问：如果topic为空，是否意味着该topic已被删除？暂时不处理
//                    return;
//                }
//                if (imTopic.members == null || imTopic.members.isEmpty()) {
//                    //member为空，视为topic被删除
//                    onTopicDelete(imTopic.topicId);
//                    return;
//                }
//                imTopic = ret.data.topic.get(0);
//                imMemberList = imTopic.members;
//                ArrayList<Long> hasDeletedMemberList = null;//被删除的memberId的集合
//                TopicItemBean topicItemBean = findTopicWithTopicId(imTopic.topicId);//找到对应的topic
//
//                if (topicItemBean != null) { //如果为空，说明在内存中没有找到对应的topic，那么，就无需再执行后续操作
//                    List<DbMember> dbMemberList = topicItemBean.getMembers();
//
//                    if (dbMemberList != null && !dbMemberList.isEmpty()) {
//                        //查找被删除的member start
//                        for (int j = 0; j < dbMemberList.size(); j++) {
//                            DbMember dbMember = dbMemberList.get(j);
//                            boolean imMemberHasDelete = true;
//                            for (int i = 0; i < imMemberList.size(); i++) { //遍历新的member
//                                ImTopic_new.Member imMember = imMemberList.get(i);
//                                if (dbMember.getImId() == imMember.memberId) { //依然存在，没被删除
//                                    imMemberHasDelete = false;
//                                    break;
//                                }
//                            }
//                            if (imMemberHasDelete) {
//                                if (dbMember.getImId() == Constants.imId) { //是当前用户被删除了，相当于topic被删除了
//                                    onTopicDelete(imTopic.topicId);
//                                    return;
//                                } else {
//                                    hasDeletedMemberList.add(dbMember.getImId());
//                                }
//                            }
//
//                        }
//
//                        //在内存中，移除掉被删除的member start
//                        if (hasDeletedMemberList != null && !hasDeletedMemberList.isEmpty()) {
//                            boolean hasDelete = false;
//                            for (int i = 0; i < hasDeletedMemberList.size(); i++) {
//                                long hasDeletedMemberId = hasDeletedMemberList.get(i);//已经被删除的memberid
//                                Iterator<DbMember> iterator = dbMemberList.iterator();
//                                while (iterator.hasNext()) {
//                                    DbMember dbMember = iterator.next();
//                                    if (dbMember.getImId() == hasDeletedMemberId) {
//                                        //移除
//                                        iterator.remove();
//                                        hasDelete = true;
//                                    }
//                                }
//                            }
//                            if (hasDelete)
//                                onOtherMemberDelete(imTopic.topicId);
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onFail(YXRequestBase request, Error error) {
//
//            }
//        });
//    }
//
//    /**
//     * 通过topicId查找内存里的topic
//     *
//     * @param topicId
//     * @return
//     */
//    private TopicItemBean findTopicWithTopicId(long topicId) {
//        TopicItemBean topicItemBean = null;
//        for (int i = 0; i < topicList.size(); i++) {
//            if (topicId == topicList.get(i).getTopicId()) { //找到对应的topic
//                topicItemBean = topicList.get(i);
//                break;
//            }
//        }
//        return topicItemBean;
//    }
//}
