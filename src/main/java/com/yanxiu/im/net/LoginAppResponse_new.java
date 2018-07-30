package com.yanxiu.im.net;


import com.yanxiu.im.bean.net_bean.ImMember_new;

/**
 * Created by cailei on 02/03/2018.
 */

public class LoginAppResponse_new extends ImResponseBase_new {
    public Data data;

    public class Data {
        public String imToken;
        public ImMember_new imMember;
    }
}
