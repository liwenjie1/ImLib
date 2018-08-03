package com.yanxiu.im.business.topiclist.interfaces;

import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;

import java.util.List;

/**
 * Created by 朱晓龙 on 2018/5/8 9:18.
 */

public interface TopicListContract {
    interface View<E>{
        void onGetDbTopicList(List<E> dbTopicList);
        void onTopicListUpdate();
        void onTopicUpdate(long topicId);
        void onNewMsgReceived(long topicId);


        void onRemovedFromTopic(long topicId,String topicName);
        void onOtherMemberRemoveFromTopic(long topicId);
        void onAddedToTopic(long topicId);
        void onRedDotState(boolean showing);

    }
    interface Presenter{
        /**
         * 获取用户的数据库topiclist
         * */
        void doGetDbTopicList(long imId);
        /**
         * 获取用户内存中的topiclist（最新http拉取的数据）
         * */
        void doTopicListUpdate(List<TopicItemBean> topics);

        /**
         * 处理topic列表红点状态
         * 触发条件：1、 receiveNewMsg 2、TopicChanged 3、getTopicList(topic.change 有变化 也就是成员变化)
         * */
        void doCheckRedDot(List<TopicItemBean> topics);

        /**
         * 处理新消息 获取
         * */
        void doReceiveNewMsg(MsgItemBean msg);
        /**
         * 处理 topic change
         * */
        void doRemoveFromTopic(long topicId);
        void doAddedToTopic(long topicId,boolean mqtt);
        void doMemberChangeOfTopic();
    }
}
