package com.yanxiu.im.business.contacts.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.yanxiu.im.R;
import com.yanxiu.im.business.contacts.interfaces.TopicMemberListContract;
import com.yanxiu.im.business.contacts.interfaces.impls.ContactsMemberListPresenter;
import com.yanxiu.im.business.view.ImTitleLayout;

import java.util.ArrayList;

public class ContactMemberListActivity extends ImBaseActivity implements TopicMemberListContract.IView {


    private ContactsMemberListPresenter mPresenter;


    private XRecyclerView mXRecyclerView;

    private String topicId;

    private ImTitleLayout mImTitleLayout;

    private final String TAG=getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_member_list);
        mPresenter = new ContactsMemberListPresenter(this);
        initView();
        initListener();
        initData();
    }

    private void initView() {
        mXRecyclerView = findViewById(R.id.loadmore_recyclerview);
        mImTitleLayout=findViewById(R.id.im_memberlist_title_layout);
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
        mXRecyclerView.setLoadingMoreEnabled(true);
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
                if (!TextUtils.isEmpty(topicId)) {
                    mPresenter.doLoadMoreMemberList(topicId, 0, 20);
                } else {
                    //提示 加载更多失败
                    mXRecyclerView.refreshComplete();
                }

            }
        });

    }

    private void initData() {
        //初始化数据 首先获取一页 member 信息
        topicId = getIntent().getStringExtra("topicId");
        if (!TextUtils.isEmpty(topicId)) {
            mPresenter.doGetMemberList(topicId);
        }
    }


    public static void invoke(Activity activity, long topicId) {
        Intent intent = new Intent(activity, ContactMemberListActivity.class);
        intent.putExtra("topicId", String.valueOf(topicId));
        activity.startActivity(intent);
    }

    @Override
    public void onGetMemberList(ArrayList memberList) {
        Log.i(TAG, "onGetMemberList: ");
    }

    @Override
    public void onLoadMemberList(ArrayList memberList) {
        Log.i(TAG, "onLoadMemberList: ");
    }

    @Override
    public void onException(String msg) {
        Log.i(TAG, "onException: "+msg);
    }
}
