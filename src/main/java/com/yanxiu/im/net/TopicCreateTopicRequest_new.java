package com.yanxiu.im.net;

/**
 * Created by cailei on 05/03/2018.
 * wiki:http://wiki.yanxiu.com/pages/viewpage.action?pageId=12327079
 */

// 1.1 创建主题
public class TopicCreateTopicRequest_new extends ImRequestBase_new {
    private String method = "topic.createTopic";

    public String topicType;   // int型，1-私聊 2-群聊
    public String topicName;
    public String yxUsers;
    public String imMemberIds;

    public String fromGroupTopicId;
}
