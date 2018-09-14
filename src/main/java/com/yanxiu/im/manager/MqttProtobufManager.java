package com.yanxiu.im.manager;

import android.os.Looper;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.business.utils.ImServerDataChecker;
import com.yanxiu.im.event.NewMsgEvent;
import com.yanxiu.im.event.TopicChangEvent;
import com.yanxiu.im.event.TopicUpdateEvent;
import com.yanxiu.im.protobuf.ImMqttProto;
import com.yanxiu.im.protobuf.MqttMsgProto;
import com.yanxiu.im.protobuf.TopicGetProto;
import com.yanxiu.im.protobuf.TopicMsgProto;

import org.greenrobot.eventbus.EventBus;

/**
 * TODO：此处完全借鉴蔡雷，目前看无需修改
 * Created by 戴延枫 on 2018/5/7.
 */

public class MqttProtobufManager {
    public enum TopicChange {
        AddTo,//111:主题添加新成员和新主题
        RemoveFrom,//112：主题删除成员
        TopicChange//101 topic设置 更新
    }
    public static final String TAG="mqttProtobuf";

    public static final int EVENT_TOPIC_SET_QUITE = 102;
    public static final int EVENT_TOPIC_ADD_MEMBER = 111;
    public static final int EVENT_TOPIC_REMOVE_MEMBER = 112;
    public static final int EVENT_TOPIC_REQUEST_FULLINFO = 101;


    public static final int EVENT_TOPIC_NEWMSG_ARRAVED = 121;


    public static void dealWithData(byte[] rawData) throws InvalidProtocolBufferException {
        MqttMsgProto.MqttMsg mqttMsg = MqttMsgProto.MqttMsg.parseFrom(rawData);

        //目前只有一种type，且type为""
        //if (mqttMsg.getType() == "xxx") {
        // 是im的消息
        ImMqttProto.ImMqtt imMqtt = ImMqttProto.ImMqtt.parseFrom(mqttMsg.getData());
        Log.i(TAG, "dealWithData: ");
        switch (imMqtt.getImEvent()) {
            //topic 有新成员加入
            case EVENT_TOPIC_ADD_MEMBER: {
                Log.i(TAG, "dealWithData: ADD MEMBER");
                for (ByteString item : imMqtt.getBodyList()) {
                    TopicGetProto.TopicGet topicProto = TopicGetProto.TopicGet.parseFrom(item);
                    long topicId = topicProto.getTopicId();
                    // EventBus发现topic更新
                    onTopicChange(topicId, TopicChange.AddTo);
                }
            }
            break;
            //topic 有成员被移除
            case EVENT_TOPIC_REMOVE_MEMBER: {
                Log.i(TAG, "dealWithData: REMOVE MEMBER");
                for (ByteString item : imMqtt.getBodyList()) {
                    TopicGetProto.TopicGet topicProto = TopicGetProto.TopicGet.parseFrom(item);
                    long topicId = topicProto.getTopicId();
                    // EventBus发现topic更新
                    onTopicChange(topicId, TopicChange.RemoveFrom);
                }
            }
            break;
            //topic 收到新消息
            case EVENT_TOPIC_NEWMSG_ARRAVED: {
                Log.i(TAG, "dealWithData: NEW MSG ARRAVED");
                for (ByteString item : imMqtt.getBodyList()) {
                    TopicMsgProto.TopicMsg msgProto = TopicMsgProto.TopicMsg.parseFrom(item);
                    ImMsg_new msg = new ImMsg_new();
                    msg.reqId = msgProto.getReqId();
                    msg.msgId = msgProto.getId();
                    msg.topicId = msgProto.getTopicId();
                    msg.senderId = msgProto.getSenderId();
                    msg.contentType = msgProto.getContentType();
                    msg.sendTime = msgProto.getSendTime();
                    msg.contentData = new ImMsg_new.ContentData();
                    msg.contentData.msg = msgProto.getContentData().getMsg();
                    msg.contentData.viewUrl = msgProto.getContentData().getViewUrl();
                    msg.contentData.width = msgProto.getContentData().getWidth();
                    msg.contentData.height = msgProto.getContentData().getHeight();
                    // EventBus发现topic更新
                    onNewMsg(msg);
                }
            }
            break;
            //topic 信息有更新
            case EVENT_TOPIC_REQUEST_FULLINFO: {
                Log.i(TAG, "dealWithData: UPDATE TOPIC INFO");
                for (ByteString item : imMqtt.getBodyList()) {
                    TopicGetProto.TopicGet topicProto = TopicGetProto.TopicGet.parseFrom(item);
                    long topicId = topicProto.getTopicId();
                    // EventBus发现topic更新
                    onTopicChange(topicId, TopicChange.TopicChange);
                }
            }
            break;
        }

    }
    /**
     * 收到新消息
     *
     * @param msg
     */
    public static void onNewMsg(ImMsg_new msg) {
        if (Looper.myLooper() == Looper.getMainLooper()) { // UI主线程
            Log.d("Tag", "main thread");
        } else { // 非UI主线程
            Log.d("Tag", "other thread");
        }

        /*
         *检查 msg 数据格式是否合法  排除空指针
         * */
        if (!ImServerDataChecker.imMsgCheck(msg)) {
            return;
        }

        NewMsgEvent event = new NewMsgEvent();
        MsgItemBean msgBean = DatabaseManager.updateDbMsgWithImMsg(msg, Constants.imId);
        event.msg = msgBean;
        EventBus.getDefault().post(event);
    }

    /**
     * topic有变化：
     * 1.新topic
     * 2.tpoic有新成员
     * 3.topic删除成员
     *
     * @param topicId
     * @param type
     */
    public static void onTopicChange(long topicId, TopicChange type) {
        TopicChangEvent event = new TopicChangEvent();
        event.topicId = topicId;
        event.type = type;
        EventBus.getDefault().post(event);

    }

    /**
     * 通知msgListActivity有topic变化（http请求msg后更新topic）
     *
     * @param topicId
     */
    public static void onTopicUpdate(long topicId) {
        TopicUpdateEvent event = new TopicUpdateEvent();
        event.topicId = topicId;
        EventBus.getDefault().post(event);
    }
}
