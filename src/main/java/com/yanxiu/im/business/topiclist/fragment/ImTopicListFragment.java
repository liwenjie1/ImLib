package com.yanxiu.im.business.topiclist.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.test.yanxiu.common_base.ui.FaceShowBaseFragment;
import com.test.yanxiu.common_base.ui.PublicLoadLayout;
import com.test.yanxiu.common_base.utils.talkingdata.EventUpdate;
import com.yanxiu.ImConfig;
import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.TopicsReponsery;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.contacts.activity.ContactsActivity;
import com.yanxiu.im.business.interfaces.ImUnreadMsgListener;
import com.yanxiu.im.business.interfaces.ImUserRemoveFromTopicListener;
import com.yanxiu.im.business.interfaces.RecyclerViewItemLongClickListener;
import com.yanxiu.im.business.interfaces.TitlebarActionListener;
import com.yanxiu.im.business.msglist.activity.ImMsgListActivity;
import com.yanxiu.im.business.topiclist.adapter.ImTopicListRecyclerViewAdapter;
import com.yanxiu.im.business.topiclist.adapter.NpaLinearLayoutManager;
import com.yanxiu.im.business.topiclist.interfaces.MqttConnectContract;
import com.yanxiu.im.business.topiclist.interfaces.TopicListContract;
import com.yanxiu.im.business.topiclist.interfaces.impls.MqttConnectPresenter;
import com.yanxiu.im.business.topiclist.interfaces.impls.TopicListPresenter;
import com.yanxiu.im.business.topiclist.sorter.ImTopicSorter;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.business.view.ImTitleLayout;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.event.MigrateMockTopicEvent;
import com.yanxiu.im.event.MqttConnectedEvent;
import com.yanxiu.im.event.MsgListMigrateMockTopicEvent;
import com.yanxiu.im.event.MsgListNewMsgEvent;
import com.yanxiu.im.event.MsgListTopicChangeEvent;
import com.yanxiu.im.event.MsgListTopicRemovedEvent;
import com.yanxiu.im.event.MsgListTopicUpdateEvent;
import com.yanxiu.im.event.NewMsgEvent;
import com.yanxiu.im.event.TopicChangEvent;
import com.yanxiu.im.manager.MqttConnectManager;
import com.yanxiu.lib.yx_basic_library.customize.dialog.CommonDialog;
import com.yanxiu.lib.yx_basic_library.customize.dialog.CustomBaseDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.List;

/**
 * Created by 朱晓龙 on 2018/5/7 10:17.
 * topic 列表 页面
 */

public class ImTopicListFragment extends FaceShowBaseFragment
        implements TopicListContract.View<TopicItemBean>, MqttConnectContract.View {


    public ImTopicListFragment() {
        // Required empty public constructor
    }

    public void showTitleBarRed(boolean show){

    }

    public TitlebarActionListener titlebarActionListener;
    public ImUnreadMsgListener imUnreadMsgListener;
    private ImTitleLayout mImTitleLayout;

    private RecyclerView im_topiclist_fragment_recyclerview;
    private ImTopicListRecyclerViewAdapter<TopicItemBean> mRecyclerAdapter;
    private View root;

    private TopicListPresenter topicListPresenter;
    private MqttConnectPresenter mqttConnectPresenter;

    /**
     * TopicListFragment的持有者需要设置 titlebar的点击监听
     */
    public void setTitlebarActionListener(TitlebarActionListener titlebarActionListener) {
        this.titlebarActionListener = titlebarActionListener;
    }

    /**
     * ImTopicListFragment 的持有者需要设置新消息提醒的监听
     */
    public void setImUnreadMsgListener(ImUnreadMsgListener imUnreadMsgListener) {
        this.imUnreadMsgListener = imUnreadMsgListener;
    }

    private PublicLoadLayout mPublicLoadLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (root == null) {
            mPublicLoadLayout = new PublicLoadLayout(getContext());
            root = inflater.inflate(R.layout.im_topiclist_fragment, container, false);
            viewInit(root);
            mPublicLoadLayout.setContentView(root);

            listenerInit();
            //如果 成功获取了 im 相关信息 执行数据操作
            //读 db 的 耗时操作
            dataInit();
        }
        return mPublicLoadLayout;
    }


    private void viewInit(View view) {
        mImTitleLayout = view.findViewById(R.id.im_title_layout);
        im_topiclist_fragment_recyclerview = view.findViewById(R.id.im_topiclist_fragment_recyclerview);
        NpaLinearLayoutManager layoutManager
                = new NpaLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        layoutManager.setStackFromEnd(false);
        im_topiclist_fragment_recyclerview.setLayoutManager(layoutManager);

        topicListPresenter = new TopicListPresenter(this, getActivity());
        mqttConnectPresenter = new MqttConnectPresenter(this);
        mRecyclerAdapter = new ImTopicListRecyclerViewAdapter<TopicItemBean>(getActivity());
        redDot=view.findViewById(R.id.reddot_iv);
    }

    private void listenerInit() {
        mImTitleLayout.setmTitlebarActionClickListener(new ImTitleLayout.TitlebarActionClickListener() {
            @Override
            public void onLeftComponentClicked() {
                if (titlebarActionListener != null) {
                    titlebarActionListener.onLeftComponentClicked();
                }
            }

            @Override
            public void onRightComponpentClicked() {
                //学员端才有 通讯录功能
                //事件统计 点击通讯录
                EventUpdate.onClickContactEvent(getActivity());
                ContactsActivity.invoke(ImTopicListFragment.this);
                if (titlebarActionListener != null) {
                    titlebarActionListener.onRightComponpentClicked();
                }
            }
        });
        //点击topic 进入聊天界面
        mRecyclerAdapter.setTopicRecyclerViewClickListener(new ImTopicListRecyclerViewAdapter.TopicRecyclerViewClickListener() {
            @Override
            public void onTopicItemClicked(int position, TopicItemBean bean) {
                mRecyclerAdapter.notifyItemChanged(position);
                mRecyclerAdapter.notifyDataSetChanged();
                //事件统计 点击群聊 topic
                if (TextUtils.equals(bean.getType(), "2")) {
                    EventUpdate.onClickGroupTopicEvent(getActivity());
                }

                ImMsgListActivity.invoke(ImTopicListFragment.this, bean.getTopicId(), ImMsgListActivity.REQUEST_CODE_TOPICID);
            }
        });
        //长按删除
        mRecyclerAdapter.setRecyclerViewItemLongClickListener(new RecyclerViewItemLongClickListener() {

            @Override
            public boolean onItemLongClicked(final int position, final TopicItemBean bean) {
                CommonDialog dialog = new CommonDialog(getActivity());
                dialog.setTitleText(R.string.clear_topic_tip);
                dialog.setContentText(R.string.clear_topic);
                dialog.show();
                dialog.setOnClickListener(new CustomBaseDialog.CustomDialogOnClickListener() {
                    @Override
                    public void customDialogConfirm() {
                        TopicsReponsery.getInstance().deleteTopicHistory(bean, new TopicsReponsery.DeleteTopicCallback() {

                            @Override
                            public void onTopicDeleted() {
                                topicListPresenter.doCheckRedDot(topicListPresenter.getTopicInMemory());
                                mRecyclerAdapter.notifyItemChanged(position);
//                                mRecyclerAdapter.notifyItemRemoved(position);
//                                mRecyclerAdapter.notifyItemRangeChanged(position, mRecyclerAdapter.getItemCount() - position - 1);
                            }
                        });
                    }

                    @Override
                    public void customDialogCancel() {

                    }
                });
                return true;
            }
        });
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (topicListPresenter != null) {
            ImTopicSorter.sortByLatestTime(topicListPresenter.getTopicInMemory());
            topicListPresenter.doCheckRedDot(topicListPresenter.getTopicInMemory());
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
    }

    //获取数据库数据进行UI 显示
    private void dataInit() {
        glideInit();
        // 角色设置
        if (Constants.APP_TYPE == Constants.APP_TYPE_UNDEFINE) {
            throw new IllegalStateException("没有设置im模块 调用者的 客户端type （学员端还是管理端）");
        }
        if (Constants.showContacts) {
            mImTitleLayout.setTitleRightText("通讯录");
        }

        im_topiclist_fragment_recyclerview.setAdapter(mRecyclerAdapter);
        mImTitleLayout.setTitle("聊聊");

        //检查 im 部分的 info 情况
        if (ImConfig.isHasInitialazed()) {
            //获取topiclist 在onGetDbTopicList 中回调
            topicListPresenter.doGetDbTopicList(Constants.imId);
            mImTitleLayout.enableRightBtn(true);
        } else {
            mImTitleLayout.enableRightBtn(false);
            mPublicLoadLayout.showOtherErrorView("服务器连接失败，请重新登录");
        }
    }

    private void glideInit() {
        //glide 缓存设置
        GlideBuilder builder = new GlideBuilder(getContext());
        File cacheDir = getContext().getExternalCacheDir();//指定的是数据的缓存地址
        int diskCacheSize = 1024 * 1024 * 200;//磁盘缓存 200M
        builder.setDiskCache(new DiskLruCacheFactory(cacheDir.getPath(), "glide", diskCacheSize));
    }

    //region MVP ui层回调

    /**
     * 获取本地数据库数据后 有presenter进行数据处理
     * 处理完成通知ui显示topic列表
     *
     * @param dbTopicList 数据库内topiclist
     */
    @Override
    public void onGetDbTopicList(List<TopicItemBean> dbTopicList) {
        //显示dbtopic列表 已经排序
        mRecyclerAdapter.setDataList(dbTopicList);
        mRecyclerAdapter.notifyDataSetChanged();
        topicListPresenter.doCheckRedDot(dbTopicList);
        if (MqttConnectManager.getInstance().isConnected()) {
            topicListPresenter.doTopicListUpdate(dbTopicList);
        }
    }

    /**
     * 执行 {@link TopicListContract.Presenter} doGetTopicList 方法后的结果回调
     * 获取更新了topiclist中topic的信息后的回调
     */
    @Override
    public void onTopicListUpdate() {
        Log.i(TAG, "onTopicListUpdate: ");
        mRecyclerAdapter.setDataList(topicListPresenter.getTopicInMemory());
        mqttConnectPresenter.subscribeTopics(topicListPresenter.getTopicInMemory());

        mRecyclerAdapter.notifyDataSetChanged();
        //更新所有 topic 的 member 与 msg 信息
        topicListPresenter.doUpdateAllTopicInfo();
        //检查红点状态
        topicListPresenter.doCheckRedDot(mRecyclerAdapter.getDataList());

    }

    @Override
    public void onTopicUpdate(long topicId) {
        mRecyclerAdapter.notifyItemChangedByTopicId(topicId);
        //检查红点状态
        topicListPresenter.doCheckRedDot(mRecyclerAdapter.getDataList());
        EventBus.getDefault().post(new MsgListTopicUpdateEvent(topicId));
    }

    @Override
    public void onTopicInfoUpdate(long topicId) {
//        mRecyclerAdapter.notifyItemChangedByTopicId(topicId);
        mRecyclerAdapter.notifyDataSetChanged();
        EventBus.getDefault().post(new MsgListTopicChangeEvent(topicId));
    }

    /**
     * 执行红点检查 结果回调
     *
     * @param showing 是否有需要显示红点的topic
     */
    @Override
    public void onRedDotState(boolean showing) {
        //通知 上层activity 显示红点标记
        if (imUnreadMsgListener != null) {
            imUnreadMsgListener.hasUnreadMsg(showing);
        }
    }


    //endregion

    // region  EventBus 通知接收

    /**
     * 针对私聊toic
     * 本地有mocktopic的私聊存在，并且在服务器端获取了 相同的realtopic私聊
     * 对两者进行替换合并处理
     * 合并处理后 底层通过EventBus 通知UI层
     * 两种情况：1、被替换的mocktopic 不在使用中  直接刷新topiclist即可
     * 2、当前被替换的mocktopic 正在使用中，需要通过EventBus 通知MsgListActivity 对topic的数据内容
     * 进行刷新，主要是msglist 的显示
     * {@link ImMsgListActivity#migrateMockTopic(MsgListMigrateMockTopicEvent)}
     *
     * @param event
     */
    @Subscribe
    public void migrateMockTopicToRealTopic(MigrateMockTopicEvent event) {
        mRecyclerAdapter.notifyDataSetChanged();
        mqttConnectPresenter.subScribeTopic(event.topicId);
        topicListPresenter.doCheckRedDot(mRecyclerAdapter.getDataList());
        // 给MsgListActivity 发通知
        EventBus.getDefault().post(new MsgListMigrateMockTopicEvent(event.topicId));
    }

    /**
     * 由底层mqtt收到新消息通知，并生成msgItemBean后 通知UI层的回调
     * 首先 找到msg对应的topic 并将msgItemBean加入msglist中
     * 然后 在处理完成的MVP回调  {@link ImTopicListFragment#onNewMsgReceived(long)}
     *
     * @param event
     */
    @Subscribe
    public void onMqttNewMsg(NewMsgEvent event) {
        Log.i("mqtt", "onMqttNewMsg: ");
        MsgItemBean newMsg = event.msg;
        topicListPresenter.doReceiveNewMsg(newMsg);
    }

    @Subscribe
    public void onMqttConnected(MqttConnectedEvent event) {
        Log.i("mqtt", "onMqttConnected: ");
        //mqtt 服务器连接通知 通知后 刷新数据列表
        topicListPresenter.doTopicListUpdate();
    }


    /**
     * 新消息 获取后 列表处理完成的回调
     * 通知ui更新
     * 首先更新topiclist
     * 然后通知msglsitActivity {@link ImMsgListActivity#receiveNewMsg(MsgListNewMsgEvent)}
     */
    @Override
    public void onNewMsgReceived(long topicId) {
        mRecyclerAdapter.notifyDataSetChanged();
        //eventbus 通知 activity更新
        EventBus.getDefault().post(new MsgListNewMsgEvent(topicId));
        topicListPresenter.doCheckRedDot(mRecyclerAdapter.getDataList());
    }

    /**
     * 底层收到mqtt消息 topic member发生改变 通过presetner 处理不同的类型并回调处理结果给UI
     * 当前的改变消息类型有两种:
     * 1、 用户被加入到新的topic中
     * {@link ImTopicListFragment#onAddedToTopic(long)}
     * 2、有用户被当前的某个topic移除
     * 当前用户被移除
     * {@link ImTopicListFragment#onRemovedFromTopic(long, String)} }
     * 其他用户被移除
     * {@link ImTopicListFragment#onOtherMemberRemoveFromTopic(long)}
     *
     * @param event
     */
    @Subscribe
    public void onMqttTopicChange(TopicChangEvent event) {
        switch (event.type) {
            case AddTo: {
                topicListPresenter.doAddedToTopic(event.topicId, true);
            }
            break;
            case RemoveFrom: {
                topicListPresenter.checkUserRemove(event.topicId);
            }
            break;
            case TopicChange: {
                topicListPresenter.doUpdateTopicInfo(event.topicId);
            }
            break;
        }
    }

    /**
     * 当前用户被从某个topic 中移除 （presenter 已经完成对数据的处理）
     * 1、目标topic正在使用中 关闭（如果正在展示 ）聊天界面  刷新topic列表
     * {@link ImMsgListActivity#topicRemoved(MsgListTopicRemovedEvent)}
     * 2、目标topic不在使用中 直接刷新topic列表
     * <p>
     * <p>
     * 最后 将用户当前剩余的topiclist 数量回调给MainActivity
     *
     * @param topicId 被移除的topicid 通知MsgListActivity是用来判断是否是正在展示的topic
     */
    @Override
    public void onRemovedFromTopic(long topicId, String topicName) {
        //取消mqtt 订阅
        mqttConnectPresenter.unsubScribeTopic(topicId);
        /*学员端 刷新界面*/
        if (Constants.APP_TYPE == Constants.APP_TYPE_STUDENT) {
            mRecyclerAdapter.notifyDataSetChanged();
            final TopicItemBean topic = TopicInMemoryUtils.findTopicByTopicId(topicId, mRecyclerAdapter.getDataList());
            Toast.makeText(getActivity(), "【已被移出" + topicName + "】", Toast.LENGTH_SHORT).show();
        }
        //eventbus 通知 MsgListActivity 如果被删除的topic正在展示，关闭topic对应的聊天界面
        //这里学员端和管理端有区别 学员端需要 退出 msglist 界面 管理端 只需要需要更新 member 信息
        EventBus.getDefault().post(new MsgListTopicRemovedEvent(topicId));
        topicListPresenter.doCheckRedDot(mRecyclerAdapter.getDataList());
        //回调给mainactivity
        if (mImUserRemoveFromTopicListener != null) {
            int remainSize = 0;
            //遍历判断 datalist 所有 topic 的 member 列表中是否包含当前用户
            for (TopicItemBean itemBean : mRecyclerAdapter.getDataList()) {
                if (TextUtils.equals(itemBean.getType(), "2")) {
                    //群聊 判断 member 是否包含 当前 member
                    if (itemBean.getMembers() != null) {
                        for (DbMember remainMember : itemBean.getMembers()) {
                            if (remainMember.getImId() == Constants.imId) {
                                remainSize++;
                                break;
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "onRemovedFromTopic: 剩余 topic 数量 " + remainSize);
            mImUserRemoveFromTopicListener.onUserRemoved(remainSize);
        }
    }

    /**
     * 当前用户订阅的topic中 有某个topic中成员被删除
     * 目前不需要处理此情况
     */
    @Override
    public void onOtherMemberRemoveFromTopic(long topicId) {

    }

    /**
     * 当前用户被添加到一个新的topic中的回调 （presenter中已经处理完topic的添加以及数据获取）
     * 首先进行mqtt 的消息订阅
     * 然后通知topiclist进行列表的更新
     */
    @Override
    public void onAddedToTopic(long topicId) {
        //增加mqtt订阅
        mqttConnectPresenter.subScribeTopic(topicId);
    }


    //endregion

    private ImUserRemoveFromTopicListener mImUserRemoveFromTopicListener;

    public void setImUserRemoveFromTopicListener(ImUserRemoveFromTopicListener imUserRemoveFromTopicListener) {
        mImUserRemoveFromTopicListener = imUserRemoveFromTopicListener;
    }

    /**
     * 由聊天界面返回
     * 1、返回后需要对 topic进行重新排序 ，对在聊天界面运行时收到的msg以及topic进行处理
     * 2、对mainactivity 的红点显示进行处理
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //topic的消息队列有变化，需要对topic的信息（latestMsgId）进行更新设置并重新排序
        mRecyclerAdapter.setDataList(topicListPresenter.getTopicInMemory());
        mRecyclerAdapter.notifyDataSetChanged();
        topicListPresenter.doCheckRedDot(mRecyclerAdapter.getDataList());
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }
}
