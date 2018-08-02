package com.yanxiu.im.business.topiclist.interfaces.impls;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.test.yanxiu.common_base.utils.SharedSingleton;
import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.im.business.topiclist.interfaces.TopicListContract;
import com.yanxiu.im.business.topiclist.sorter.ImTopicSorter;
import com.yanxiu.im.business.utils.ImServerDataChecker;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.manager.RequestQueueManager;
import com.yanxiu.im.net.GetTopicMsgsRequest_new;
import com.yanxiu.im.net.GetTopicMsgsResponse_new;
import com.yanxiu.im.net.TopicGetMemberTopicsResponse_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;


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

    private Handler mHandler = new Handler();

    private TopicListContract.View view;
    private Context mContext;

    private RequestQueueManager rqManager = new RequestQueueManager();
    private int totalRetryTimes = 10;

    public TopicListPresenter(TopicListContract.View view, Context mContext) {
        this.view = view;
        this.mContext = mContext;
    }

    /**
     * 获取 数据库 保存的topic列表
     * 初步生成UI
     */
    @Override
    public void doGetDbTopicList(long imId) {
        //异步获取 数据库数据 topic列表
        DatabaseManager.useDbForUser(Long.toString(Constants.imId) + "_db");//todo:应该放在config里面去
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
        } //第一次的排序 按照 最后操作时间排序
        ImTopicSorter.sortByLatestTime(dbTopics);
        //保存单例
        SharedSingleton.getInstance().set(SharedSingleton.KEY_TOPIC_LIST, dbTopics);
        //初始化日期选项
        for (TopicItemBean dbTopic : dbTopics) {
            if (dbTopic.getMsgList() != null) {
                TopicInMemoryUtils.processMsgListDateInfo(dbTopic.getMsgList());
            }
        }

        if (view != null) {
            final List<TopicItemBean> finalDbTopics = dbTopics;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    view.onGetDbTopicList(finalDbTopics);
                }
            });

        }
    }

    /**
     * 异步方法 结果在网络请求回调中 返回UI
     * 更新用户的topic list
     * 判断topic.change 来分为两个数组
     * 一组 change 有变化的 需要更新member列表
     * 另一组没有 变化的 直接更新msglist
     */
    @Override
    public void doTopicListUpdate(final List<TopicItemBean> topicsFromDb) {
        com.yanxiu.im.net.TopicGetMemberTopicsRequest_new getMemberTopicsRequest = new com.yanxiu.im.net.TopicGetMemberTopicsRequest_new();
        getMemberTopicsRequest.imToken = Constants.imToken;
        getMemberTopicsRequest.startRequest(TopicGetMemberTopicsResponse_new.class, new IYXHttpCallback<TopicGetMemberTopicsResponse_new>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, TopicGetMemberTopicsResponse_new ret) {
                List<TopicItemBean> maybeNeedUpdateMsgTopicList = new ArrayList<>();
                // 3
                //获取用户服务器上所有的topic
                /*由于删除需求的限制 管理端用户被某个 topic 删除后不能立即 将数据库删除  （需求 被删除依然可以看见历史消息 ）所以这里每次在获取用户最新的 topic 列表时进行一次统一的删除操作
                * 删除那些 本地有 服务器上没有的 topic（群聊）
                * */
                ArrayList<TopicItemBean> deteletedTopic=new ArrayList<>();
                if (ret.data.topic == null) {
                    ret.data.topic=new ArrayList<>();
                }

                for (TopicItemBean dbtopic : topicsFromDb) {
                    if (TextUtils.equals(dbtopic.getType(),"2")) {
                        boolean remain=false;
                        for (ImTopic_new remainTopic : ret.data.topic) {
                            if (dbtopic.getTopicId()==remainTopic.topicId) {
                                remain=true;
                                break;
                            }
                        }
                        //如果已经不在了 。。。 加入到待删除列表中
                        if (!remain) {
                            deteletedTopic.add(dbtopic);
                        }
                    }
                }
                //删除 topic 数据库
                for (TopicItemBean deleteTopic : deteletedTopic) {
                    DatabaseManager.deleteTopicById(deleteTopic.getTopicId());
                }
                //内存中删除 已经失效的 topic
                topicsFromDb.removeAll(deteletedTopic);

                //检查是否需要更新member 不需要更新member的topic 加入到mayBeNeedUpdateMsgTopicList
                checkTopicNeedUpdateMembers(ret, topicsFromDb, maybeNeedUpdateMsgTopicList);
                //已经分完组
                //除了新topic和有topicchagne的topic，也需要请求msg
                if (maybeNeedUpdateMsgTopicList != null && !maybeNeedUpdateMsgTopicList.isEmpty()) {
                    updateEachTopicMsgs(maybeNeedUpdateMsgTopicList);
                } else {
                    if (view != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onTopicListUpdate();
                            }
                        });
                    }
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                // TODO: 2018/5/21  更新topic列表失败
            }
        });
    }

    /**
     * 3.1
     * 判断topic是否需要更新member
     * 从Http获取需要更新的topic的信息，完成后写入DB，更新UI
     * 需要更新的两种情况：
     * 1.新的topic
     * 2.topicChange有变化的topic
     */
    private void checkTopicNeedUpdateMembers(TopicGetMemberTopicsResponse_new ret, List<TopicItemBean> topicList, List<TopicItemBean> maybeNeedUpdateMsgTopicList) {

        List<String> idTopicsNeedUpdateMember = new ArrayList<>(); // 因为可能有新的，所以只能用topicId

        //用http返回的topic去遍历本地的topic，所有不在DB中的，以及所有在DB中但change不等于topicChange的topics，都需要更新
        for (com.yanxiu.im.bean.net_bean.ImTopic_new imTopic : ret.data.topic) {
            boolean needUpdateMembers = true;
            for (TopicItemBean dbTopic : topicList) {
                if (dbTopic.getTopicId() == imTopic.topicId) { //已经存在的topic
                    dbTopic.setLatestMsgId(imTopic.latestMsgId);
                    dbTopic.setLatestMsgTime(imTopic.latestMsgTime);
                    if (dbTopic.getChange().equals(imTopic.topicChange)) { //已经存在的topic，且topicchange无变化的，不需更新
                        needUpdateMembers = false;
                        maybeNeedUpdateMsgTopicList.add(dbTopic);
                    }
                    break;
                }
            }
            if (needUpdateMembers) { //需要更新的topic
                idTopicsNeedUpdateMember.add(Long.toString(imTopic.topicId));
            }
        }
        //更新memeber 信息
        if (idTopicsNeedUpdateMember.size() > 0) {
            for (String topicId : idTopicsNeedUpdateMember) {
                // 由于server限制，改成一个个取
                updateTopicsWithMembers(topicList, topicId);
            }
        }
    }

    private void updateTopicsWithMembers(final List<TopicItemBean> topicList, final String topicIds) {
        updateTopicsWithMembers(topicList, topicIds, false);
    }

    /**
     * 3.2
     * 更新members
     * http, mqtt 公用
     * 最后一步影响排序的 http 请求
     *
     * @param fromMqtt 区分 是由 http 拉取的新 topiclist 中执行的更新还是 有 mqtt addto 进行的数据请求
     */
    private void updateTopicsWithMembers(final List<TopicItemBean> topicList, final String topicIds, final boolean fromMqtt) {
        com.yanxiu.im.net.TopicGetTopicsRequest_new getTopicsRequest = new com.yanxiu.im.net.TopicGetTopicsRequest_new();
        getTopicsRequest.imToken = Constants.imToken;
        getTopicsRequest.topicIds = topicIds;
        rqManager.addRequest(getTopicsRequest, com.yanxiu.im.net.TopicGetTopicsResponse_new.class, new IYXHttpCallback<com.yanxiu.im.net.TopicGetTopicsResponse_new>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, com.yanxiu.im.net.TopicGetTopicsResponse_new ret) {
                // 更新数据库
                List<TopicItemBean> topicsToUpdateMsgs = new ArrayList<>();

                for (com.yanxiu.im.bean.net_bean.ImTopic_new imTopic : ret.data.topic) {
                    TopicItemBean tempTopic = null;//需要更新的topic，为保证对象一致，该topic必须是topicList里的对象。
                    TopicItemBean dbTopic = DatabaseManager.updateDbTopicWithImTopic(imTopic);//更新数据库
                    //加入到更新msg的topicItembean列表
                    topicsToUpdateMsgs.add(dbTopic);

                    if (imTopic.latestMsgId >= dbTopic.getLatestMsgId()) {
                        dbTopic.setLatestMsgTime(imTopic.latestMsgTime);
                        dbTopic.setLatestMsgId(imTopic.latestMsgId);
                    }
                    synchronized (topicList) {
                        boolean hasThisTopic = false;
                        // 更新UI使用的topicList内的topic member列表
                        for (TopicItemBean uiTopic : topicList) {
                            if (uiTopic.getTopicId() == imTopic.topicId) {
                                hasThisTopic = true;
                                uiTopic.setTopicId(imTopic.topicId);
                                uiTopic.setName(imTopic.topicName);
                                uiTopic.setType(imTopic.topicType);
                                uiTopic.setChange(imTopic.topicChange);
                                uiTopic.setGroup(imTopic.topicGroup);
                                //latestMsgID latset msgTime
                                uiTopic.setMembers(dbTopic.getMembers());//TODO：member对象变了
                                tempTopic = uiTopic;
                            }
                        }

                        if (!hasThisTopic) {
                            //列表中目前不包含 相同topicid 的topic
                            tempTopic = dbTopic;
                            tempTopic.setShowDot(true);
                            //如果没有消息内容  排序需要 latest 时间
                            if (tempTopic.getLatestMsgId() == 0) {
                                tempTopic.setLatestMsgTime(System.currentTimeMillis());
                            }

                            //首先判断 dbtopic 为私聊才判断
                            if (!TopicInMemoryUtils.isPrivateTopic(dbTopic)) {
                                topicList.add(dbTopic);
                            } else {
                                if (!TopicInMemoryUtils.hasTheSameMockPrivateTopic(dbTopic.getMembers(), topicList)) {
                                    //检查mocktopic 没有对应的mocktopic 直接加入到topiclist 中 进行列表更新
                                    topicList.add(dbTopic);
                                }
                            }
                            //新topic 通知订阅
                            if (view != null) {
                                //加入了 新topic
                                final TopicItemBean finalTempTopic = tempTopic;
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        view.onAddedToTopic(finalTempTopic.getTopicId());
                                    }
                                });

                            }
                        }
                        //topic list 中的 topic 有更新  增加 或 latestmsgid 更新
                    if (fromMqtt) {
                        //如果是 mqtt 是在线状态加入了新的 topic 执行插入排序
                        ImTopicSorter.insertTopicToTop(tempTopic.getTopicId(), topicList);
                    } else {
                        ImTopicSorter.sortByLatestTime(topicList);
                    }
//                        ImTopicSorter.sortByLatestTime(topicList);
                        if (view != null) {
                            //通知 UI list 更新 （排序）
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    view.onTopicListUpdate();
                                }
                            });
                        }
                    }
                }
                // 4，对于需要更新members的topic，等待更新完members，再去取msgs
                updateEachTopicMsgs(topicsToUpdateMsgs);
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                // TODO: 2018/5/21 获取member
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
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    view.onRedDotState(hasReddot2);
                }
            });
        }
    }

    @Override
    public void doReceiveNewMsg(MsgItemBean msg) {
        List<TopicItemBean> topics = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        final TopicItemBean targetTopic = TopicInMemoryUtils.findTopicByTopicId(msg.getTopicId(), topics);
        if (targetTopic.getMsgList() == null) {
            targetTopic.setMsgList(new ArrayList<MsgItemBean>());
        }
        //msg 去重
        for (MsgItemBean msgBean : targetTopic.getMsgList()) {
            if (TextUtils.equals(msgBean.getReqId(), msg.getReqId())) {
                return;
            }
        }
        targetTopic.getMsgList().add(0, msg);

        //自己的消息 不显示红点
        targetTopic.setShowDot(msg.getSenderId() != Constants.imId);
        targetTopic.setLatestMsgTime(msg.getSendTime());
        targetTopic.setLatestMsgId(msg.getMsgId());
        DatabaseManager.updateTopicWithTopicItemBean(targetTopic);
        ImTopicSorter.insertTopicToTop(targetTopic.getTopicId(), topics);
        //收到新消息后 进行日期处理
        processMsgListDateInfo(targetTopic.getMsgList());
        //通知Ui更新
        if (view != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    view.onNewMsgReceived(targetTopic.getTopicId());
                }
            });

        }
    }

    @Override
    public void doRemoveFromTopic(long topicId) {
        List<TopicItemBean> topics = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        TopicItemBean removedTopic = null;
        for (TopicItemBean topicItemBean : topics) {
            if (topicItemBean.getTopicId() == topicId) {
                removedTopic = topicItemBean;
                break;
            }
        }
        // TODO: 2018/5/22 删除数据库 中topic信息
        topics.remove(removedTopic);
        if (view != null) {
            final TopicItemBean finalRemovedTopic = removedTopic;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    view.onRemovedFromTopic(finalRemovedTopic.getTopicId());
                }
            });

        }
    }

    @Override
    public void doAddedToTopic(long topicId, boolean mqtt) {
        List<TopicItemBean> topics = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        //在网络请求结果中回调给UI
        updateTopicsWithMembers(topics, topicId + "", mqtt);
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

    public void sortTopics(List<TopicItemBean> topics) {
        ImTopicSorter.sortByLatestTime(topics);
    }

    /**
     * 跟新topic的最新一页msg
     *
     * @param topics
     */
    private void updateEachTopicMsgs(List<TopicItemBean> topics) {
        totalRetryTimes = 10;
        for (TopicItemBean topic : topics) {
            doGetTopicMsgsRequest(topic);
        }
        rqManager.setmCallBack(new RequestQueueCallBack());
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
            DatabaseManager.checkAndMigrateMockTopic();
        }
    }

    /**
     * 获取且仅用于 网路恢复连接后 拉取 最新一页的msglist
     * 当获取最新一页成功以后 清除已有数据
     */
    private void doGetTopicMsgsRequest(final TopicItemBean topicItemBean) {
        //判断是否需要更新msg
        if ((topicItemBean.getMsgList() != null) && (topicItemBean.getMsgList().size() > 0)) {
            MsgItemBean dbMsg = topicItemBean.getMsgList().get(0);
            if (dbMsg.getMsgId() >= topicItemBean.getLatestMsgId()) {
                // 数据库中已有最新的msg，不用更新
                topicItemBean.setLatestMsgId(dbMsg.getMsgId());
                topicItemBean.setLatestMsgTime(dbMsg.getSendTime());
                if (view != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            view.onTopicUpdate(topicItemBean.getTopicId());
                        }
                    });
                }
                return;
            }
        }
        //更新msg
        GetTopicMsgsRequest_new getTopicMsgsRequest = new GetTopicMsgsRequest_new();
        getTopicMsgsRequest.imToken = Constants.imToken;
        getTopicMsgsRequest.topicId = Long.toString(topicItemBean.getTopicId());
        //如果是最新加入的topic 没有消息记录 赋予latestmsgid 为long最大值
        //由于是获取最新消息 所以 请求startid 采用Long.MAXVALUE
        getTopicMsgsRequest.startId = String.valueOf(Long.MAX_VALUE);


        getTopicMsgsRequest.order = "desc";

        rqManager.addRequest(getTopicMsgsRequest, GetTopicMsgsResponse_new.class, new IYXHttpCallback<GetTopicMsgsResponse_new>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetTopicMsgsResponse_new ret) {
                // 拉取消息数量为0 获取数据为空
                if (ret.data.topicMsg == null || ret.data.topicMsg.size() == 0) {
                    //判断获取的消息数量是否为0 或空  此时 不显示红点
                    topicItemBean.setShowDot(false);
                    DatabaseManager.updateTopicWithTopicItemBean(topicItemBean);
                    //只有 msg list 进行了填充
                    if (view != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onTopicUpdate(topicItemBean.getTopicId());
                            }
                        });
                    }
                    return;
                }

                List<MsgItemBean> currentMsgList = topicItemBean.getMsgList();
                if (currentMsgList == null) {
                    currentMsgList = new ArrayList<>();
                    topicItemBean.setMsgList(currentMsgList);
                }

                //将获取的msg数据写入数据库
                List<MsgItemBean> newList = new ArrayList<>();
                for (ImMsg_new imMsgNew : ret.data.topicMsg) {
                    /*对服务器返回的msg 数据进行格式检查 防止空指针*/
                    if (ImServerDataChecker.imMsgCheck(imMsgNew)) {
                        newList.add(DatabaseManager.updateDbMsgWithImMsg(imMsgNew, Constants.imId));
                    }

                }
                //判断是否有新消息进来 (显示红点) topic 在上一步的http请求中已经被更新 所以直接比较 topicitembean的latestmsgid
                boolean showRedDot = TopicInMemoryUtils.checkTopicShouldShowRedDot(topicItemBean, newList);
                topicItemBean.setShowDot(showRedDot);
                //将红点信息保存数据库
                DatabaseManager.updateTopicWithTopicItemBean(topicItemBean);
                //清空上一次保存的数据
                currentMsgList.clear();

                //数据库获取最新一页msg
                ArrayList<MsgItemBean> msgsFromDb =
                        DatabaseManager.getTopicMsgs(topicItemBean.getTopicId(), DatabaseManager.minMsgId, DatabaseManager.pagesize);

                currentMsgList.addAll(msgsFromDb);

                //设置下次请求的msgid
//                topicItemBean.setRequestMsgId(TopicInMemoryUtils.getMinMsgBeanIdInList(newList));
                //处理日期显示
                processMsgListDateInfo(msgsFromDb);

                if (view != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            view.onTopicUpdate(topicItemBean.getTopicId());
                        }
                    });
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                // 重试
                if (totalRetryTimes-- <= 0) {
                    //重试多次失败 则不对数据进行处理 保存初始化时获取到的db数据最为显示
                    return;
                }
                doGetTopicMsgsRequest(topicItemBean);
            }
        });
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
     * @param topics  用户当前所有的 topic
     */
    public void checkUserRemove(final long topicId, final List<TopicItemBean> topics) {
        com.yanxiu.im.net.TopicGetTopicsRequest_new getTopicsRequest = new com.yanxiu.im.net.TopicGetTopicsRequest_new();
        getTopicsRequest.imToken = Constants.imToken;
        getTopicsRequest.topicIds = topicId + "";
        rqManager.addRequest(getTopicsRequest, com.yanxiu.im.net.TopicGetTopicsResponse_new.class, new IYXHttpCallback<com.yanxiu.im.net.TopicGetTopicsResponse_new>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, com.yanxiu.im.net.TopicGetTopicsResponse_new ret) {
                // 更新数据库
                List<TopicItemBean> topicsNeedUpdateMember = new ArrayList<>();
                com.yanxiu.im.bean.net_bean.ImTopic_new imTopic = null;
                List<com.yanxiu.im.bean.net_bean.ImTopic_new.Member> imMemberList = null;
                if (ret.data.topic == null || ret.data.topic.isEmpty()) {
                    //TODO:疑问：如果topic为空，是否意味着该topic已被删除？暂时不处理
                    return;
                }
                //获取当前本地持有的 topic 对象
                final TopicItemBean targetLocalTopic = TopicInMemoryUtils.findTopicByTopicId(topicId, topics);
                //获取 推送的 目标 topic
                imTopic = ret.data.topic.get(0);
                //获取 目标 topic 的最新 member 列表
                imMemberList = imTopic.members;
                /* 对比 当前用户持有的 topic member 列表与 服务器返回的最新 member 列表 删除被移除的 member*/
                synchronized (targetLocalTopic.getMembers()) {//可能同时两条 或多条 同一个 topic 的 member 推送 造成多线程操作 memberlist 所以加锁
                    ArrayList<DbMember> dbMembershasBeenDel = new ArrayList<>();
                    if (targetLocalTopic.getMembers() == null) {
                        // 不知道怎么办
                        return;
                    }
                    if (imMemberList == null) {
                        //为了 member 统一删除方法
                        imMemberList = new ArrayList<>();
                    }

                    for (DbMember dbMember : targetLocalTopic.getMembers()) {
                        boolean remain = false;
                        for (ImTopic_new.Member remainMember : imMemberList) {
                            if (dbMember.getImId() == remainMember.memberId) {
                                remain = true;
                                break;
                            }
                        }
                        if (!remain) {//如果用户已经不再列表中
                            dbMembershasBeenDel.add(dbMember);
                        }
                    }
                    //删除那些不在列表中的 member
                    targetLocalTopic.getMembers().removeAll(dbMembershasBeenDel);
                    //判断被移除的 member 是不是自己
                    for (DbMember removedMember : dbMembershasBeenDel) {
                        if (removedMember.getImId() == Constants.imId) {
                            /*如果是学员端 这里需要将 topic 列表进行删除 */
                            if (Constants.APP_TYPE == Constants.APP_TYPE_STUDENT) {
                                TopicInMemoryUtils.removeTopicFromListById(topicId, topics);
                            }
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (view != null) {
                                        view.onRemovedFromTopic(targetLocalTopic.getTopicId());
                                    }
                                }
                            });
                        } else {//如果不是自己被移除  通知 其他人被移除
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (view != null) {
                                        view.onOtherMemberRemoveFromTopic(targetLocalTopic.getTopicId());
                                    }
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {

            }
        });
    }
}
