package com.yanxiu.im.net;

/**
 * 1.6 获取多个主题
 * Created by cailei on 05/03/2018.
 * wiki: http://wiki.yanxiu.com/pages/viewpage.action?pageId=12326683
 */

public class TopicGetTopicsRequest_new extends ImRequestBase_new {
    private String method = "topic.getTopics";

    public String topicIds;   // 主题id，多个逗号分隔，每次最多10个

    @Override
    protected boolean setCallBackInChildThread() {
        return true;
    }
}
