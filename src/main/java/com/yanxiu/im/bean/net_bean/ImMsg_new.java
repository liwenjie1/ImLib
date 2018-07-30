package com.yanxiu.im.bean.net_bean;

import com.google.gson.annotations.SerializedName;

/**
 * Created by cailei on 05/03/2018.
 */
/**
"topicMsg": [
        {
        "id": 13,
        "bizSource": 1,
        "topicId": 11,
        "senderId": 7,
        "reqId": "123",
        "contentType": 10,
        "content": null,
        "sendTime": 1514457316877,
        "contentData": {
        "msg": "1231234",
        "rid": null,
        "thumbnail": null,
        "viewUrl": null,
        "width": 0,
        "height": 0
        }
        }
        ]
*/
public class ImMsg_new {
    @SerializedName("id")
    public long msgId;
    public long topicId;
    public long senderId;
    public int contentType;
    public long sendTime;
    public String reqId;
    public ContentData contentData;

    public static class ContentData {
        public String msg;
        public String thumbnail;
        public String viewUrl;
        public int width;
        public int height;
    }
}
