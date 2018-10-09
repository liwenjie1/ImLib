package com.yanxiu.im.business.contacts.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.yanxiu.im.R;
import com.yanxiu.im.business.contacts.adapter.TopicMemberAdapter;
import com.yanxiu.im.business.contacts.interfaces.TopicMemberListContract;
import com.yanxiu.im.business.contacts.interfaces.impls.TopicMemberListPresenter;
import com.yanxiu.im.business.view.ImTitleLayout;
import com.yanxiu.lib.yx_basic_library.util.YXToastUtil;

import java.util.ArrayList;

public class TopicMemberListActivity extends ImBaseActivity implements TopicMemberListContract.IView {


    private TopicMemberListPresenter mPresenter;


    private XRecyclerView mXRecyclerView;

    private String topicId;

    private ImTitleLayout mImTitleLayout;

    private TopicMemberAdapter mTopicMemberAdapter;

    private final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_member_list);
        mPresenter = new TopicMemberListPresenter(this);
        initView();
        initListener();
        initData();
    }

    private void initView() {
        mXRecyclerView = findViewById(R.id.loadmore_recyclerview);
        mImTitleLayout = findViewById(R.id.im_memberlist_title_layout);
    }

    private void initListener() {
        mImTitleLayout.setTitle("群成员");
        mImTitleLayout.setmTitlebarActionClickListener(new ImTitleLayout.TitlebarActionClickListener() {
            @Override
            public void onLeftComponentClicked() {
                onBackPressed();
            }

            @Override
            public void onRightComponpentClicked() {

            }
        });

        mXRecyclerView.setPullRefreshEnabled(true);
        mXRecyclerView.setLoadingMoreEnabled(false);
        LinearLayoutManager manager=new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mXRecyclerView.setLayoutManager(manager);
        mXRecyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                //
                if (!TextUtils.isEmpty(topicId)) {
                    mPresenter.doGetMemberList(topicId);
                } else {
                    //提示 刷新失败
                    mXRecyclerView.refreshComplete();
                }
            }

            @Override
            public void onLoadMore() {
//                if (!TextUtils.isEmpty(topicId)) {
//                    mPresenter.doLoadMoreMemberList(topicId, 0, 20);
//                } else {
//                    提示 加载更多失败
//                    mXRecyclerView.refreshComplete();
//                }

            }
        });
        mTopicMemberAdapter = new TopicMemberAdapter();
        mXRecyclerView.setAdapter(mTopicMemberAdapter);

    }

    private void initData() {
        //初始化数据 首先获取一页 member 信息
        topicId = getIntent().getStringExtra("topicId");
        if (!TextUtils.isEmpty(topicId)) {
            mPresenter.doGetMemberList(topicId);
        }
    }


    public static void invoke(Activity activity, long topicId) {
        Intent intent = new Intent(activity, TopicMemberListActivity.class);
        intent.putExtra("topicId", String.valueOf(topicId));
        activity.startActivity(intent);
    }

    @Override
    public void onGetMemberList(ArrayList memberList) {
        mXRecyclerView.refreshComplete();
        Log.i(TAG, "onGetMemberList: ");
        mTopicMemberAdapter.setDatas(memberList);
        mTopicMemberAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoadMemberList(ArrayList memberList) {
        mXRecyclerView.refreshComplete();
        Log.i(TAG, "onLoadMemberList: ");
    }

    @Override
    public void onException(final String msg) {
        Log.i(TAG, "onException: " + msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                YXToastUtil.showToast(msg);
            }
        });

    }
}
