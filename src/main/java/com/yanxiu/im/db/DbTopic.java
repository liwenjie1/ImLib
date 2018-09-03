package com.yanxiu.im.db;

import com.yanxiu.im.bean.MsgItemBean;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * topic表
 * Created by 戴延枫 on 2018/5/7.
 */

public class DbTopic extends DataSupport {
    @Column(unique = true, defaultValue = "unknown", nullable = false)
    private long topicId;
    private String name;
    private String type;//主题类型： 1-私聊 2-群聊
    private String change;  // 如果topic有名称、人员变动，则server的值和db值不相等，需要重新获取topic信息

    private String group;
    private String fromTopic;   // 从哪个topic来的私聊

    private long fromGroup;   // 来源群id， 私聊如果来自班级群，传群id。否则不传
    private boolean showDot = false;

    private boolean alreadyDeletedLocalTopic = false;//本地删除了对话,默认为false
    private long latestMsgIdWhenDeletedLocalTopic = -1;//本地删除了对话时，最后的消息id

    private List<DbMember> members = new ArrayList<>();

    // 只为UI显示用，不做数据库存储用
    @Column(ignore = true)
    public long latestMsgId;
    @Column(ignore = true)
    public long latestMsgTime;
    @Column(ignore = true)
    public List<MsgItemBean> mergedMsgs = new ArrayList<>();


    public long getTopicId() {
        return topicId;
    }

    public void setTopicId(long topicId) {
        this.topicId = topicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public List<DbMember> getMembers() {
        return members;
    }

    public void setMembers(List<DbMember> members) {
        this.members = members;
    }

    public boolean isShowDot() {
        return showDot;
    }

    public void setShowDot(boolean showDot) {
        this.showDot = showDot;
    }

    public long getFromGroup() {
        return fromGroup;
    }

    public void setFromGroup(long fromGroup) {
        this.fromGroup = fromGroup;
    }

    public long getLatestMsgId() {
        return latestMsgId;
    }

    public void setLatestMsgId(long latestMsgId) {
        this.latestMsgId = latestMsgId;
    }

    public long getLatestMsgTime() {
        return latestMsgTime;
    }

    public void setLatestMsgTime(long latestMsgTime) {
        this.latestMsgTime = latestMsgTime;
    }

    public List<MsgItemBean> getMergedMsgs() {
        return mergedMsgs;
    }

    public void setMergedMsgs(List<MsgItemBean> mergedMsgs) {
        this.mergedMsgs = mergedMsgs;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getFromTopic() {
        return fromTopic;
    }

    public void setFromTopic(String fromTopic) {
        this.fromTopic = fromTopic;
    }

    public boolean isAlreadyDeletedLocalTopic() {
        return alreadyDeletedLocalTopic;
    }

    public void setAlreadyDeletedLocalTopic(boolean alreadyDeletedLocalTopic) {
        this.alreadyDeletedLocalTopic = alreadyDeletedLocalTopic;
    }

    public long getLatestMsgIdWhenDeletedLocalTopic() {
        return latestMsgIdWhenDeletedLocalTopic;
    }

    public void setLatestMsgIdWhenDeletedLocalTopic(long latestMsgIdWhenDeletedLocalTopic) {
        this.latestMsgIdWhenDeletedLocalTopic = latestMsgIdWhenDeletedLocalTopic;
    }
}
