package com.yanxiu.im.bean.net_bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by cailei on 05/03/2018.
 */

public class ImTopic_new {
    @SerializedName("id")
    public long topicId;
    public String topicName;
    public String topicType;    // 1-私聊，2-群聊

    public String topicGroup;

    public int state;
    public String createTime;   // 格式 2017-12-27 20:15:17
    public String topicChange;  // 见DbTopic::change
    public long latestMsgId;
    public long latestMsgTime;  // 格式 1514475540032



    public List<Member> members;
    /**
     * id : 1943
     * bizSource : 22
     * bizId : 688
     * topicLogo : null
     * topicType : 2
     * fromGroupTopicId : null
     * privateTopicKey : null
     * speak : 1
     * isRobot : 0
     * topicChange : 5
     * members : null
     * topicLogoMemberId : 0
     * personalConfig : null
     * personalConfigInfo : {"quite":0}
     */

    private int bizSource;
    private String bizId;
    private String topicLogo;
    private long fromGroupTopicId;
    private String privateTopicKey;
    private int speak;
    private int isRobot;
    private int topicLogoMemberId;
    private PersonalConfigInfoBean personalConfigInfo;


    public int getBizSource() {
        return bizSource;
    }

    public void setBizSource(int bizSource) {
        this.bizSource = bizSource;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getTopicLogo() {
        return topicLogo;
    }

    public void setTopicLogo(String topicLogo) {
        this.topicLogo = topicLogo;
    }


    public long getFromGroupTopicId() {
        return fromGroupTopicId;
    }

    public void setFromGroupTopicId(long fromGroupTopicId) {
        this.fromGroupTopicId = fromGroupTopicId;
    }

    public String getPrivateTopicKey() {
        return privateTopicKey;
    }

    public void setPrivateTopicKey(String privateTopicKey) {
        this.privateTopicKey = privateTopicKey;
    }

    public int getSpeak() {
        return speak;
    }

    public void setSpeak(int speak) {
        this.speak = speak;
    }

    public int getIsRobot() {
        return isRobot;
    }

    public void setIsRobot(int isRobot) {
        this.isRobot = isRobot;
    }


    public int getTopicLogoMemberId() {
        return topicLogoMemberId;
    }

    public void setTopicLogoMemberId(int topicLogoMemberId) {
        this.topicLogoMemberId = topicLogoMemberId;
    }


    public PersonalConfigInfoBean getPersonalConfigInfo() {
        return personalConfigInfo;
    }

    public void setPersonalConfigInfo(PersonalConfigInfoBean personalConfigInfo) {
        this.personalConfigInfo = personalConfigInfo;
    }

    public static class Member {
        // 其余字段暂时不用
        public long memberId;   // 就是imId
        public ImMember_new memberInfo;
        // topicID
        public long topicId;
        //
        public int memberRole;
    }

    public static class PersonalConfigInfoBean {
        /**
         * quite : 0
         */

        private int quite;

        public int getQuite() {
            return quite;
        }

        public void setQuite(int quite) {
            this.quite = quite;
        }
    }
}