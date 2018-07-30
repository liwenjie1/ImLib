package com.yanxiu.im.net;

/**
 * Created by cailei on 02/03/2018.
 */

// 1.2 获取mqtt服务器配置
public class PolicyMqttServerRequest_new extends ImRequestBase_new {
    private String method="policy.mqtt.server";

    public String type = "tcp";     // 连接方式： tcp ws
}
