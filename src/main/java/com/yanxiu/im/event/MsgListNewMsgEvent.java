package com.yanxiu.im.event;

/**
 * Created by 朱晓龙 on 2018/5/24 14:51.
 * mqtt 获取到新消息 通知给当前聊天界面
 * 发送者：{@link com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment}
 * 接收者：{@link com.yanxiu.im.business.msglist.activity.ImMsgListActivity}
 *
 */

public class MsgListNewMsgEvent {
    public long topicId;

    public MsgListNewMsgEvent(long topicId) {
        this.topicId = topicId;
    }
}
