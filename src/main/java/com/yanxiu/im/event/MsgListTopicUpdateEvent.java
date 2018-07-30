package com.yanxiu.im.event;
/**
 * Created by 朱晓龙 on 2018/5/24 14:51.
 *  一般为http请求后 获取了最新的topic 信息（members, msglsit等）通知给MsgActivity更新
 * 发送者：{@link com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment}
 * 接收者：{@link com.yanxiu.im.business.msglist.activity.ImMsgListActivity}
 *
 */


public class MsgListTopicUpdateEvent {
    public long topicId;

    public MsgListTopicUpdateEvent(long topicId) {
        this.topicId = topicId;
    }
}
