package com.yanxiu.im.event;

/**
 * Created by 朱晓龙 on 2018/5/24 14:51.
 * 用户被某个topic 移除 通知当前聊天界面 并关闭
 * 发送者：{@link com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment}
 * 接收者：{@link com.yanxiu.im.business.msglist.activity.ImMsgListActivity}
 *
 */


public class MsgListTopicRemovedEvent {
   public long topicId=0;

    public MsgListTopicRemovedEvent(long topicId) {
        this.topicId = topicId;
    }
}
