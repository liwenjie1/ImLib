package com.yanxiu.im.net;

/**
 * Created by Canghaixiao.
 * Time : 2017/11/8 14:31.
 * Function :
 */
public class GetImIdByUseridRequest extends ImRequestBase_new {
    public String method = "login.getMemberTopic";
    public String userId;
    public String fromGroupTopicId;

}
