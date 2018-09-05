package com.yanxiu.im.net;

/**
 * im当前用户心跳
 * Created by dyf 2018年9月5日16:15:04
 * wiki:http://wiki.yanxiu.com/pages/viewpage.action?pageId=12326677#id-用户、登录接口-%E7%94%A8%E6%88%B7%E3%80%81%E7%99%BB%E5%BD%95%E6%8E%A5%E5%8F%A3-2.3im%E5%BD%93%E5%89%8D%E7%94%A8%E6%88%B7%E5%BF%83%E8%B7%B3
 */

public class HeartBeatRequest extends ImRequestBase_new {

    /**
     * reqId            客户端唯一id
     * bizSource        业务来源
     * type             类型：app pc
     * onlineSeconds    在线有效时长，单位：秒
     */
    private String method = "online.heartbeat";
    private String type = "app";
    private String onlineSeconds = "70";
}
