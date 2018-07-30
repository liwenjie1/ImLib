package com.yanxiu.im.event;

import com.yanxiu.im.manager.MqttProtobufManager;
import com.yanxiu.lib.yx_basic_library.base.bean.YXBaseEvent;

/**
 * 收到mqtt的topic有change
 * 发送者：{@link com.yanxiu.im.manager.MqttProtobufManager}
 * 接收者：{@link com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment}
 * Created by 戴延枫 on 2018/5/9.
 */

public class TopicChangEvent extends YXBaseEvent {
    public long topicId;
    public MqttProtobufManager.TopicChange type;
}
