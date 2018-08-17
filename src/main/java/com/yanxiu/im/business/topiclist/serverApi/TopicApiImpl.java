package com.yanxiu.im.business.topiclist.serverApi;

import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.im.manager.RequestQueueManager;
import com.yanxiu.im.net.GetTopicMsgsRequest_new;
import com.yanxiu.im.net.GetTopicMsgsResponse_new;
import com.yanxiu.im.net.TopicGetMemberTopicsRequest_new;
import com.yanxiu.im.net.TopicGetMemberTopicsResponse_new;
import com.yanxiu.im.net.TopicGetTopicsRequest_new;
import com.yanxiu.im.net.TopicGetTopicsResponse_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.ArrayList;

import okhttp3.Request;

/**
 * create by 朱晓龙 2018/8/16 下午1:48
 */
public class TopicApiImpl {
    private static RequestQueueManager mRequestQueueManager = new RequestQueueManager();


    public static void requestUserTopicList(String token, final TopicListCallback<ImTopic_new> callback) {
        TopicGetMemberTopicsRequest_new request = new TopicGetMemberTopicsRequest_new();
        request.imToken = token;
        request.startRequest(TopicGetMemberTopicsResponse_new.class, new IYXHttpCallback<TopicGetMemberTopicsResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, TopicGetMemberTopicsResponse_new ret) {
                if (ret == null || ret.code != 0 || ret.data == null || ret.data.topic == null) {
                    callback.onGetTopicList(null);
                    return;
                }
                callback.onGetTopicList((ArrayList<ImTopic_new>) ret.data.topic);

            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                callback.onGetTopicList(null);
            }
        });

    }


    public static void requestTopicInfo(String token, String topicId, final TopicListCallback<ImTopic_new> callback) {
        TopicGetTopicsRequest_new memberRequest = new TopicGetTopicsRequest_new();
        memberRequest.imToken = token;
        memberRequest.topicIds = topicId;
        mRequestQueueManager.addRequest(memberRequest, TopicGetTopicsResponse_new.class, new IYXHttpCallback<TopicGetTopicsResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, TopicGetTopicsResponse_new ret) {
                if (ret == null || ret.code != 0 || ret.data == null || ret.data.topic == null) {
                    callback.onGetTopicList(null);
                    return;
                }
                callback.onGetTopicList((ArrayList<ImTopic_new>) ret.data.topic);

            }

            @Override
            public void onFail(YXRequestBase request, Error error) {

            }
        });
    }

    /**
     * 获取分页 消息列表
     * */

    public static void requestTopicMsgPages(String token,String topicId,String startId,final MsgListCallback<ImMsg_new> callback){
        GetTopicMsgsRequest_new msgsRequest = new GetTopicMsgsRequest_new();
        msgsRequest.imToken = token;
        msgsRequest.topicId = topicId;
        msgsRequest.startId=startId;
        mRequestQueueManager.addRequest(msgsRequest, GetTopicMsgsResponse_new.class, new IYXHttpCallback<GetTopicMsgsResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetTopicMsgsResponse_new ret) {
                if (ret == null || ret.code != 0 || ret.data == null || ret.data.topicMsg == null) {
                    callback.onGetMsgList(null);
                } else {
                    callback.onGetMsgList((ArrayList<ImMsg_new>) ret.data.topicMsg);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {

            }
        });
    }


    /**
     * 获取最新一页 消息
     * 指定 开始 msgid 为最大值
     */
    public static void requestLatestTopicMsgs(String token, String topicId, final MsgListCallback<ImMsg_new> callback) {
       requestTopicMsgPages(token,topicId,Long.MAX_VALUE+"",callback);

    }

    public interface MsgListCallback<E> {
        void onGetMsgList(ArrayList<E> dataList);
    }

    public interface TopicListCallback<E> {
        void onGetTopicList(ArrayList<E> topicList);
    }


}
