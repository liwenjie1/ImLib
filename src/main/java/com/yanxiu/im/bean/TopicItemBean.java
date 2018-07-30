package com.yanxiu.im.bean;

import com.yanxiu.im.db.DbMember;
import com.yanxiu.lib.yx_basic_library.base.bean.YXBaseBean;

import java.util.ArrayList;
import java.util.List;

/**
 * UI层使用的topic数据实体类
 * Created by 戴延枫 on 2018/5/8.
 */

public class TopicItemBean extends YXBaseBean {
    private long topicId;
    private String name;
    private String type;//主题类型： 1-私聊 2-群聊
    private String change;  // 如果topic有名称、人员变动，则server的值和db值不相等，需要重新获取topic信息

    private String group;
    private String fromTopic;   // 从哪个topic来的私聊

    private long fromGroup;   // 来源群id， 私聊如果来自班级群，传群id。否则不传
    private boolean showDot = false;

    private List<DbMember> members = new ArrayList<>();

    private long latestMsgId;
    private long latestMsgTime;
    private List<MsgItemBean> msgList = new ArrayList<>();

    /**
     * 最有一条http请求成功的msg
     * */
    private long requestMsgId =Long.MAX_VALUE;

    public long getRequestMsgId() {
        return requestMsgId;
    }

    public void setRequestMsgId(long requestMsgId) {
        this.requestMsgId = requestMsgId;
    }

    public MsgItemBean getLatestMsg() {
        if (msgList != null&&msgList.size()>0) {
            //倒序：取第一个 正序：取最后一个
           return msgList.get(0);
        }
        return null;
    }

    public long getLatestMsgId() {
        return latestMsgId;
    }

    /**
     * 创建msg时，生成msgid
     *

     * @return
     */
    public long generateMyMsgId() {
        long latestMsgId;
        if (getMsgList() == null || getMsgList().isEmpty()) {
            //没有msg，mymsgId为-1
            latestMsgId = -1;
        } else { //有消息
            //因为msglist是倒序，所以，只要拿到第一条数据，那么magId就是最大的。
            latestMsgId = getMsgList().get(0).getMsgId();
        }
        return latestMsgId;
    }

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

    public long getFromGroup() {
        return fromGroup;
    }

    public void setFromGroup(long fromGroup) {
        this.fromGroup = fromGroup;
    }

    public boolean isShowDot() {
        return showDot;
    }

    public void setShowDot(boolean showDot) {
        this.showDot = showDot;
    }

    public List<DbMember> getMembers() {
        return members;
    }

    public void setMembers(List<DbMember> members) {
        //TODO：遍历现有的，已经存在需替换，不存在就增加
        this.members = members;
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

    public List<MsgItemBean> getMsgList() {
        return msgList;
    }

    public void setMsgList(List<MsgItemBean> msgList) {
        this.msgList = msgList;
    }
}
