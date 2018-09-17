package com.yanxiu.im.net;

/**
 * Created by cailei on 02/03/2018.
 * wiki:http://wiki.yanxiu.com/pages/viewpage.action?pageId=12327079
 */

// 1.2 获取mqtt服务器配置
public class PolicyConfigRequest_new extends ImRequestBase_new {
    public String method="policy.mqtt.server";

    public String type ="tcp";
}
