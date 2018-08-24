package com.yanxiu.im.business.msglist.interfaces.impls;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.test.yanxiu.common_base.utils.SharedSingleton;
import com.yanxiu.im.Constants;
import com.yanxiu.im.TopicsReponsery;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.im.business.msglist.interfaces.MsgListContract;
import com.yanxiu.im.business.topiclist.sorter.ImTopicSorter;
import com.yanxiu.im.business.utils.ImServerDataChecker;
import com.yanxiu.im.business.utils.ImageFileUtils;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.db.DbMyMsg;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.net.GetTopicMsgsRequest_new;
import com.yanxiu.im.net.GetTopicMsgsResponse_new;
import com.yanxiu.im.net.TopicCreateTopicRequest_new;
import com.yanxiu.im.net.TopicCreateTopicResponse_new;
import com.yanxiu.im.sender.ISender;
import com.yanxiu.im.sender.SenderFactory;
import com.yanxiu.im.sender.SenderManager;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Request;

/**
 * Created by 朱晓龙 on 2018/5/8 15:17.
 * msglist 界面主要处理的是
 * 1、发送新消息 （创建、数据库写入、发送）
 * 2、下拉加载（http请求、更新/创建数据库消息）
 */

public class MsgListPresenter implements MsgListContract.IPresenter<MsgItemBean> {
    private final String TAG = getClass().getSimpleName();
    private Handler mHandler = new Handler();

    private MsgListContract.IView view;
    private Context mContext;

    /**
     * senderManager 控制消息的发送以及 消息发送状态的更新
     */
    private SenderManager mTextSenderManager;
    private SenderManager mImageSenderManager;

    public MsgListPresenter(MsgListContract.IView view, Context mContext) {
        this.view = view;
        this.mContext = mContext;
    }


    /**
     * 检查是否符合打开私聊的条件
     */
    public boolean doCheckMemberChat(long senderId, TopicItemBean curTopic) {
        // 非可用的topic
        if (curTopic == null || DatabaseManager.isMockTopic(curTopic)) {
            return false;
        }

        // 是私聊 不能再次创建私聊
        if (TextUtils.equals("1", curTopic.getType())) {
            return false;
        }

        //push test
        //头像是用户本人
        if (senderId == Constants.imId) {
            return false;
        }
        return true;
    }

    public boolean checkUserExsist(long senderId, TopicItemBean curTopic) {
//topic 中含有目标用户
        if (TopicInMemoryUtils.checkMemberInTopic(senderId, curTopic)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 消息发送 senderManager初始化
     */
    private void initSenderManager(TopicItemBean currentTopic) {
        mTextSenderManager = SharedSingleton.getInstance().get("text_" + currentTopic.getTopicId());
        if (mTextSenderManager == null) {
            mTextSenderManager = new SenderManager(Integer.MAX_VALUE);
            SharedSingleton.getInstance().set("text_" + currentTopic.getTopicId(), mTextSenderManager);
        }
        mImageSenderManager = SharedSingleton.getInstance().get("image_" + currentTopic.getTopicId());
        if (mImageSenderManager == null) {
            mImageSenderManager = new SenderManager();
            SharedSingleton.getInstance().set("image_" + currentTopic.getTopicId(), mImageSenderManager);
        }
    }


    /**
     * 重置当前topic的 红点显示标志
     * MsgListActivity收到 eventbus 通知后 重置新消息提醒显示
     */
    public void resetTopicRedDot(long msgTopicId, TopicItemBean currentTopic) {
        //判断是否是当前topic
        if (currentTopic == null) {
            return;
        }
        if (currentTopic.getTopicId() == msgTopicId) {
            currentTopic.setShowDot(false);
            DatabaseManager.updateTopicWithTopicItemBean(currentTopic);
        }
    }


    /**
     * 创建realTopic
     *
     * @param createTopicCallback
     */
    private void createRealTopic(long memberId, String fromTopicId, final TopicItemBean currentTopic, final CreateTopicCallback createTopicCallback) {
        TopicCreateTopicRequest_new createTopicRequest = new TopicCreateTopicRequest_new();
        createTopicRequest.imToken = Constants.imToken;
        createTopicRequest.topicType = "1"; // 私聊
        createTopicRequest.imMemberIds = Long.toString(Constants.imId) + "," + Long.toString(memberId);
        createTopicRequest.fromGroupTopicId = fromTopicId;
        createTopicRequest.startRequest(TopicCreateTopicResponse_new.class, new IYXHttpCallback<TopicCreateTopicResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, final TopicCreateTopicResponse_new ret) {

                ImTopic_new imTopic = null;
                if (ret != null && ret.data != null && ret.data.topic != null && !ret.data.topic.isEmpty()) {
                    // 应该只有一个imTopic
                    imTopic = ret.data.topic.get(0);
                }
                if (imTopic == null) { //视为失败
                    DatabaseManager.topicCreateFailed(currentTopic);
                    createTopicCallback.onFail();
                    return;
                }
                DatabaseManager.migrateMockTopicToRealTopic(currentTopic, imTopic);
                //创建topic 成功 开始发送消息
                createTopicCallback.onSuccess();

            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                DatabaseManager.topicCreateFailed(currentTopic);
                createTopicCallback.onFail();
            }
        });
    }

    private interface CreateTopicCallback {
        void onSuccess();

        void onFail();
    }


    /**
     * 执行发送文字消息的逻辑、数据处理
     * 1、创建文字类型的 MsgItemBean
     * 2、加入到当前topic 的msglist
     * 3、处理日期显示
     * 4、回调UI 显示要发送的文字消息
     * 5、检查当前topic是否为mocktopic 并创建realtopic
     * 6、获取文字senderManager 对象（用于消息发送）
     * 7、执行消息发送
     *
     * @param msgStr       文字消息的内容
     * @param currentTopic 当前正在展示的topic  可能为mocktopic 或 realtopic
     */
    @Override
    public void doSendTextMsg(final String msgStr, @NonNull final TopicItemBean currentTopic) {
        //根据文字内容创建msgbean 如果是临时topic currentTopic 是一个mocktopic
        final MsgItemBean msgItemBean = createTextMsgBean(msgStr, currentTopic);
        List<MsgItemBean> msgList = currentTopic.getMsgList();
        if (msgList == null) {
            msgList = new ArrayList<>();
            currentTopic.setMsgList(msgList);
        }

        msgList.add(0, msgItemBean);
        TopicInMemoryUtils.processMsgListDateInfo(msgList);
        sortInsertTopics(currentTopic.getTopicId());
        //通知UI 更新
        if (view != null) {
            view.onNewMsg();
        }

        if (DatabaseManager.isMockTopic(currentTopic)) {

            //先创建topic
            long memberId = -1;
            for (DbMember memberNew : currentTopic.getMembers()) {
                if (memberNew.getImId() != Constants.imId) {
                    memberId = memberNew.getImId();
                    break;
                }
            }
            createRealTopic(memberId, currentTopic.getFromTopic(), currentTopic, new CreateTopicCallback() {
                @Override
                public void onSuccess() {
                    initSenderManager(currentTopic);
                    mTextSenderManager.addSender(msgItemBean.getISender());
                }

                @Override
                public void onFail() {
                    if (view != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onCreateTopicFail();
                            }
                        });

                    }
                }
            });
        } else {
            //如果不是mocktopic 直接发送
            initSenderManager(currentTopic);
            mTextSenderManager.addSender(msgItemBean.getISender());
        }
    }

    @NonNull
    private MsgItemBean createTextMsgBean(String msgStr, TopicItemBean currentTopic) {
        MsgItemBean msgItemBean = new MsgItemBean(MsgItemBean.MSG_TYPE_MYSELF, 10);
        msgItemBean.setState(DbMyMsg.State.Sending.ordinal());
        msgItemBean.setLocalViewUrl("");
        msgItemBean.setMsgId(currentTopic.generateMyMsgId());
        msgItemBean.setTopicId(currentTopic.getTopicId());
        msgItemBean.setMsg(msgStr);
        msgItemBean.setSenderId(Constants.imId);
        msgItemBean.setReqId(UUID.randomUUID().toString());
        msgItemBean.setSendTime(System.currentTimeMillis());
        //保存数据库
        DatabaseManager.createOrUpdateMyMsg(msgItemBean);
        return msgItemBean;
    }

    private void sortInsertTopics(long topicId) {
        List<TopicItemBean> topics = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        if (topics == null) {
            return;
        }
        ImTopicSorter.insertTopicToTop(topicId, topics);
    }

    /**
     * 执行发送图片消息的逻辑、数据处理
     * 1、创建图片类型消息 itembean
     * 2、加入到当前topic 的msglist
     * 3、处理日期显示
     * 4、回调UI 显示要发送的图片消息
     * 5、检查当前topic是否为mocktopic 并创建realtopic
     * 6、获取文字senderManager 对象（用于消息发送）
     * 7、执行消息发送
     *
     * @param imgUrl       图片消息的七牛路径
     * @param currentTopic 当前正在展示的topic  可能为mocktopic 或 realtopic
     */
    @Override
    public void doSendImgMsg(final String imgUrl, final TopicItemBean currentTopic) {
        final MsgItemBean msgItemBean = createImgMsgBean(imgUrl, currentTopic);
        List<MsgItemBean> msgList = currentTopic.getMsgList();
        if (msgList == null) {
            msgList = new ArrayList<>();
            currentTopic.setMsgList(msgList);
        }
        msgList.add(0, msgItemBean);
        sortInsertTopics(currentTopic.getTopicId());
        //通知UI 更新
        if (view != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    view.onNewMsg();
                }
            });

        }

        if (DatabaseManager.isMockTopic(currentTopic)) {

            //先创建topic
            long memberId = -1;
            for (DbMember memberNew : currentTopic.getMembers()) {
                if (memberNew.getImId() != Constants.imId) {
                    memberId = memberNew.getImId();
                    break;
                }
            }
            createRealTopic(memberId, currentTopic.getFromTopic(), currentTopic, new CreateTopicCallback() {
                @Override
                public void onSuccess() {
                    initSenderManager(currentTopic);
                    mImageSenderManager.addSender(msgItemBean.getISender());
                }

                @Override
                public void onFail() {
                    if (view != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onCreateTopicFail();
                            }
                        });

                    }
                }
            });
        } else {
            //开始发送msg
            initSenderManager(currentTopic);
            mImageSenderManager.addSender(msgItemBean.getISender());
        }
    }

    @NonNull
    private MsgItemBean createImgMsgBean(String imgUrl, TopicItemBean currentTopic) {
        MsgItemBean msgItemBean = new MsgItemBean(MsgItemBean.MSG_TYPE_MYSELF, 20);
        msgItemBean.setState(DbMyMsg.State.Sending.ordinal());
        msgItemBean.setLocalViewUrl(imgUrl);
        msgItemBean.setMsgId(currentTopic.generateMyMsgId());
        msgItemBean.setTopicId(currentTopic.getTopicId());
        msgItemBean.setMsg("");
        msgItemBean.setSenderId(Constants.imId);
        msgItemBean.setReqId(UUID.randomUUID().toString());
        msgItemBean.setSendTime(System.currentTimeMillis());
        Integer[] size = ImageFileUtils.getPicWithAndHeight(imgUrl);
        msgItemBean.setWidth(size[0]);
        msgItemBean.setHeight(size[1]);
        //保存数据库
        DatabaseManager.createOrUpdateMyMsg(msgItemBean);
        return msgItemBean;
    }

    @Override
    public void doResendMsg(final int p, final TopicItemBean currentTopic) {

        //在原位置移除
        List<MsgItemBean> msgList = currentTopic.getMsgList();
        final MsgItemBean msgItemBean = msgList.get(p);
        msgList.remove(p);

        //更新 item
        msgItemBean.setSendTime(System.currentTimeMillis());
        msgItemBean.setMsgId(currentTopic.generateMyMsgId());
        final ISender sender = SenderFactory.createSender(msgItemBean);
        msgItemBean.setISender(sender);
        msgItemBean.setState(1);
        //更新数据库
        DatabaseManager.updateDbMsgWithMsgItemBean(msgItemBean);
        //重新添加到队首
        msgList.add(0, msgItemBean);
        //检查日期显示
        TopicInMemoryUtils.processMsgListDateInfo(msgList);

        sortInsertTopics(currentTopic.getTopicId());
        //通知UI显示
        if (view != null) {
            view.onResendMsg(p);
        }
        //检查是否已经建立了 相同的私聊
        if (DatabaseManager.isMockTopic(currentTopic)) {
            DatabaseManager.checkAndMigrateMockTopic();
        }
        if (DatabaseManager.isMockTopic(currentTopic)) {

            //先创建topic
            long memberId = -1;
            for (DbMember memberNew : currentTopic.getMembers()) {
                if (memberNew.getImId() != Constants.imId) {
                    memberId = memberNew.getImId();
                    break;
                }
            }
            createRealTopic(memberId, currentTopic.getFromTopic(), currentTopic, new CreateTopicCallback() {
                @Override
                public void onSuccess() {
                    initSenderManager(currentTopic);
                    //开始发送msg
                    if (msgItemBean.getContentType() == 20) {
                        mImageSenderManager.addSender(sender);
                    } else {
                        mTextSenderManager.addSender(sender);
                    }
                }

                @Override
                public void onFail() {
                    if (view != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onCreateTopicFail();
                            }
                        });

                    }
                }
            });
        } else {
            //已有mocktopic
            initSenderManager(currentTopic);
            //开始发送msg
            if (msgItemBean.getContentType() == 20) {
                mImageSenderManager.addSender(sender);
            } else {
                mTextSenderManager.addSender(sender);
            }
        }
    }

    private Handler handler = new Handler();

    @Override
    public void doLoadMore(final TopicItemBean currentTopic) {
        //最小延迟 500
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GetTopicMsgsRequest_new getMsgsRequest = new GetTopicMsgsRequest_new();
                getMsgsRequest.imToken = Constants.imToken;
                //临时topic 在未发送消息之前currentTopic为空
                if (currentTopic == null || DatabaseManager.isMockTopic(currentTopic)) {
                    if (view != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onLoadMoreFromHttp(0);
                            }
                        });

                    }
                    return;
                }


                //获取startId
                //首先如果 msglist为空 说明 登录时以及db中都没有msglist 以Long.MAXVALUE 请求开始请求
                long startId = Long.MAX_VALUE;
                if (currentTopic.getMsgList() == null) {
                    currentTopic.setMsgList(new ArrayList<MsgItemBean>());
                } else {
                    //如果msglist不为空 采用最旧的msg进行请求 realMsgId 为服务器上的msgId 用于http请求
                    startId = TopicInMemoryUtils.getMinMsgBeanRealIdInList(currentTopic.getMsgList());
                }

                getMsgsRequest.startId = String.valueOf(startId);
                getMsgsRequest.topicId = currentTopic.getTopicId() + "";

                getMsgsRequest.startRequest(GetTopicMsgsResponse_new.class, new IYXHttpCallback<GetTopicMsgsResponse_new>() {
                    @Override
                    public void onRequestCreated(Request request) {
                    }

                    @Override
                    public void onSuccess(YXRequestBase request, GetTopicMsgsResponse_new ret) {
                        //请求异常
                        if (ret == null || ret.code != 0) {
                            if (view != null) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        view.onLoadMoreFromHttp(0);
                                    }
                                });
                            }
                            return;
                        }
                        //返回数据异常 或 没有数据
                        if (ret.data == null || ret.data.topicMsg == null) {
                            if (view != null) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        view.onLoadMoreFromHttp(0);
                                    }
                                });
                            }
                            return;
                        }

                        //保存入数据库
                        for (ImMsg_new msgNew : ret.data.topicMsg) {
                            /*检查 有服务器返回的msg 数据格式 防止空指针*/
                            if (ImServerDataChecker.imMsgCheck(msgNew)) {
                                DatabaseManager.updateDbMsgWithImMsg(msgNew, Constants.imId);
                            }
                        }
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                loadMsgFromDb(currentTopic);
                            }
                        });
                    }

                    @Override
                    public void onFail(YXRequestBase request, Error error) {
                        loadMsgFromDb(currentTopic);
                    }
                });
            }
        }, 500);
    }


    /**
     * 从数据库读取一页 msg
     */
    private void loadMsgFromDb(TopicItemBean currentTopic) {
        List<MsgItemBean> currentMsgList = currentTopic.getMsgList();
        //从数据库读 加载的一页
        long dbStartId = TopicInMemoryUtils.getMinImMsgIdInList(currentMsgList);
        final List<MsgItemBean> msgPage = DatabaseManager.getTopicMsgs(currentTopic.getTopicId(), dbStartId, DatabaseManager.pagesize);
        if (msgPage == null || msgPage.size() == 0) {
            //获取的列表为空？
            if (view != null) {
                view.onLoadMoreFromHttp(0);
            }
            return;
        }

        //去重
        TopicInMemoryUtils.duplicateRemoval(msgPage, currentMsgList);
        //加入消息列表
        if (msgPage.size() > 0) {
            currentMsgList.addAll(msgPage);
            //更新 requestMsgId
            currentTopic.setRequestMsgId(msgPage.get(msgPage.size() - 1).getRealMsgId());

        }


        TopicInMemoryUtils.processMsgListDateInfo(currentMsgList);
        if (view != null) {
            view.onLoadMoreFromDb(msgPage.size());
        }
    }


    /**
     * 由聊天界面返回时 需要将msglist进行截取 只保留最新的20条
     */
    public void doCuroffMsgList(TopicItemBean currentTopic) {
        if (currentTopic == null) {
            return;
        }
        if (currentTopic.getMsgList() == null) {
            return;
        }

        if (currentTopic.getMsgList().size() > 20) {
            MsgItemBean curBean = currentTopic.getMsgList().get(20);
            if (curBean.getType() == MsgItemBean.MSG_TYPE_MYSELF) {
                //继续向上查找
                for (int i = 20; i < currentTopic.getMsgList().size(); i++) {
                    if (currentTopic.getMsgList().get(i).getType() == MsgItemBean.MSG_TYPE_OTHER_PEOPLE) {
                        curBean = currentTopic.getMsgList().get(i);
                        break;
                    }
                }
            }

            int curIndex = currentTopic.getMsgList().indexOf(curBean);

            List<MsgItemBean> remainMsgs = new ArrayList<>();
            for (int i = 0; i <= curIndex; i++) {
                remainMsgs.add(currentTopic.getMsgList().get(i));
            }
            currentTopic.getMsgList().clear();
            currentTopic.getMsgList().addAll(remainMsgs);
        }
    }

    /**
     * 请求更新 topicinfo
     */
    public void updateTopicInfo(final TopicItemBean currentTopic) {
        TopicsReponsery.getInstance().updateTopicInfo(currentTopic, new TopicsReponsery.GetTopicItemBeanCallback() {
            @Override
            public void onGetTopicItemBean(TopicItemBean bean) {
                if (bean != null) {
                    view.onTopicInfoUpdate();
                }
            }
        });
    }

    /**
     * 点击用户头像 打开一个私聊
     * 1、私聊已经存在  2、私聊不存在
     * */
    @Override
    public void openPrivateTopicByMember(long memberId, long fromTopicId) {
        TopicsReponsery.getInstance().getPrivateTopicByMemberid(memberId, fromTopicId, new TopicsReponsery.GetPrivateTopicCallback<TopicItemBean>() {
            @Override
            public void onFindRealPrivateTopic(TopicItemBean bean) {
                //本地有 这个私聊 直接显示
                view.onRealTopicOpened(bean);
            }

            @Override
            public void onNoTargetTopic(String memberName) {
                //本地没有这个私聊  进行 title 设置  等待下一步操作
                view.onNewPrivateTopicOpened(memberName);
            }
        });
    }

    /**
     * 当用户 由topic列表点击topic 进入时 调用
     *  此时的 topic 一定存在 所以 只查找本地数据即可
     * @param topicId 查找目标的id
     */
    @Override
    public void openTopicByTopicId(long topicId) {
        TopicsReponsery.getInstance().getLocalTopic(topicId, new TopicsReponsery.GetTopicItemBeanCallback() {
            @Override
            public void onGetTopicItemBean(TopicItemBean targetTopic) {
                //数据库获取最新一页 msg
                ArrayList<MsgItemBean> msgsFromDb =
                        DatabaseManager.getTopicMsgs(targetTopic.getTopicId(), DatabaseManager.minMsgId, DatabaseManager.pagesize);
                targetTopic.getMsgList().clear();
                targetTopic.getMsgList().addAll(msgsFromDb);
                //处理
                TopicInMemoryUtils.processMsgListDateInfo(msgsFromDb);
                targetTopic.setName(targetTopic.getGroup());
                targetTopic.setShowDot(false);
                DatabaseManager.updateTopicWithTopicItemBean(targetTopic);
                if (view != null) {
                    view.onRealTopicOpened(targetTopic);
                }
            }
        });
    }
    /**
     * 有 push 推送过来  打开的对话
     * 1、本地有目标 topic
     * 2、本地没有目标 topic 需要创建一个临时的 TempTopic 来显示界面 并等待服务器请求目标 topic 的详细信息回来更新
     * */
    @Override
    public void openPushTopic(final long topicId) {
        //首先从本地获取
        TopicsReponsery.getInstance().getLocalTopic(topicId, new TopicsReponsery.GetTopicItemBeanCallback() {
            @Override
            public void onGetTopicItemBean(TopicItemBean bean) {
                //判断是否获取到
                if (bean == null) {
                    //创建一个temp 并请求服务器进行 数据更新
                    bean = TopicsReponsery.getInstance().createTempTopicBean(topicId, "pushTopic");
                }
                //将临时 topic 回调 给 UI
                view.onPushTopicOpend(bean);
                //请求服务器获取 push topic 的详细信息
                updateTopicInfo(bean);
            }
        });
    }

    /**
     * 创建一个 mocktopic
     * 用于保存 临时对话的 msglist
     */
    @Override
    public TopicItemBean createMockTopicForMsg(long memberId, long fromTopic) {
        List<TopicItemBean> topics = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        TopicItemBean mockTopic = DatabaseManager.createMockTopic(memberId, fromTopic);
        if (topics != null) {
            topics.add(mockTopic);
            ImTopicSorter.sortByLatestTime(topics);
        }
        return mockTopic;
    }

    /**
     * 检查当前 开启聊天界面的私聊与 mqtt通知的新topic 是否可以合并
     */
    public boolean checkNullTopicCanbeMerged(long topicId, long memberId) {
        List<TopicItemBean> topics = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        if (topics == null) {
            return false;
        }
        TopicItemBean targetTopic = TopicInMemoryUtils.findTopicByTopicId(topicId, topics);
        if (targetTopic == null) {
            return false;
        }
        if (!TextUtils.equals("1", targetTopic.getType())) {
            return false;
        }
        if (targetTopic.getMsgList() == null || targetTopic.getMembers().size() == 0) {
            return false;
        }

        for (DbMember member : targetTopic.getMembers()) {
            if (member.getImId() == memberId) {
                return true;
            }
        }

        return false;
    }

    public TopicItemBean getTargetTopic(long topicId) {
        List<TopicItemBean> topics = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        return TopicInMemoryUtils.findTopicByTopicId(topicId, topics);
    }


    /**
     * 这两个方法 应对 点击查看大图后 在大图界面左右滑动切换浏览当前消息列表的所有图片
     */
    public List<MsgItemBean> getAllImageMsgs(ArrayList<MsgItemBean> msgList) {
        List<MsgItemBean> imgList = new ArrayList<>();
        for (MsgItemBean msgDataBean : msgList) {
            if (msgDataBean.getContentType() == 20) {
                //倒序插入 符合习惯 向左滑 查看较早消息 向右滑 查看更新的消息
                imgList.add(0, msgDataBean);
            }
        }
        return imgList;
    }

    public List<String> getAllImageUrls(ArrayList<MsgItemBean> msgList) {
        List<String> urls = new ArrayList<>();
        for (MsgItemBean msgDataBean : msgList) {
            if (msgDataBean.getContentType() == 20) {
                urls.add(msgDataBean.getViewUrl());
            }
        }
        return urls;
    }

}
