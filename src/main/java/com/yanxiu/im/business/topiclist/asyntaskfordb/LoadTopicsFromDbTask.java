package com.yanxiu.im.business.topiclist.asyntaskfordb;

import android.os.AsyncTask;

import com.test.yanxiu.common_base.utils.SharedSingleton;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.topiclist.interfaces.TopicListContract;
import com.yanxiu.im.business.topiclist.interfaces.impls.TopicListPresenter;
import com.yanxiu.im.business.topiclist.sorter.ImTopicSorter;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.manager.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 朱晓龙 on 2018/6/6 15:20.
 */

public class LoadTopicsFromDbTask extends AsyncTask<Long,Integer,List<TopicItemBean>> {

    private TopicListContract.View<TopicItemBean> mView;
    private TopicListPresenter mPresenter;

    public LoadTopicsFromDbTask(TopicListContract.View<TopicItemBean> view) {
        mView = view;
    }

    @Override
    protected List<TopicItemBean> doInBackground(Long... imId) {
        DatabaseManager.useDbForUser(Long.toString(imId[0]) + "_db");//todo:应该放在config里面去
        //这个耗时操作 在db数据较多时
        List<TopicItemBean> dbTopics = DatabaseManager.topicsFromDb();
        if (dbTopics == null) {
            dbTopics = new ArrayList<>();
        }

        //检查是否有 异常退出造成的sending 状态数据
        for (TopicItemBean dbTopic : dbTopics) {
            for (MsgItemBean msgItemBean : dbTopic.getMsgList()) {
                if (msgItemBean.getState() == 1) {
                    msgItemBean.setState(2);
                    DatabaseManager.updateDbMsgWithMsgItemBean(msgItemBean);
                }
            }
        }
        return dbTopics;
    }



    @Override
    protected void onPostExecute(List<TopicItemBean> dbTopics) {
        super.onPostExecute(dbTopics);
        //第一次的排序 按照 最后操作时间排序
        ImTopicSorter.sortByLatestTime(dbTopics);
        //保存单例
        SharedSingleton.getInstance().set(SharedSingleton.KEY_TOPIC_LIST, dbTopics);
        //初始化日期选项
        for (TopicItemBean dbTopic : dbTopics) {
            if (dbTopic.getMsgList() != null) {
                TopicInMemoryUtils.processMsgListDateInfo(dbTopic.getMsgList());
            }
        }
        mView.onGetDbTopicList(dbTopics);
    }
}
