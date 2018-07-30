package com.yanxiu.im.db;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

/**
 * msg表
 * Created by 戴延枫 on 2018/5/7.
 */

public class DbMsg extends DataSupport {
    @Column(unique = true, defaultValue = "unknown", nullable = false)
    private String reqId;        // 客户端生成的唯一id
    protected long msgId;          // 由于client发送可能失败，所以msgId不作为主键---
    protected long topicId;       // 此msg所属的topic
    protected long senderId;     // 此msg的owner
    protected long sendTime;       // msg的发送时间

    protected int contentType;     // 10-文本，20-图片，30-视频
    protected String msg;          // txt怎插入msg，pic插入
    protected String thumbnail;
    protected String viewUrl;  //pic等的url
    protected int width; //image-width
    protected int height;//image-height
    protected String localViewUrl;

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }

    public long getTopicId() {
        return topicId;
    }

    public void setTopicId(long topicId) {
        this.topicId = topicId;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getViewUrl() {
        return viewUrl;
    }

    public void setViewUrl(String viewUrl) {
        this.viewUrl = viewUrl;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getLocalViewUrl() {
        return localViewUrl;
    }

    public void setLocalViewUrl(String localViewUrl) {
        this.localViewUrl = localViewUrl;
    }
}
