package com.yanxiu.im.event;

import com.yanxiu.im.manager.MqttProtobufManager;
import com.yanxiu.lib.yx_basic_library.base.bean.YXBaseEvent;

/**
 * 收到http、mqtt的topic更新
 * 发送者：{@link MqttProtobufManager}
 * 接收者：{@link com.yanxiu.im.activity.ImMsgListActivity_new}
 * Created by 戴延枫 on 2018/5/9.
 */

public class TopicUpdateEvent extends YXBaseEvent {
    public long topicId;
//    public List<DbMsg> newMsgs;
}
