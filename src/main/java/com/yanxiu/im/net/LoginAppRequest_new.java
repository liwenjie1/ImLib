package com.yanxiu.im.net;

/**
 * Created by cailei on 02/03/2018.
 * dyf:不需要了
 */

// 1.1 app登录接口
public class LoginAppRequest_new extends ImRequestBase_new {
    private String method="login.app";

    // bizToken 在父类，考虑每个请求都传一遍，以便和imToken一起追踪
}
