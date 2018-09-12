package com.yanxiu.im.business.msglist.interfaces.impls;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.test.yanxiu.common_base.utils.SharedSingleton;
import com.yanxiu.im.Constants;
import com.yanxiu.im.TopicsReponsery;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.msglist.interfaces.MsgListContract;
import com.yanxiu.im.business.topiclist.sorter.ImTopicSorter;
import com.yanxiu.im.business.utils.ImDateFormateUtils;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.sender.ISender;
import com.yanxiu.im.sender.SenderFactory;
import com.yanxiu.im.sender.SenderManager;

import java.util.ArrayList;
import java.util.List;

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
        //首先创建 需要发送的 msgitebean
        final MsgItemBean msgItemBean = createTextMsgBean(msgStr, currentTopic);
        List<MsgItemBean> msgList = currentTopic.getMsgList();
        if (msgList == null) {
            msgList = new ArrayList<>();
            currentTopic.setMsgList(msgList);
        }
        msgList.add(0, msgItemBean);
        TopicInMemoryUtils.processMsgListDateInfo(msgList);
        sortInsertTopics(currentTopic.getTopicId());
        //内存 topic 处理完毕 回调 ui 进行显示
        view.onNewMsg();
        //判断当前 topic 是否为 mocktopic
        if (DatabaseManager.isMockTopic(currentTopic)) {
            //如果是 mocktopic 先请求创建 realtopic
            //先创建topic
            TopicsReponsery.getInstance().createNewTopic(currentTopic, currentTopic.getFromTopic(), new TopicsReponsery.CreateTopicCallback() {
                @Override
                public void onTopicCreatedSuccess(TopicItemBean topicItemBean) {
                    //创建成功  执行一次 递归操作
                    initSenderManager(currentTopic);
                    mTextSenderManager.addSender(msgItemBean.getISender());
                }

                @Override
                public void onTopicCreateFailed() {
                    //TODO 创建失败 直接显示 失败操作？！
                    Log.i(TAG, "onTopicCreateFailed: ");
                    view.onNewMsg();
                }
            });
        } else {

            //显示后 执行 发送
            initSenderManager(currentTopic);
            mTextSenderManager.addSender(msgItemBean.getISender());
        }
    }

    @NonNull
    private MsgItemBean createTextMsgBean(String msgStr, TopicItemBean currentTopic) {
        MsgItemBean msgItemBean = ImDateFormateUtils.createTextMsgBean(msgStr, currentTopic);
        //保存数据库
        DatabaseManager.createOrUpdateMyMsg(msgItemBean);
        return msgItemBean;
    }

    private void sortInsertTopics(long topicId) {

        List<TopicItemBean> topics = TopicsReponsery.getInstance().getTopicInMemory();
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
        //首先创建 需要发送的 msgitebean
        final MsgItemBean msgItemBean = createImgMsgBean(imgUrl, currentTopic);
        List<MsgItemBean> msgList = currentTopic.getMsgList();
        if (msgList == null) {
            msgList = new ArrayList<>();
            currentTopic.setMsgList(msgList);
        }
        msgList.add(0, msgItemBean);
        TopicInMemoryUtils.processMsgListDateInfo(msgList);
        sortInsertTopics(currentTopic.getTopicId());
        //内存 topic 处理完毕 回调 ui 进行显示
        view.onNewMsg();
        //判断当前 topic 是否为 mocktopic
        if (DatabaseManager.isMockTopic(currentTopic)) {
            //如果是 mocktopic 先请求创建 realtopic
            //先创建topic
            TopicsReponsery.getInstance().createNewTopic(currentTopic, currentTopic.getFromTopic(), new TopicsReponsery.CreateTopicCallback() {
                @Override
                public void onTopicCreatedSuccess(TopicItemBean topicItemBean) {
                    //创建成功  执行一次 递归操作
                    initSenderManager(currentTopic);
                    mImageSenderManager.addSender(msgItemBean.getISender());
                }

                @Override
                public void onTopicCreateFailed() {
                    //TODO 创建失败 直接显示 失败操作？！
                    Log.i(TAG, "onTopicCreateFailed: ");
                    view.onNewMsg();
                }
            });
        } else {
            //执行 发送
            initSenderManager(currentTopic);
            mImageSenderManager.addSender(msgItemBean.getISender());
        }
    }

    @NonNull
    private MsgItemBean createImgMsgBean(String imgUrl, TopicItemBean currentTopic) {
        final MsgItemBean imgMsgBean = ImDateFormateUtils.createImgMsgBean(imgUrl, currentTopic);
        //保存数据库
        DatabaseManager.createOrUpdateMyMsg(imgMsgBean);
        return imgMsgBean;
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
            DatabaseManager.checkAndMigrateMockTopic(TopicsReponsery.getInstance().getTopicInMemory());
        }
        if (DatabaseManager.isMockTopic(currentTopic)) {

            //先创建topic
            TopicsReponsery.getInstance().createNewTopic(currentTopic, currentTopic.getFromTopic(), new TopicsReponsery.CreateTopicCallback() {
                @Override
                public void onTopicCreatedSuccess(TopicItemBean topicItemBean) {
                    initSenderManager(currentTopic);
                    //开始发送msg
                    if (msgItemBean.getContentType() == 20) {
                        mImageSenderManager.addSender(sender);
                    } else {
                        mTextSenderManager.addSender(sender);
                    }
                }

                @Override
                public void onTopicCreateFailed() {
                    view.onCreateTopicFail();
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


    @Override
    public void doLoadMore(final TopicItemBean currentTopic) {
        if (currentTopic == null) {
            view.onLoadMoreFromDb(0);
            return;
        }
        //清空 历史数据标志位
        currentTopic.setLatestMsgIdWhenDeletedLocalTopic(-1);
        //数据库清空标志位
        DatabaseManager.updateTopicWithTopicItemBean(currentTopic);
        //获取起始 msgid
        final long startId = TopicInMemoryUtils.getMinImMsgIdInList(currentTopic.getMsgList());
        //执行手动刷新
        TopicsReponsery.getInstance().loadPageMsg(currentTopic, startId, new TopicsReponsery.GetMsgPageCallback() {
            @Override
            public void onGetPage(ArrayList<MsgItemBean> msgs) {
                if (msgs == null) {
                    view.onLoadMoreFromDb(0);
                } else {
                    view.onLoadMoreFromDb(msgs.size());
                }
            }
        });
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
        Log.i(TAG, "updateTopicInfo: ");
        if (currentTopic == null) {
            return;
        }
        TopicsReponsery.getInstance().updateTopicMemberInfoFromServer(currentTopic, new TopicsReponsery.GetTopicItemBeanCallback() {
            @Override
            public void onGetTopicItemBean(TopicItemBean bean) {
                if (currentTopic != null) {
                    currentTopic.setShowDot(false);
                }
                if (bean != null) {
                    view.onTopicInfoUpdate();
                }
            }
        });
    }

    /**
     * 点击用户头像 打开一个私聊
     * 1、私聊已经存在  2、私聊不存在
     */
    @Override
    public void openPrivateTopicByMember(final long memberId, String mName, final long fromTopicId) {
        Log.i(TAG, "openPrivateTopicByMember: ");
        //
        openPrivateTopic(memberId, mName, fromTopicId);
    }

    private void openPrivateTopic(long memberId, final String mName, long fromTopicId) {
        TopicsReponsery.getInstance().getPrivateTopicByMemberid(memberId, fromTopicId, new TopicsReponsery.GetPrivateTopicCallback<TopicItemBean>() {
            @Override
            public void onFindRealPrivateTopic(TopicItemBean bean) {
                Log.i(TAG, "onFindRealPrivateTopic: ");
                //本地有 这个私聊 直接显示
                view.onRealTopicOpened(bean);
            }

            @Override
            public void onNoTargetTopic(String memberName) {
                Log.i(TAG, "onNoTargetTopic: ");
                //本地没有这个私聊  进行 title 设置  等待下一步操作
                if (TextUtils.isEmpty(memberName)) {
                    memberName = mName;
                }
                view.onNewPrivateTopicOpened(memberName);
            }
        });
    }

    /**
     * 当用户 由topic列表点击topic 进入时 调用
     * 此时的 topic 一定存在 所以 只查找本地数据即可
     *
     * @param topicId 查找目标的id
     */
    @Override
    public void openTopicByTopicId(long topicId) {
        Log.i(TAG, "openTopicByTopicId: ");
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

                //判断 deletemsgid 对要展示的数据进行截断
                long deleteMsgId = targetTopic.getLatestMsgIdWhenDeletedLocalTopic();
                if (deleteMsgId > 0) {
                    TopicInMemoryUtils.cutoffMsgListByMsgId(deleteMsgId, targetTopic.getMsgList());
                }
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
     */
    @Override
    public void openPushTopic(final long topicId) {
        Log.i(TAG, "openPushTopic: ");
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
    public TopicItemBean createMockTopicForMsg(long memberId, long fromTopic, String memberName) {
        return TopicsReponsery.getInstance().createMockTopic(fromTopic, memberId, memberName);
    }

    /**
     * 检查当前 开启聊天界面的私聊与 mqtt通知的新topic 是否可以合并
     */
    public boolean checkNullTopicCanbeMerged(long topicId, long memberId) {
        final ArrayList<TopicItemBean> topics = TopicsReponsery.getInstance().getTopicInMemory();
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
        List<TopicItemBean> topics = TopicsReponsery.getInstance().getTopicInMemory();
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
