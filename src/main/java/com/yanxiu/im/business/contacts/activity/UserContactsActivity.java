package com.yanxiu.im.business.contacts.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.test.yanxiu.common_base.ui.InputMethodUtil;
import com.test.yanxiu.common_base.ui.PublicLoadLayout;
import com.test.yanxiu.common_base.utils.talkingdata.EventUpdate;
import com.yanxiu.im.R;
import com.yanxiu.im.business.contacts.adapter.UserContactsClazsAdapter;
import com.yanxiu.im.business.contacts.adapter.UserContactsMemberAdapter;
import com.yanxiu.im.business.contacts.interfaces.UserContactsContract;
import com.yanxiu.im.business.contacts.interfaces.impls.UserContactsPresenter;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;
import com.yanxiu.im.net.GetClazsListResponse;
import com.yanxiu.im.net.GetContactMembersResponse_new;

import java.util.List;

public class UserContactsActivity extends ImBaseActivity implements UserContactsContract.IView<GetClazsListResponse.ClazsInfosBean, GetContactMembersResponse_new.AdressData>, View.OnClickListener {
    private PublicLoadLayout mRootView;
    private ImageView iv_back;
    private SearchView search_view;
    private RelativeLayout rl_group_switch;
    private TextView tv_current_group_name;
    private ImageView iv_group_switch_arrow;
    private LinearLayout ll_contacts_group;
    private TextView tv_select_ok;

    private RecyclerView rv_contacts_groups;
    private XRecyclerView rv_contacts_members;

    //班级列表adapter
    private UserContactsClazsAdapter mGroupAdapter;
    //成员列表adapter
    private UserContactsMemberAdapter mMemberAdapter;

    private UserContactsPresenter mContactsPresenter;


    public static void invoke(Activity activity) {
        Intent intent = new Intent(activity, UserContactsActivity.class);
        activity.startActivity(intent);
    }

    public static void invoke(android.support.v4.app.Fragment fragment) {
        Intent intent = new Intent(fragment.getActivity(), UserContactsActivity.class);
        fragment.startActivityForResult(intent, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
        initListener();

        mContactsPresenter = new UserContactsPresenter(this);
        //请求班级列表
        mContactsPresenter.doLoadClazsList();
    }


    private void initViews() {
        mRootView = new PublicLoadLayout(this);
        mRootView.setContentView(R.layout.contacts_activity);
        setContentView(mRootView);

        iv_back = findViewById(R.id.iv_back);
        search_view = findViewById(R.id.search_view);

        rl_group_switch = findViewById(R.id.rl_group_switch);
        tv_current_group_name = findViewById(R.id.tv_current_group_name);
        iv_group_switch_arrow = findViewById(R.id.iv_group_switch_arrow);

        ll_contacts_group = findViewById(R.id.ll_contacts_group);
        tv_select_ok = findViewById(R.id.tv_select_ok);
        tv_select_ok.setVisibility(View.GONE);
        rv_contacts_groups = findViewById(R.id.rv_contacts_groups);

        rv_contacts_members = findViewById(R.id.rv_contacts_members);
        modifySearchViewStyle(search_view);
        /*通讯录班级列表*/
        mGroupAdapter = new UserContactsClazsAdapter(this);
        //此处是为了实现切换班级列表限制最多显示三行半
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {

                View view = recycler.getViewForPosition(0);
                measureChild(view, widthSpec, heightSpec);
                int measuredWidth = View.MeasureSpec.getSize(widthSpec);
                int measuredHeight = view.getMeasuredHeight();
                if (mGroupAdapter.getItemCount() > 3) {
                    setMeasuredDimension(measuredWidth, (int) (measuredHeight * 3.5));
                } else {
                    setMeasuredDimension(measuredWidth, measuredHeight * mGroupAdapter.getItemCount());
                }
            }
        };
        linearLayoutManager.setAutoMeasureEnabled(false);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv_contacts_groups.setHasFixedSize(false);
        rv_contacts_groups.setLayoutManager(linearLayoutManager);
        rv_contacts_groups.setAdapter(mGroupAdapter);
        /*通讯录成员列表*/
        mMemberAdapter = new UserContactsMemberAdapter();
        rv_contacts_members.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv_contacts_members.setAdapter(mMemberAdapter);
        rv_contacts_members.setLoadingMoreEnabled(true);
        rv_contacts_members.setPullRefreshEnabled(true);
    }

    private void initListener() {
        iv_back.setOnClickListener(this);
        rl_group_switch.setOnClickListener(this);
        ll_contacts_group.setOnClickListener(this);
        mRootView.setRetryButtonOnclickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContactsPresenter.doLoadClazsList();
            }
        });
        search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mContactsPresenter.setKeyWords(newText);
                GetClazsListResponse.ClazsInfosBean clazsInfoByPos = mGroupAdapter.getClazsInfoByPos(mGroupAdapter.mCurrentSelectedPosition);
                mContactsPresenter.doGetMembersList(String.valueOf(clazsInfoByPos.getId()));
                return true;
            }
        });
        mGroupAdapter.setItemOnClickListener(new RecyclerViewItemOnClickListener() {
            @Override
            public void onItemClicked(View view, int position) {
                //获取 clazs bean
                GetClazsListResponse.ClazsInfosBean info = mGroupAdapter.getClazsInfoByPos(position);
                //获取 clazs id
                mContactsPresenter.doGetMembersList(String.valueOf(info.getId()));
                showCurrentContactsGroupName(info.getClazsName());
                closeGroupListWindow();
            }
        });
        rv_contacts_members.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                GetClazsListResponse.ClazsInfosBean clazsInfoByPos = mGroupAdapter.getClazsInfoByPos(mGroupAdapter.mCurrentSelectedPosition);
                mContactsPresenter.doGetMembersList(String.valueOf(clazsInfoByPos.getId()));
            }

            @Override
            public void onLoadMore() {
                GetClazsListResponse.ClazsInfosBean clazsInfoByPos = mGroupAdapter.getClazsInfoByPos(mGroupAdapter.mCurrentSelectedPosition);
                mContactsPresenter.doLoadMembersList(String.valueOf(clazsInfoByPos.getId()), mMemberAdapter.getStudentList().size()-1, 40);

            }
        });
        mMemberAdapter.setRecyclerViewItemOnClickListener(new RecyclerViewItemOnClickListener() {
            @Override
            public void onItemClicked(View view, int position) {
                hideSoftInput();
                //事件统计 点击头像
                EventUpdate.onClickMsgContactImageEvent(UserContactsActivity.this);
                GetContactMembersResponse_new.AdressBookPeople adressBookPeople = mMemberAdapter.getDataList().get(position);
                // TODO: 2018/10/9  跳转到 成员详情页
                Bundle bundle = new Bundle();
                bundle.putSerializable("data", adressBookPeople);
                GetClazsListResponse.ClazsInfosBean clazsInfoByPos = mGroupAdapter.getClazsInfoByPos(mGroupAdapter.mCurrentSelectedPosition);
                bundle.putString("topicId",clazsInfoByPos.getTopicId());
                TopicMemberInfoActivity.incoke(UserContactsActivity.this, bundle);
                //检查数据库
//                final ImMember_new memberInfo = mMemberAdapter.getDatas().get(position).getMemberInfo();
//                final ContactsGroupBean contactsGroupBean = mContactsPresenter.getGroupsBeans().get(mContactsPresenter.getCurrentGroupIndex());
//                mContactsPresenter.checkMemberDbInfo(contactsGroupBean, memberInfo);
//
//                ImMsgListActivity.invoke(UserContactsActivity.this,
//                        memberInfo.imId,
//                        memberInfo.memberName,
//                        memberInfo.avatar,
//                        mContactsPresenter.getCurrentGroupId(),
//                        mContactsPresenter.getCurrentGroupName(), ImMsgListActivity.REQUEST_CODE_MEMBERID);
            }
        });

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_back) {
            finish();
        } else if (v.getId() == R.id.rl_group_switch) {
            hideSoftInput();
            if (ll_contacts_group.getVisibility() != View.VISIBLE) {
                openGroupListWindow();
            } else {
                closeGroupListWindow();
            }
        } else if (v.getId() == R.id.ll_contacts_group) {
            if (ll_contacts_group.getVisibility() == View.VISIBLE) {
                hideSoftInput();
                closeGroupListWindow();
            }
        }
    }

    @Override
    public void showLoading() {
        mRootView.showLoadingView();
    }

    @Override
    public void hideLoading() {
        mRootView.hiddenLoadingView();
    }

    @Override
    public void showNetError() {
        mRootView.showNetErrorView();
    }

    @Override
    public void showNoDataError() {
        mRootView.showOtherErrorView("暂无联系人");
    }

    @Override
    public void showOtherError(String error) {
        mRootView.showOtherErrorView(error);
    }

    @Override
    public void hideError() {
        mRootView.hiddenNetErrorView();
        mRootView.hiddenOtherErrorView();
    }

    @Override
    public void showCurrentContactsGroupName(String groupName) {
        tv_current_group_name.setText(groupName);
    }

    @Override
    public void showContactsMembersList(GetContactMembersResponse_new.AdressData memberBeans) {
        rv_contacts_members.refreshComplete();
        mMemberAdapter.setDataList(memberBeans.masters,memberBeans.students.elements);
        mMemberAdapter.notifyDataSetChanged();
    }

    @Override
    public void addLoadMember(GetContactMembersResponse_new.AdressData memberBeans) {
        rv_contacts_members.loadMoreComplete();
        mMemberAdapter.addStudentList(memberBeans.students.elements);
        mMemberAdapter.notifyDataSetChanged();
    }

    @Override
    public void showContactsGroupsList(int selectedPosition, List groupBeans) {
        mGroupAdapter.setDatas(groupBeans);
        mGroupAdapter.setSelectedPosition(selectedPosition);
        mGroupAdapter.notifyDataSetChanged();
        GetClazsListResponse.ClazsInfosBean clazsInfoByPos = mGroupAdapter.getClazsInfoByPos(selectedPosition);
        showCurrentContactsGroupName(clazsInfoByPos.getClazsName());
        mContactsPresenter.doGetMembersList(String.valueOf(clazsInfoByPos.getId()));
    }


    /**
     * 展开班级列表
     */
    private void openGroupListWindow() {
        iv_group_switch_arrow.setRotation(180);
//        mContactsPresenter.loadGroupsList();
        rv_contacts_groups.clearAnimation();
        ll_contacts_group.setVisibility(View.VISIBLE);
        TranslateAnimation mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        mShowAction.setDuration(200);
        rv_contacts_groups.startAnimation(mShowAction);
    }

    /**
     * 收起班级列表
     */
    private void closeGroupListWindow() {
        iv_group_switch_arrow.setRotation(0);
        rv_contacts_groups.clearAnimation();
        TranslateAnimation mHiddenAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f);
        mHiddenAction.setDuration(200);
        mHiddenAction.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ll_contacts_group.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rv_contacts_groups.startAnimation(mHiddenAction);
    }


    /**
     * 关闭软键盘
     */
    private void hideSoftInput() {
        search_view.clearFocus();
        InputMethodUtil.closeInputMethod(this, mRootView);
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

}
