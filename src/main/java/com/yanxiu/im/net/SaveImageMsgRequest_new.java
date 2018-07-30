package com.yanxiu.im.net;

/**
 * 2.3发表图片内容
 * Created by cailei on 05/03/2018.
 * wiki :http://wiki.yanxiu.com/pages/viewpage.action?pageId=12326683
 */

public class SaveImageMsgRequest_new extends ImRequestBase_new {
    private String method = "topic.saveImageMsg";
    public String topicId;
    public String rid;
    public String width;
    public String height;

    @Override
    protected HttpType httpType() {
        return HttpType.POST;
    }
}
