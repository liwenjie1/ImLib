package com.yanxiu.im.business.topiclist.interfaces.impls;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.yanxiu.im.Constants;
import com.yanxiu.im.TopicsReponsery;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.topiclist.interfaces.TopicListContract;
import com.yanxiu.im.business.topiclist.sorter.ImTopicSorter;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.manager.RequestQueueManager;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by 朱晓龙 on 2018/5/8 9:31.
 * 主要功能是向底层获取 topic数据
 * 当获取数据后 进行初步处理，
 * 1、排序
 * 2、设置显示红点
 * 3、判断是否显示日期
 * <p>
 * 主要为收消息的相关处理
 */

public class TopicListPresenter implements TopicListContract.Presenter {
    private final String TAG = getClass().getSimpleName();


    private TopicListContract.View view;
    private Context mContext;

    private RequestQueueManager rqManager = new RequestQueueManager();
    private int totalRetryTimes = 10;

    public TopicListPresenter(TopicListContract.View view, Context mContext) {
        this.view = view;
        this.mContext = mContext;
    }

    private Handler mHandler = new Handler();

    public List<TopicItemBean> getTopicInMemory() {
        return TopicsReponsery.getInstance().getTopicInMemory();
    }

    /**
     * 获取 数据库 保存的topic列表
     * 初步生成UI
     */
    @Override
    public void doGetDbTopicList(long imId) {
        final ArrayList<TopicItemBean> dbTopics = TopicsReponsery.getInstance().getLocalTopicList(imId);
        //检查是否有 异常退出造成的sending 状态数据
        for (TopicItemBean dbTopic : dbTopics) {
            for (MsgItemBean msgItemBean : dbTopic.getMsgList()) {
                if (msgItemBean.getState() == 1) {
                    msgItemBean.setState(2);
                    DatabaseManager.updateDbMsgWithMsgItemBean(msgItemBean);
                }
            }
        } //第一次的排序 按照 最后操作时间排序
        ImTopicSorter.sortByLatestTime(dbTopics);
        //初始化日期选项
        for (TopicItemBean dbTopic : dbTopics) {
            if (dbTopic.getMsgList() != null) {
                TopicInMemoryUtils.processMsgListDateInfo(dbTopic.getMsgList());
            }
        }
        if (view != null) {
            final List<TopicItemBean> finalDbTopics = dbTopics;
            view.onGetDbTopicList(finalDbTopics);
        }
    }

    /**
     * 异步方法 结果在网络请求回调中 返回UI
     * 更新用户的topic list
     */
    @Override
    public void doTopicListUpdate(final List<TopicItemBean> topicsFromDb) {
        TopicsReponsery.getInstance().getServerTopicList(Constants.imToken, new TopicsReponsery.TopicListUpdateCallback<TopicItemBean>() {
            @Override
            public void onListUpdated(ArrayList<TopicItemBean> dataList) {
                ImTopicSorter.sortByLatestTime(dataList);
                doCheckRedDot(dataList);
                view.onTopicListUpdate();
            }
        });
    }

    public void doTopicListUpdate() {
        TopicsReponsery.getInstance().getServerTopicList(Constants.imToken, new TopicsReponsery.TopicListUpdateCallback<TopicItemBean>() {
            @Override
            public void onListUpdated(ArrayList<TopicItemBean> dataList) {
                ImTopicSorter.sortByLatestTime(dataList);
                doCheckRedDot(dataList);
                view.onTopicListUpdate();
            }
        });
    }

    /**
     * 更新每一个 topic 的信息
     * 1、member 2、msglist
     */
    public void doUpdateAllTopicInfo() {
        TopicsReponsery.getInstance().updateAllTopicInfo(new TopicsReponsery.GetTopicItemBeanCallback() {
            @Override
            public void onGetTopicItemBean(TopicItemBean bean) {
                //会多次回调 知道完全结束
                if (bean != null) {
                    view.onTopicUpdate(bean.getTopicId());
                }
            }
        });
    }

    public void doUpdateTopicInfo(final long topicId) {
        TopicsReponsery.getInstance().getLocalTopic(topicId, new TopicsReponsery.GetTopicItemBeanCallback() {
            @Override
            public void onGetTopicItemBean(TopicItemBean bean) {
                //
                if (bean != null) {
                    TopicsReponsery.getInstance().updateTopicInfo(bean, new TopicsReponsery.GetTopicItemBeanCallback() {
                        @Override
                        public void onGetTopicItemBean(TopicItemBean bean) {
                            view.onTopicInfoUpdate(bean.getTopicId());
                        }
                    });
                }else {
                    Log.i(TAG, "onGetTopicItemBean: topic 事件  新加入 topic");
                    //如果 本地没有这个 topic 而又收到了 update 通知 说明是 自己被添加到这个 topic 中
                    doAddedToTopic(topicId,true);
                }
            }
        });
    }


    @Override
    public void doCheckRedDot(List<TopicItemBean> topics) {
        //处理红点
        boolean hasReddot = false;
        if (topics == null) {
            return;
        }
        for (TopicItemBean topic : topics) {
            if (topic.isShowDot()) {
                hasReddot = true;
                break;
            }
        }
        final boolean hasReddot2 = hasReddot;
        if (view != null) {
            view.onRedDotState(hasReddot2);
        }
    }

    @Override
    public void doReceiveNewMsg(MsgItemBean msg) {
        //底层处理后 发送接收到的消息  首先找到目标 topic
        List<TopicItemBean> topics = TopicsReponsery.getInstance().getTopicInMemory();
        if (topics == null) {
            return;
        }
        //首先找到 msg 对应的 topic topic list 中可能没有 对应的 topic 因为是被清空历史记录的 topic
        TopicItemBean targetTopic = TopicInMemoryUtils.findTopicByTopicId(msg.getTopicId(), topics);
        //在数据库中查找 如果执行了数据查找 可以认为 这是一个被清空历史的topic
        if (targetTopic == null) {
            targetTopic = TopicsReponsery.getInstance().getTopicFromDb(msg.getTopicId());
            if (targetTopic == null) {
                return;
            }
            //重新加入列表
            topics.add(targetTopic);
            if (targetTopic.isAlreadyDeletedLocalTopic()) {
                // todo 是一个清空历史消息的 topic
            }else {
                //不是呢？
            }
        }

        if (targetTopic.getMsgList() == null) {
            targetTopic.setMsgList(new ArrayList<MsgItemBean>());
        }
        //msg 去重
        for (MsgItemBean msgBean : targetTopic.getMsgList()) {
            if (TextUtils.equals(msgBean.getReqId(), msg.getReqId())) {
                return;
            }
        }
        //将新收到的 msg 加入的 msg 列表中
        targetTopic.getMsgList().add(0, msg);
        //重置 信息清空标志位 只清空标志位
        targetTopic.setAlreadyDeletedLocalTopic(false);
        //自己的消息 不显示红点
        targetTopic.setShowDot(msg.getSenderId() != Constants.imId);
        targetTopic.setLatestMsgTime(msg.getSendTime());
        targetTopic.setLatestMsgId(msg.getMsgId());
        DatabaseManager.updateTopicWithTopicItemBean(targetTopic);
        ImTopicSorter.insertTopicToTop(targetTopic.getTopicId(), topics);
        //收到新消息后 进行日期处理
        processMsgListDateInfo(targetTopic.getMsgList());
        //通知Ui更新
        final long topicId=targetTopic.getTopicId();
        if (view != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    view.onNewMsgReceived(topicId);
                }
            });

        }

    }

    /**
     * 执行 移除操作
     * 操作 由 mqtt 推送的人员变动消息触发
     * 在执行topicmember 更新后 检查 member 状态 进行调用
     */
    @Override
    public void doRemoveFromTopic(long topicId) {
        //在本地数据中移除 topic
        final TopicItemBean removedTopic = TopicsReponsery.getInstance().removeTopic(topicId);
        if (view != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    view.onRemovedFromTopic(removedTopic.getTopicId(), removedTopic.getGroup());
                }
            });
        }
    }

    @Override
    public void doAddedToTopic(long topicId, boolean mqtt) {
        List<TopicItemBean> topics = TopicsReponsery.getInstance().getTopicInMemory();
        if (topics == null) {
            return;
        }
        //在网络请求结果中回调给UI
        TopicsReponsery.getInstance().addToTopic(topicId, new TopicsReponsery.AddToTopicCallback<TopicItemBean>() {
            @Override
            public void onAdded(TopicItemBean topicBean) {
                if (view != null) {
                    //这里进行了订阅操作
                    view.onAddedToTopic(topicBean.getTopicId());
                    //通知列表有更新
                    view.onTopicListUpdate();
                }
            }

            @Override
            public void onFailure(String msg) {
                //add to 失败
                Log.i(TAG, "onFailure: " + msg);
            }
        });
    }


    @Override
    public void doMemberChangeOfTopic() {
        // 暂时什么都不做
    }

    //每次获取最新一页消息后执行
    private void processMsgListDateInfo(List<MsgItemBean> msgList) {
        if (msgList != null && msgList.size() > 0) {
            MsgItemBean preItem;
            MsgItemBean curItem;
            int length = msgList.size();
            for (int i = 0; i < length; i++) {
                curItem = msgList.get(i);
                if (length - 1 > i) {
                    preItem = msgList.get(i + 1);
                    boolean showDate = (curItem.getSendTime() - preItem.getSendTime() > 5 * 60 * 1000);
                    curItem.setShowDate(showDate);
                } else {
                    curItem.setShowDate(true);
                }
            }
        }
    }

    /**
     * RequestQueueManager的回调，用来在http拉取数据结束后，合并mockTopic
     * 用来保证realtopic获取member和msg结束后，再merge。否则可能在合并mocktopic时，realtopic还没有执行完更新member或者msg
     */
    private class RequestQueueCallBack implements RequestQueueManager.RequestQueueCallBack {

        /**
         * 队列里请求全部结束
         */
        @Override
        public void requestAllFinish() {
            DatabaseManager.checkAndMigrateMockTopic(TopicsReponsery.getInstance().getTopicInMemory());
        }

    }


    /**
     * 检测用户是否被移除
     * 通过请求该topic的members，来判断当前用户是否被删除。
     * 1.如果删除，则相当于topic删除。
     * 2.如果是别的用户被删除，在topic里的memberlist移除。
     * <p>
     * <p>
     * 3 在区分成员角色的模式下  管理员不能被删除
     *
     *
     *
     */
    /**
     * create by 朱晓龙 2018/8/2 上午11:19
     * 用户在某个 topic 被删除的时候 列表并不进行更新
     * <p>
     * 只对被删除的 topic 进行 member 删除  topicmemberlist 中删除当前用户的 member 用于后续操作的判断依据
     * <p>
     * 学员端与 管理端的区别是在收到删除通知后 是否跳转班级选择界面
     * <p>
     * 而在 收到加入 topic 通知时 需要及时刷新列表
     *
     * @param topicId 收到 mqtt 删除推送的 topicID
     */
    public void checkUserRemove(final long topicId) {
        //首先获取 本地 topic 目标
        TopicsReponsery.getInstance().getLocalTopic(topicId, new TopicsReponsery.GetTopicItemBeanCallback() {
            @Override
            public void onGetTopicItemBean(TopicItemBean bean) {
                //找到 目标 topicbean
                TopicsReponsery.getInstance().updateTopicMemberInfoFromServer(bean, new TopicsReponsery.GetTopicItemBeanCallback() {
                    @Override
                    public void onGetTopicItemBean(TopicItemBean bean) {
                        if (bean != null) {
                            //遍历查找 是否自己还在 member 列表中
                            final boolean remain = TopicInMemoryUtils.checkMemberInTopic(Constants.imId, bean);
                            if (!remain) {
                                //自己不再列表中了
                                //删除
                                TopicsReponsery.getInstance().removeTopic(topicId);
                                //回调
                                view.onRemovedFromTopic(topicId, bean.getName());
                            } else {
                                view.onOtherMemberRemoveFromTopic(topicId);
                            }
                        }
                    }
                });
            }
        });


//        com.yanxiu.im.net.TopicGetTopicsRequest_new getTopicsRequest = new com.yanxiu.im.net.TopicGetTopicsRequest_new();
//        getTopicsRequest.imToken = Constants.imToken;
//        getTopicsRequest.topicIds = topicId + "";
//        rqManager.addRequest(getTopicsRequest, com.yanxiu.im.net.TopicGetTopicsResponse_new.class, new IYXHttpCallback<com.yanxiu.im.net.TopicGetTopicsResponse_new>() {
//            /**
//             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
//             *
//             * @param request OkHttp Request
//             */
//            @Override
//            public void onRequestCreated(Request request) {
//            }
//
//            @Override
//            public void onSuccess(YXRequestBase request, com.yanxiu.im.net.TopicGetTopicsResponse_new ret) {
//                if (topics == null) {
//                    return;
//                }
//                // 更新数据库
//                List<TopicItemBean> topicsNeedUpdateMember = new ArrayList<>();
//                com.yanxiu.im.bean.net_bean.ImTopic_new imTopic = null;
//                List<com.yanxiu.im.bean.net_bean.ImTopic_new.Member> imMemberList = null;
//                if (ret.data.topic == null || ret.data.topic.isEmpty()) {
//                    //TODO:疑问：如果topic为空，是否意味着该topic已被删除？暂时不处理
//                    return;
//                }
//                //获取当前本地持有的 topic 对象
//                final TopicItemBean targetLocalTopic = TopicInMemoryUtils.findTopicByTopicId(topicId, topics);
//                //获取 推送的 目标 topic
//                imTopic = ret.data.topic.get(0);
//                //获取 目标 topic 的最新 member 列表
//                imMemberList = imTopic.members;
//                /* 对比 当前用户持有的 topic member 列表与 服务器返回的最新 member 列表 删除被移除的 member*/
//                synchronized (targetLocalTopic.getMembers()) {//可能同时两条 或多条 同一个 topic 的 member 推送 造成多线程操作 memberlist 所以加锁
//                    ArrayList<DbMember> dbMembershasBeenDel = new ArrayList<>();
//                    if (targetLocalTopic.getMembers() == null) {
//                        // 不知道怎么办
//                        return;
//                    }
//                    if (imMemberList == null) {
//                        //为了 member 统一删除方法
//                        imMemberList = new ArrayList<>();
//                    }
//
//                    for (DbMember dbMember : targetLocalTopic.getMembers()) {
//                        boolean remain = false;
//                        for (ImTopic_new.Member remainMember : imMemberList) {
//                            if (dbMember.getImId() == remainMember.memberId) {
//                                remain = true;
//                                break;
//                            }
//                        }
//                        if (!remain) {//如果用户已经不再列表中
//                            dbMembershasBeenDel.add(dbMember);
//                        }
//                    }
//                    //删除那些不在列表中的 member
//                    targetLocalTopic.getMembers().removeAll(dbMembershasBeenDel);
//                    //判断被移除的 member 是不是自己
//                    for (DbMember removedMember : dbMembershasBeenDel) {
//                        if (removedMember.getImId() == Constants.imId) {
//                            /*如果是学员端 这里需要将 topic 列表进行删除 */
//                            if (Constants.APP_TYPE == Constants.APP_TYPE_STUDENT) {
//                                TopicInMemoryUtils.removeTopicFromListById(topicId, topics);
//                            }
//                            mHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (view != null) {
//                                        view.onRemovedFromTopic(targetLocalTopic.getTopicId(), targetLocalTopic.getGroup());
//                                    }
//                                }
//                            });
//                        } else {//如果不是自己被移除  通知 其他人被移除
//                            mHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (view != null) {
//                                        view.onOtherMemberRemoveFromTopic(targetLocalTopic.getTopicId());
//                                    }
//                                }
//                            });
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onFail(YXRequestBase request, Error error) {
//
//            }
//        });
    }


}
