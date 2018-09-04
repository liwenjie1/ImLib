package com.yanxiu.im.net;

import com.yanxiu.im.bean.net_bean.ImTopic_new;

import java.util.List;

public class UpdatePublicConfigResponse extends ImResponseBase_new {
    /**
     * data : {"imEvent":102,"reqId":"1","topicChange":null,"topic":[{"id":11,"bizSource":1,"bizId":"123","topicName":"","topicType":2,"topicGroup":"英语组","privateTopicKey":null,"topicLogoMemberId":0,"state":1,"topicChange":1,"speak":1,"personalConfig":{"quite":0},"createTime":"2017-12-27 20:15:17","latestMsgId":16,"latestMsgTime":1514475540032,"members":null}],"topicMsg":null}
     */

    private UpdatePersonalConfigResponse.DataBean data;

    public UpdatePersonalConfigResponse.DataBean getData() {
        return data;
    }

    public void setData(UpdatePersonalConfigResponse.DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * imEvent : 102
         * reqId : 1
         * topicChange : null
         * topic : [{"id":11,"bizSource":1,"bizId":"123","topicName":"","topicType":2,"topicGroup":"英语组","privateTopicKey":null,"topicLogoMemberId":0,"state":1,"topicChange":1,"speak":1,"personalConfig":{"quite":0},"createTime":"2017-12-27 20:15:17","latestMsgId":16,"latestMsgTime":1514475540032,"members":null}]
         * topicMsg : null
         */

        private int imEvent;
        private String reqId;
        private String topicChange;
        private String topicMsg;
        private List<ImTopic_new> topic;

        public int getImEvent() {
            return imEvent;
        }

        public void setImEvent(int imEvent) {
            this.imEvent = imEvent;
        }

        public String getReqId() {
            return reqId;
        }

        public void setReqId(String reqId) {
            this.reqId = reqId;
        }

        public String getTopicChange() {
            return topicChange;
        }

        public void setTopicChange(String topicChange) {
            this.topicChange = topicChange;
        }

        public String getTopicMsg() {
            return topicMsg;
        }

        public void setTopicMsg(String topicMsg) {
            this.topicMsg = topicMsg;
        }

        public List<ImTopic_new> getTopic() {
            return topic;
        }

        public void setTopic(List<ImTopic_new> topic) {
            this.topic = topic;
        }

    }
}
