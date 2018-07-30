package com.yanxiu.im.sender;


import java.util.UUID;

/**
 * 该接口主要是用于im模块信息发送控制
 * Created by 杨小明 on 2018/5/7.
 */

public interface ISender {

    /**
     * 开始发送
     */
    void startSend();

    /**
     * 停止发送
     */
    void stopSend();

    /**
     * 添加发送监听接口
     *
     * @param senderListener 监听接口
     */
    void addSenderListener(ISenderListener senderListener);

    /**
     * 移除发送监听接口
     *
     * @param senderListener 监听接口
     */
    void removeSenderListener(ISenderListener senderListener);

    /**
     * ISender唯一标示，用于去重
     */
    UUID uuid();

    /**
     * sender任务重复时，更新sender
     *
     * @param sender 新sender
     */
    void updateSender(ISender sender);

}
