package com.yanxiu.im.net;

/**
 * 2.2 发表文字内容
 * Created by cailei on 05/03/2018.
 * wiki :http://wiki.yanxiu.com/pages/viewpage.action?pageId=12326683
 */

public class SaveTextMsgRequest_new extends ImRequestBase_new {
    private String method = "topic.saveTextMsg";
    public String topicId;
    public String msg;

    @Override
    protected HttpType httpType() {
        return HttpType.POST;
    }
}
