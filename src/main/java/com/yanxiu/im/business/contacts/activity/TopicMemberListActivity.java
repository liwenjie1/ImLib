package com.yanxiu.im.business.contacts.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.test.yanxiu.common_base.ui.InputMethodUtil;
import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.business.contacts.adapter.TopicMemberAdapter;
import com.yanxiu.im.business.contacts.interfaces.TopicMemberListContract;
import com.yanxiu.im.business.contacts.interfaces.impls.TopicMemberListPresenter;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;
import com.yanxiu.im.business.msglist.activity.ImMsgListActivity;
import com.yanxiu.im.business.view.ImTitleLayout;
import com.yanxiu.im.net.GetImIdByUserIdResponse;
import com.yanxiu.im.net.GetImIdByUseridRequest;
import com.yanxiu.im.net.GetTopicMemberListResponse;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;
import com.yanxiu.lib.yx_basic_library.util.YXToastUtil;

import java.util.ArrayList;

import okhttp3.Request;

public class TopicMemberListActivity extends ImBaseActivity implements TopicMemberListContract.IView {


    private TopicMemberListPresenter mPresenter;


    private XRecyclerView mXRecyclerView;

    private String topicId;

    private ImTitleLayout mImTitleLayout;

    private TopicMemberAdapter mTopicMemberAdapter;

    private SearchView mSearchView;

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

    /**
     * V7包原生的SearchView在UI上有些不符  所以做了些修改
     * 可参考: https://tanjundang.github.io/2016/11/17/SearchView/
     *
     * @param searchView searchView
     */
    private void modifySearchViewStyle(SearchView searchView) {
        SearchView.SearchAutoComplete tv = searchView.findViewById(R.id.search_src_text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.color_333333));
        tv.setHintTextColor(ContextCompat.getColor(this, R.color.color_999999));
        tv.setTextSize(14);

        ImageView imageView = searchView.findViewById(R.id.search_mag_icon);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams.width = 51;
        layoutParams.height = 51;
        imageView.setLayoutParams(layoutParams);
    }

    private void initView() {
        mXRecyclerView = findViewById(R.id.loadmore_recyclerview);
        mImTitleLayout = findViewById(R.id.im_memberlist_title_layout);
        mSearchView = findViewById(R.id.search_view);
        modifySearchViewStyle(mSearchView);
        mSearchView.clearFocus();
        hideSoftInput();
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
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mTopicMemberAdapter.getFilter().filter(newText);
                return true;
            }
        });
        mXRecyclerView.setPullRefreshEnabled(true);
        mXRecyclerView.setLoadingMoreEnabled(false);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mXRecyclerView.setLayoutManager(manager);
        mXRecyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                //
                hideSoftInput();
                //清空搜索框
                mSearchView.setQuery("", false);
                if (!TextUtils.isEmpty(topicId)) {
                    mPresenter.doGetMemberList(topicId);
                } else {
                    //提示 刷新失败
                    mXRecyclerView.refreshComplete();
                }
            }

            @Override
            public void onLoadMore() {

            }
        });
        mTopicMemberAdapter = new TopicMemberAdapter();
        mXRecyclerView.setAdapter(mTopicMemberAdapter);
        mTopicMemberAdapter.setRecyclerViewItemOnClickListener(new RecyclerViewItemOnClickListener() {
            @Override
            public void onItemClicked(View view, int position) {
                GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean contactsBean = mTopicMemberAdapter.getDatas().get(position);
                requestImId(true, contactsBean);
            }
        });
    }

    /**
     * 关闭软键盘
     */
    private void hideSoftInput() {
        mSearchView.clearFocus();
        InputMethodUtil.closeInputMethod(this, mSearchView);
    }

    private long mImMemberId;//im member id
    private String mTopicGroup;

    /**
     * 根据用户id获取Im id
     */
    private void requestImId(final boolean isFirst, final GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean bean) {
        GetImIdByUseridRequest getImIdRequest = new GetImIdByUseridRequest();
        getImIdRequest.userId = String.valueOf(bean.getMemberInfo().getUserId());
        getImIdRequest.imToken = Constants.imToken;
        getImIdRequest.fromGroupTopicId = topicId;
        getImIdRequest.startRequest(GetImIdByUserIdResponse.class, new IYXHttpCallback<GetImIdByUserIdResponse>() {

            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetImIdByUserIdResponse ret) {
                if (ret != null && ret.data != null && ret.code == 0) {
                    mImMemberId = ret.data.memberId;
                    mTopicGroup = ret.data.topic.topicGroup;

                    if (mImMemberId == Constants.imId) {
                        return;
                    }

                    ImMsgListActivity.invoke(TopicMemberListActivity.this,
                            mImMemberId,
                            bean.getMemberInfo().getMemberName(),
                            bean.getMemberInfo().getAvatar(),
                            Long.parseLong(topicId),
                            mTopicGroup, ImMsgListActivity.REQUEST_CODE_MEMBERID);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                YXToastUtil.showToast(error.getMessage());
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
