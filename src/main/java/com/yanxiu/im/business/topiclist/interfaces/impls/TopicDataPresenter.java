package com.yanxiu.im.business.topiclist.interfaces.impls;

import android.os.Handler;
import android.util.Log;

import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.topiclist.interfaces.TopicDataReponsitory;
import com.yanxiu.im.business.topiclist.interfaces.TopicListContract;

import java.util.ArrayList;
import java.util.List;

/**
 * create by 朱晓龙 2018/8/16 下午1:34
 */
public class TopicDataPresenter implements TopicListContract.Presenter {
    private Handler uihandler = new Handler();
    private final String TAG=getClass().getSimpleName();
    private TopicListContract.View<TopicItemBean> mView;

    public TopicDataPresenter(TopicListContract.View<TopicItemBean> view) {

        mView = view;
    }

    @Override
    public void doGetDbTopicList(long imId) {
        TopicDataReponsitory.reponsitoryInit(Constants.imId + "");
        //页面加载的第一步 显示数据库缓存数据
        TopicDataReponsitory.getInstance().getTopicList(Constants.imToken, new TopicDataReponsitory.GetTopicListCallback() {
            @Override
            public void onGetTopicList(final ArrayList<TopicItemBean> topicItemBeans) {
                if (mView == null) {
                    return;
                }
                uihandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mView.onGetDbTopicList(topicItemBeans);
                    }
                });
            }
        });
    }

    @Override
    public void doTopicListUpdate(List<TopicItemBean> topics) {
        //更新 用户的 topic 列表
        TopicDataReponsitory.getInstance().updateTopicsInfo(Constants.imToken, new TopicDataReponsitory.ActionCallback() {
            @Override
            public void onListUpdated(final ArrayList<TopicItemBean> datas) {
                /*topic 列表更新 完成*/
                uihandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "onListUpdated: ");
                        mView.onTopicListUpdate(datas);
                    }
                });
            }

            @Override
            public void onTopicMemberUpdated(final long topicId) {
                /*成员更新完成*/
                uihandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "onTopicMemberUpdated: ");
//                        mView.onTopicUpdate(topicId);
                    }
                });
            }

            @Override
            public void onTopicMsgUpdated(final long topicId) {
                /*最新消息更新完成*/
                uihandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "onTopicMsgUpdated: ");
                        mView.onTopicUpdate(topicId);
                    }
                });
            }
        });
    }

    @Override
    public void doCheckRedDot(List<TopicItemBean> topics) {

    }

    @Override
    public void doReceiveNewMsg(MsgItemBean msg) {
        TopicDataReponsitory.getInstance().addMsgs();
    }

    @Override
    public void doRemoveFromTopic(long topicId) {
        final boolean deleteMemory = Constants.APP_TYPE == Constants.APP_TYPE_STUDENT;
        TopicDataReponsitory.getInstance().deleteTopics(deleteMemory);
    }

    /**
     * 当前用户被加入到某个新的 topic 中
     */
    @Override
    public void doAddedToTopic(long topicId, boolean mqtt) {
        //两步操作 1、获取完成的 topic 信息 包括 member 列表 msg 列表
        //2、保存到本地以及内存中 返回通知 UI 刷新界面
    }

    @Override
    public void doMemberChangeOfTopic() {

    }
}
