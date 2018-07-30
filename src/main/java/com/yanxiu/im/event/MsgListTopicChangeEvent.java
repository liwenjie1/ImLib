package com.yanxiu.im.event;

/**
 * Created by 朱晓龙 on 2018/5/24 14:51.
 * MQTT topicChange 消息
 * 发送者：{@link com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment}
 * 接收者：{@link com.yanxiu.im.business.msglist.activity.ImMsgListActivity}
 *
 */

public class MsgListTopicChangeEvent {
    public long topicId;

    public MsgListTopicChangeEvent(long topicId) {
        this.topicId = topicId;
    }
}
