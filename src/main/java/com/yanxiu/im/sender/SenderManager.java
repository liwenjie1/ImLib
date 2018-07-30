package com.yanxiu.im.sender;


import java.util.ArrayList;
import java.util.LinkedList;

/**
 * sender管理器,支持同时一个或多个sender
 * Created by 杨小明 on 2018/5/7.
 */

public class SenderManager {
    //默认发送数量
    private static final int DEFALUT_CONCURRENCY_SENDER = 2;
    //最大并发数
    private int mMaxConcurrency;
    //等待任务列表
    private LinkedList<ISender> mWaitingSenderList = new LinkedList();
    //正在执行的任务
    private ArrayList<ISender> mOngoingSenderList = new ArrayList();

    //sender执行监听
    private ISenderListener mSenderListener = new ISenderListener() {
        @Override
        public void OnSuccess(ISender sender) {
            mOngoingSenderList.remove(sender);
            doNextSend();
        }

        @Override
        public void OnFail(ISender sender) {
            mOngoingSenderList.remove(sender);
            doNextSend();
        }

        @Override
        public void OnProgress(double progress) {

        }
    };

    public SenderManager() {
        mMaxConcurrency = DEFALUT_CONCURRENCY_SENDER;
    }

    public SenderManager(int maxConcurrency) {
        if (maxConcurrency <= 0) {
            mMaxConcurrency = DEFALUT_CONCURRENCY_SENDER;
        } else {
            mMaxConcurrency = maxConcurrency;
        }
    }

    /**
     * 添加任务
     *
     * @param sender 待添加的任务
     */
    public void addSender(ISender sender) {
        sender.addSenderListener(mSenderListener);
        //sender去重，等待中则替换任务
        for (int i = 0; i < mWaitingSenderList.size(); i++) {
            if (mWaitingSenderList.get(i).uuid().equals(sender.uuid())) {
                mWaitingSenderList.set(i, sender);
                return;
            }
        }
        //sender去重, 执行中，则更新任务
        for (ISender temp : mOngoingSenderList) {
            if (temp.uuid().equals(sender.uuid())) {
                temp.updateSender(sender);
                return;
            }
        }
        mWaitingSenderList.add(sender);
        doNextSend();
    }

    /**
     * 是否可以执行下一个任务
     *
     * @return
     */
    private boolean shouldSendNext() {
        if (mWaitingSenderList.size() > 0 && mOngoingSenderList.size() < mMaxConcurrency) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 下一次发送
     */
    private void doNextSend() {
        if (!shouldSendNext()) {
            return;
        }
        ISender sender = mWaitingSenderList.removeFirst();
        sender.startSend();
        mOngoingSenderList.add(sender);
    }


    /**
     * 从队列里面删除任务，暂时未使用
     *
     * @param sender 待删除的任务
     * @return boolean 删除成功与否
     */
    public boolean removeSender(ISender sender) {
        return false;
    }

}
