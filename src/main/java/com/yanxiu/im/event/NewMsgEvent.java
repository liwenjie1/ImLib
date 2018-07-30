package com.yanxiu.im.event;

import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.lib.yx_basic_library.base.bean.YXBaseEvent;

/**
 * 收到mqtt的新消息
 * 发送者：{@link com.yanxiu.im.manager.MqttProtobufManager}
 * 接收者：{@link com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment}
 * Created by 戴延枫 on 2018/5/9.
 */

public class NewMsgEvent extends YXBaseEvent {
    public MsgItemBean msg;
}
