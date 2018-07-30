package com.yanxiu.im.net;

/**
 * 2.1 获取主题内容
 * Created by cailei on 05/03/2018.
 * wiki :http://wiki.yanxiu.com/pages/viewpage.action?pageId=12326683
 */

public class GetTopicMsgsRequest_new extends ImRequestBase_new {
    private String method = "topic.getTopicMsgs";

    public String topicId;
    public String startId;      // 起始消息id
    public String order;        // 排序：asc、desc 默认-desc
    public String dataNum;      // 每次获取消息数量，默认-20

    @Override
    protected boolean setCallBackInChildThread() {
        return true;
    }
}
