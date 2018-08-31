package com.yanxiu.im.manager;

import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.im.net.GetTopicMsgsRequest_new;
import com.yanxiu.im.net.GetTopicMsgsResponse_new;
import com.yanxiu.im.net.TopicCreateTopicRequest_new;
import com.yanxiu.im.net.TopicCreateTopicResponse_new;
import com.yanxiu.im.net.TopicGetMemberTopicsRequest_new;
import com.yanxiu.im.net.TopicGetMemberTopicsResponse_new;
import com.yanxiu.im.net.TopicGetTopicsResponse_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Request;

public class HttpRequestManager {

    public RequestQueueManager requestQueueManager = new RequestQueueManager();


    private ArrayList<UUID> requests;

    public HttpRequestManager() {
        requests=new ArrayList<>();
    }
    public void clearReuqest(){
        if (requests != null) {
            for (UUID request : requests) {

            }
        }
    }

    /**
     * 获取用户的 topic 列表
     */
    public void requestUserTopicList(String imToken, final GetTopicListCallback<ImTopic_new> callback) {
        TopicGetMemberTopicsRequest_new request = new TopicGetMemberTopicsRequest_new();
        request.imToken = imToken;
        requestQueueManager.addRequest(request, TopicGetMemberTopicsResponse_new.class, new IYXHttpCallback<TopicGetMemberTopicsResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, TopicGetMemberTopicsResponse_new ret) {
                if (ret.code != 0 || ret.data == null || ret.data.topic == null) {
                    if (callback != null) {
                        callback.onGetFailure();
                    }
                    return;
                }
                if (callback != null) {
                    callback.onGetTopicList(ret.data.topic);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                if (callback != null) {
                    callback.onGetFailure();
                }
            }
        });
    }

    public interface GetTopicListCallback<E> {
        void onGetTopicList(List<E> topicList);

        void onGetFailure();
    }


    /**
     * 获取 topic 的 member 列表
     */
    public void requestTopicMemberList(String imToken, String topidId, final GetTopicMemberListCallback<ImTopic_new> callback) {
        com.yanxiu.im.net.TopicGetTopicsRequest_new getTopicsRequest = new com.yanxiu.im.net.TopicGetTopicsRequest_new();
        getTopicsRequest.imToken = imToken;
        getTopicsRequest.topicIds = topidId;
        requestQueueManager.addRequest(getTopicsRequest, com.yanxiu.im.net.TopicGetTopicsResponse_new.class, new IYXHttpCallback<TopicGetTopicsResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, com.yanxiu.im.net.TopicGetTopicsResponse_new ret) {
                /*没有 topic 信息*/
                if (ret.code != 0 || ret.data == null || ret.data.topic == null || ret.data.topic.size() == 0) {
                    if (callback != null) {
                        callback.onGetFailure();
                    }
                    return;
                }
                ImTopic_new imTopic = ret.data.topic.get(0);
                //将服务器返回的结果保存到数据库 并生成 topicitembean
                if (callback != null) {
                    callback.onGetTopicMembers(imTopic);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                if (callback != null) {
                    callback.onGetFailure();
                }
            }
        });
    }

    public interface GetTopicMemberListCallback<E> {
        void onGetTopicMembers(E topicWithMembers);

        void onGetFailure();
    }

    /**
     * 获取 topic 的 msg、列表
     */
    public void requestTopicMsgList(String imToken, long topicId, final GetTopicMsgListCallback<ImMsg_new> callback) {
        //更新msg
        GetTopicMsgsRequest_new getTopicMsgsRequest = new GetTopicMsgsRequest_new();
        getTopicMsgsRequest.imToken = imToken;
        getTopicMsgsRequest.topicId = Long.toString(topicId);
        //如果是最新加入的topic 没有消息记录 赋予latestmsgid 为long最大值
        //由于是获取最新消息 所以 请求startid 采用Long.MAXVALUE
        getTopicMsgsRequest.startId = String.valueOf(Long.MAX_VALUE);
        getTopicMsgsRequest.order = "desc";
        requestQueueManager.addRequest(getTopicMsgsRequest, GetTopicMsgsResponse_new.class, new IYXHttpCallback<GetTopicMsgsResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetTopicMsgsResponse_new ret) {
                if (ret.code != 0 || ret.data == null || ret.data.topicMsg == null) {
                    if (callback != null) {
                        callback.onGetFailure();
                    }
                    return;
                }
                if (callback != null) {
                    callback.onGetTopicMsgList(ret.data.topicMsg);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                if (callback != null) {
                    callback.onGetFailure();
                }
            }
        });
    }

    public interface GetTopicMsgListCallback<E> {
        void onGetTopicMsgList(List<E> msgList);

        void onGetFailure();
    }


    /**
     * 创建 新的 topic
     */
    public void requestCreateNewPrivateTopic(String imToken, String fromTopicId, long memberId, long myImId, final CreatePrivateTopicCallback<ImTopic_new> callback) {
        TopicCreateTopicRequest_new createTopicRequest = new TopicCreateTopicRequest_new();
        createTopicRequest.imToken = imToken;
        createTopicRequest.topicType = "1"; // 私聊
        createTopicRequest.imMemberIds = memberId + "," + myImId;
        createTopicRequest.fromGroupTopicId = fromTopicId;
        requestQueueManager.addRequest(createTopicRequest, TopicCreateTopicResponse_new.class, new IYXHttpCallback<TopicCreateTopicResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, final TopicCreateTopicResponse_new ret) {
                //首先检查参数
                if (ret == null || ret.code != 0 || ret.data == null) {
                    //这种情况 应该是 请求本身出现错误
                    if (callback != null) {
                        callback.onFailure();
                    }
                    return;
                }
                if (ret.data.topic == null || ret.data.topic.size() == 0) {
                    //这种情况可能是 已经存在了 需要创建的 topic
                    if (callback != null) {
                        callback.onFailure();
                    }
                    return;
                }

                final ImTopic_new imTopicNew = ret.data.topic.get(0);
                //回调给上层
                if (callback != null) {
                    callback.onCreated(imTopicNew);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                if (callback != null) {
                    callback.onFailure();
                }
            }
        });
    }


    public interface CreatePrivateTopicCallback<E> {
        void onCreated(E topic);

        void onFailure();
    }


}
