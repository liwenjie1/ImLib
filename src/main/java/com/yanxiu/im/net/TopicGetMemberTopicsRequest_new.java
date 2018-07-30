package com.yanxiu.im.net;

/**
 * Created by cailei on 05/03/2018.
 * 与TopicGetTopics不同，这个请求里不带有members列表信息
 * wiki:http://wiki.yanxiu.com/pages/viewpage.action?pageId=12326683
 */

// 1.7 获取当前用户的主题列表
public class TopicGetMemberTopicsRequest_new extends ImRequestBase_new {
    private String method="topic.getMemberTopics";

    @Override
    protected boolean setCallBackInChildThread() {
        return true;
    }
}
