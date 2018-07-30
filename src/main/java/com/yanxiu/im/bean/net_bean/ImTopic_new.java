package com.yanxiu.im.bean.net_bean;

import com.google.gson.annotations.SerializedName;
import com.yanxiu.im.bean.net_bean.ImMember_new;

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
    public class Member {
        // 其余字段暂时不用
        public long memberId;   // 就是imId
        public ImMember_new memberInfo;
    }
}