package com.yanxiu.im.net;

/**
 * Created by cailei on 02/03/2018.
 */

public class PolicyConfigResponse_new extends ImResponseBase_new {
    public Data data;

    public class Data {


        private String mqttServer;

        public String getMqttServer() {
            return mqttServer;
        }

        public void setMqttServer(String mqttServer) {
            this.mqttServer = mqttServer;
        }

    }
}
