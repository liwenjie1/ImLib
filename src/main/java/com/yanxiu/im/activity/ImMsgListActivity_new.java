//package com.yanxiu.im.activity;
//
//import android.os.Bundle;
//
//import com.test.yanxiu.common_base.ui.ImBaseActivity;
//import com.yanxiu.im.bean.TopicItemBean;
//import com.yanxiu.im.event.TopicUpdateEvent;
//
//import org.greenrobot.eventbus.EventBus;
//import org.greenrobot.eventbus.Subscribe;
//
//public class ImMsgListActivity_new extends ImBaseActivity {
//
//    private TopicItemBean mTopic;
//    private long memberId;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EventBus.getDefault().register(this);
//    }
//
//    /**
//     * topic内容有变化
//     *
//     * @param event
//     */
//    @Subscribe
//    public void onTopicUpdate(TopicUpdateEvent event) {
//
//        //TODO:xilong 判断是否是同一个topic
////        if (event.topicId != topic.getTopicId()) {
////            return;
////        }
//    }
//
////    private void doSend(final String msg, final String reqId) {
////        final String msgReqId = (reqId == null ? UUID.randomUUID().toString() : reqId);
////        if (true) { // 没有topic
////
////            // 1.创建mock topic
////            mTopic = DatabaseManager.createMockTopic(1, 1);
////
////            //创建msg
////            MsgItemBean myMsg = new MsgItemBean(MsgItemBean.MSG_TYPE_MYSELF);
////            myMsg.setState(DbMyMsg_new.State.Sending.ordinal());
////            myMsg.setReqId(msgReqId);
////            myMsg.setMsgId(getLatestMsgId());
////            myMsg.setTopicId(mTopic.getTopicId());
////            myMsg.setSenderId(Constants.imId);
////            myMsg.setSendTime(new Date().getTime());
////            myMsg.setContentType(10);
////            myMsg.setMsg(msg);
////
////
////            //2.创建或者更新mymsg数据库--saveDB
////            boolean UpdateSuccess = DatabaseManager.createOrUpdateMyMsg(myMsg);
////            if (UpdateSuccess) {
////                //更新UI数据
////                mTopic.getMsgList().add(0, myMsg);
////            }
////
////            //3.创建realTopic
//////            createRealTopic(new IYXHttpCallback<TopicCreateTopicResponse_new>() {
//////                /**
//////                 * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
//////                 *
//////                 * @param request OkHttp Request
//////                 */
//////                @Override
//////                public void onRequestCreated(Request request) {
//////
//////                }
//////
//////                @Override
//////                public void onSuccess(YXRequestBase request, TopicCreateTopicResponse_new ret) {
//////                    ImTopic_new imTopic = null;
//////                    if (ret != null && ret.data != null && ret.data.topic != null && !ret.data.topic.isEmpty()) {
//////                        // 应该只有一个imTopic
//////                        imTopic = ret.data.topic.get(0);
//////                    }
//////                    if (imTopic == null) { //视为失败
//////                        DatabaseManager.topicCreateFailed(mTopic);
//////                        return;
//////                    }
//////
//////                    DatabaseManager.migrateMockTopicToRealTopic(mTopic, imTopic);
//////                    // TODO:@xiaolong:刷新ui
//////
//////
//////                }
//////
//////                @Override
//////                public void onFail(YXRequestBase request, Error error) {
//////                    DatabaseManager.topicCreateFailed(mTopic);
//////                }
//////            });
//////
//////        } else {
//////            // TODO:@小龙 ：已经有对话，后续逻辑
////////            doSendMsg(msg, msgReqId);
//////
//////        }
////    }
////
////    /**
////     * 创建realTopic
////     *
////     * @param callback
////     */
////    private void createRealTopic(IYXHttpCallback<TopicCreateTopicResponse_new> callback) {
////        TopicCreateTopicRequest_new createTopicRequest = new TopicCreateTopicRequest_new();
////        createTopicRequest.imToken = Constants.imToken;
////        createTopicRequest.topicType = "1"; // 私聊
////        createTopicRequest.imMemberIds = Long.toString(Constants.imId) + "," + Long.toString(memberId);
////        createTopicRequest.fromGroupTopicId = mTopic.getFromTopic();
////        createTopicRequest.startRequest(TopicCreateTopicResponse_new.class, callback);
////    }
//
//    /**
//     * 创建msg时，生成msgid
//     *
//     * @return
//     */
//    private long getLatestMsgId() {
//        long latestMsgId;
//        if (mTopic.getMsgList() == null || mTopic.getMsgList().isEmpty()) {
//            //没有msg，mymsgId为-1
//            latestMsgId = -1;
//        } else { //有消息
//            //因为msglist是倒序，所以，只要拿到第一条数据，那么magId就是最大的。
//            latestMsgId = mTopic.getMsgList().get(0).getMsgId();
//        }
//        return latestMsgId;
//    }
//}
