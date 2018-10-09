package com.yanxiu.im.net;

/**
 * Created by Canghaixiao.
 * Time : 2017/11/7 16:27.
 * Function :
 */
public class GetImIdByUserIdResponse extends ImResponseBase_new {

    public ImIdData data;

    public class ImIdData {
        public TopicInfo topic;
        public int memberId;

        public class TopicInfo{
            public long id;
            public int bizSource;
            public String topicName;
            public String topicLogo;
            public int topicType;
            public String topicGroup;
            public String fromGroupTopicId;
            public int state;
            public int topicChange;
            public int speak;
            public String createTime;
            public String latestMsgId;
            public String latestMsgTime;
        }
    }

}
