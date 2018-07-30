package com.yanxiu.im.bean;

import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.sender.ISender;
import com.yanxiu.im.sender.SenderFactory;
import com.yanxiu.lib.yx_basic_library.base.bean.YXBaseBean;

/**
 * UI层使用的msg数据实体类
 * Created by 戴延枫 on 2018/5/8.
 */

public class MsgItemBean extends YXBaseBean {
    /**
     * 消息类型
     */
    public static final int MSG_TYPE_MYSELF = 0;//消息类型：我自己的消息
    public static final int MSG_TYPE_OTHER_PEOPLE = 1; //消息类型：其他人的消息
    private int type; //消息类型 MESSAGE_TYPE_MYSELF / MESSAGE_TYPE_OTHER_PEOPLE
    private int state = 1;// // 0-成功 1-正在传 2-失败
    private float progress;

    private ISender isender;

    public ISender getISender() {
        return isender;
    }

    public void setISender(ISender isender) {
        this.isender = isender;
    }

    /*日期字段*/
    private boolean showDate = false;

    public boolean isShowDate() {
        return showDate;
    }

    public void setShowDate(boolean showDate) {
        this.showDate = showDate;
    }


    //数据库字段 start
    private String reqId;        // 客户端生成的唯一id
    private long msgId;          // 由于client发送可能失败，所以msgId不作为主键
    private long topicId;       // 此msg所属的topic
    private long senderId;     // 此msg的owner
    private long sendTime;       // msg的发送时间

    private int contentType;     // 10-文本，20-图片，30-视频
    private String msg;          // txt怎插入msg，pic插入
    private String thumbnail;
    private String viewUrl;  //pic等的url
    private int width; //image-width
    private int height;//image-height
    private String localViewUrl;
    private long realMsgId; // http返回的，真正的msgid

    //数据库字段 end

    private DbMember member;

    private MsgItemBean() {
        //new sender
//        sender = new Sender();
    }

    public MsgItemBean(int type, int contentType) {
        this.contentType = contentType;
        setType(type);

    }

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
        member = DatabaseManager.getMemberById(senderId);
    }

    /**
     * 用于 点击进入 topic 后 更新 member 信息
     * */
    public void setMember(DbMember member) {
        this.member = member;
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

    private void setContentType(int contentType) {
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

    public int getType() {
        return type;
    }

    private void setType(int type) {
        this.type = type;
        if (isender == null && this.type == MSG_TYPE_MYSELF) {
            ISender sender = SenderFactory.createSender(this);
            setISender(sender);
        } else {
            isender = null;
        }
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public DbMember getMember() {
        return member;
    }

    public long getRealMsgId() {
        return realMsgId;
    }

    public void setRealMsgId(long realMsgId) {
        this.realMsgId = realMsgId;
    }
}
