package com.yanxiu.im.event;

/**
 * Created by 朱晓龙 on 2018/5/24 14:51.
 * http拉取topic后，有mockTopic同realTopic合并完成
 * 发送者：{@link com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment}
 * 接收者：{@link com.yanxiu.im.business.msglist.activity.ImMsgListActivity}
 *
 */


public class MsgListMigrateMockTopicEvent {
   public long topicId;

    public MsgListMigrateMockTopicEvent(long topicId) {
        this.topicId = topicId;
    }
}
