package com.yanxiu.im.db;

/**
 * 我发送的消息表
 * Created by 戴延枫 on 2018/5/7.
 */

public class DbMyMsg extends DbMsg {
    public enum State {
        Success,
        Sending,
        Failed
    }

    private int state;      // 0-成功 1-正在传 2-失败
    private long realMsgId; // http返回的，真正的msgid


    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getRealMsgId() {
        return realMsgId;
    }

    public void setRealMsgId(long realMsgId) {
        this.realMsgId = realMsgId;
    }
}
