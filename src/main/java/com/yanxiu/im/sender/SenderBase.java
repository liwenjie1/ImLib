package com.yanxiu.im.sender;

import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.db.DbMyMsg;
import com.yanxiu.im.manager.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 消息sender基类,实现部分公共方法
 * Created by Hu Chao on 18/5/15.
 */
public abstract class SenderBase implements ISender {

    //待发送的消息
    protected MsgItemBean mMsgItemBean;

    //状态监听接口
    protected List<ISenderListener> mSenderListeners;


    /**
     * @param msgItemBean 不能为null
     */
    protected SenderBase(MsgItemBean msgItemBean) {
        mSenderListeners = new ArrayList<>();
        mMsgItemBean = msgItemBean;
    }

    /**
     * 添加监听器
     *
     * @param senderListener
     */
    @Override
    public void addSenderListener(ISenderListener senderListener) {
        mSenderListeners.add(senderListener);
    }

    /**
     * 移除监听器
     *
     * @param senderListener
     */
    @Override
    public void removeSenderListener(ISenderListener senderListener) {
        mSenderListeners.remove(senderListener);
    }

    /**
     * 发送消息成功
     */
    protected void handleSuccess() {
        mMsgItemBean.setISender(null);
        mMsgItemBean.setProgress(0);
        mMsgItemBean.setState(DbMyMsg.State.Success.ordinal());
        for (ISenderListener senderListener : mSenderListeners) {
            senderListener.OnSuccess(this);
        }
        DatabaseManager.updateDbMsgWithMsgItemBean(mMsgItemBean);
    }

    /**
     * 发送消息失败
     */
    protected void handleFail() {
        mMsgItemBean.setISender(null);
        mMsgItemBean.setProgress(0);
        mMsgItemBean.setState(DbMyMsg.State.Failed.ordinal());
        for (ISenderListener senderListener : mSenderListeners) {
            senderListener.OnFail(this);
        }
        DatabaseManager.updateDbMsgWithMsgItemBean(mMsgItemBean);
    }

    /**
     * 发送消息进度
     */
    protected void handleProgress(double progress) {
        mMsgItemBean.setProgress((float) progress);
        for (ISenderListener senderListener : mSenderListeners) {
            senderListener.OnProgress(progress);
        }
    }

    /**
     * 默认以MsgItemBean的reqId作为sender的唯一标示
     *
     * @return
     */
    @Override
    public UUID uuid() {
        return UUID.fromString(mMsgItemBean.getReqId());
    }

    /**
     * 更新执行中的sender
     *
     * @param sender 新sender
     */
    @Override
    public void updateSender(ISender sender) {
        //更新new Sender中MsgItemBean
        ((SenderBase) sender).mMsgItemBean.setLocalViewUrl(mMsgItemBean.getLocalViewUrl());
        ((SenderBase) sender).mMsgItemBean.setWidth(mMsgItemBean.getWidth());
        ((SenderBase) sender).mMsgItemBean.setHeight(mMsgItemBean.getHeight());
        ((SenderBase) sender).mMsgItemBean.setViewUrl(mMsgItemBean.getViewUrl());
        ((SenderBase) sender).mMsgItemBean.setProgress(mMsgItemBean.getProgress());
        //替换old Sender中MsgItemBean为new MsgItemBean
        mMsgItemBean = ((SenderBase) sender).mMsgItemBean;
        //替换old Sender中的回调接口为new Listeners
        mSenderListeners = ((SenderBase) (((SenderBase) sender).mMsgItemBean.getISender())).mSenderListeners;
        //替换new Sender中MsgItemBean的Sender为old Sender
        ((SenderBase) sender).mMsgItemBean.setISender(this);
    }
}
