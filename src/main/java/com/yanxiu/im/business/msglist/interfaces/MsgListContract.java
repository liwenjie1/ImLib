package com.yanxiu.im.business.msglist.interfaces;

import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;

/**
 * Created by 朱晓龙 on 2018/5/8 15:15.
 */

public interface MsgListContract {
    interface IView<E extends MsgItemBean> {
        /**
         * 队首插入新消息时的回调
         * */
        void onNewMsg();
        /**
         * 加载了更多一页时的回调
         * @param start 首个变动的位置
         * @param end 最后一个变动的位置
         * */
        void onLoadMoreWithMerge(int start,int end);

        void onTopicInfoUpdate();

        void onLoadMoreFromDb(int size);
        void onLoadMoreFromHttp(int size);

        /**
         * 重发一条消息
         * */
        void onResendMsg(int oldPosition);

        /**
         * 开启新 私聊topic
         * */
        void onTopicOpened(TopicItemBean privateTopic);

        /**
         * 临时topic
         * */
        void onTempUiTopicOpen(String topicName);

        /**
         * 创建新topic失败
         * */
        void onCreateTopicFail();

    }


    /**
     * msglist 列表页面的交互操作
     */
    interface IPresenter<E extends MsgItemBean> {
        /**
         * 发送一条新消息
         * @param msgStr 准备发送的文字消息内容
         * */
        void doSendTextMsg(String msgStr,TopicItemBean currentTopic);
        /**
         * 发送一条新消息
         * @param url 准备发送的图片消息路径
         * */
        void doSendImgMsg(String url,TopicItemBean currentTopic);
        /**
         * 重发一条消息
         * @param oldPosition 重发 msg 所在的位置
         * */
        void doResendMsg(int oldPosition, TopicItemBean currentTopic);
        /**
         * 获取一页历史消息
         * @param currentTopic 当前 topic对象
         * */
        void doLoadMore(TopicItemBean currentTopic);
        /**
         * 开启新 topic
         * @param memberId 私聊对象 的 memberId
         * */
        void openPrivateTopicByMember(long memberId,long fromTopicId);
        /**
         * 根据topicid 打开topic
         * */
        void openTopicByTopicId(long topicId);

        /**
         * 创建mocktopic
         * */
        TopicItemBean createMockTopicForMsg(long memberId,long fromTopic);
    }

}
