package com.yanxiu.im.bean.net_bean;

import java.util.List;

/**
 * Created by cailei on 05/03/2018.
 */
/**
"data": {
        "imEvent": 121,
        "reqId": "1",
        "topicChange": null,
        "topic": null,
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
        },
*/
public class ImDataForMsg_new {
    public long imEvent;
    public String reqId;
    // 还有一堆不确定干什么用，暂时没写
    public List<ImMsg_new> topicMsg;
}
