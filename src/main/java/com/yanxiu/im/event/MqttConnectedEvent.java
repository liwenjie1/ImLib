package com.yanxiu.im.event;

import com.yanxiu.im.manager.MqttConnectManager;

/**
 * create by 朱晓龙 2018/8/30 下午1:42
 * sender {@link com.yanxiu.im.manager.MqttConnectManager#connectMqttServer(String, MqttConnectManager.MqttServerConnectCallback)}
 * receiver {@link com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment}
 * receiver {@link com.yanxiu.im.business.msglist.activity.ImMsgListActivity}
 *
 */
public class MqttConnectedEvent {
    public int eventId=0;
}
