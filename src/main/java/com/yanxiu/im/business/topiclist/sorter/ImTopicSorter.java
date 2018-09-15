package com.yanxiu.im.business.topiclist.sorter;

import android.text.TextUtils;

import com.yanxiu.im.bean.TopicItemBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by 朱晓龙 on 2018/5/8 11:21.
 * 负责处理所有关于topiclist页面的排序工作
 * 需要线程同步
 */

public class ImTopicSorter {
    /**
     * 将原始topic 列表 按照 群聊私聊的不同 进行一次 排序
     * 群聊在前 私聊在后
     * */
    public static void groupTopicByType(List<TopicItemBean> topicList){
        if (topicList == null) {
            return;
        }
        synchronized (topicList) {
            ArrayList<TopicItemBean> groupTopic = new ArrayList<>();
            ArrayList<TopicItemBean> privateTopic = new ArrayList<>();
            for (TopicItemBean topicDataBean : topicList) {
                if (TextUtils.equals(topicDataBean.getType(), "2")) {
                    groupTopic.add(topicDataBean);
                } else {
                    privateTopic.add(topicDataBean);
                }
            }
            topicList.clear();
            topicList.addAll(groupTopic);
            topicList.addAll(privateTopic);
        }
    }

    /**
     * 收到新消息后的排序操作
     * 只对收到消息的topic 进行置顶操作
     * */
    public static void insertTopicToTop(long topicId, List<TopicItemBean> topicList){
        sortByLatestTime(topicList);
//        if (topicList == null) {
//            return;
//        }
//        synchronized (topicList) {
//            if (topicList == null) {
//                return;
//            }
//            Iterator<TopicItemBean> iterator = topicList.iterator();
//            //首先找到目标topic
//            TopicItemBean targetTopic = null;
//            while (iterator.hasNext()) {
//                targetTopic = iterator.next();
//                if (topicId == targetTopic.getTopicId()) {
//                    //然后进行原有位置的移除操作
//                    iterator.remove();
//
//                    if (targetTopic != null) {
//                        topicList.add(0, targetTopic);
//                        groupTopicByType(topicList);
//                    }
//                    break;
//                }
//            }
//        }
    }
    /**
     * 应用启动后 获取全部topic的信息进行一次排序
     * 这次排序 根据 lstestMsgId 以及 latestMsgSendTime来进行
     * 排序比较标准为 以消息最新者置顶
     * */
    public static void sortByLatestTime(List<TopicItemBean> topicList){
        if (topicList == null) {
            return;
        }
        synchronized (topicList) {
            //
            Comparator<TopicItemBean> comparator = new Comparator<TopicItemBean>() {
                @Override
                public int compare(TopicItemBean t1, TopicItemBean t2) {
                    if (t1.getLatestMsgTime()>t2.getLatestMsgTime()) {
                        return -1;
                    }else if (t1.getLatestMsgTime()<t2.getLatestMsgTime()){
                        return 1;
                    }else {
                        return 0;
                    }
                }
            };
            Collections.sort(topicList,comparator);
            groupTopicByType(topicList);
        }
    }

}
