package com.yanxiu.im.business.contacts.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.test.yanxiu.common_base.ui.InputMethodUtil;
import com.test.yanxiu.common_base.ui.PublicLoadLayout;
import com.test.yanxiu.common_base.utils.talkingdata.EventUpdate;
import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.ContactsGroupBean;
import com.yanxiu.im.bean.ContactsMemberBean;
import com.yanxiu.im.bean.net_bean.ImMember_new;
import com.yanxiu.im.business.contacts.adapter.ContactsGroupAdapter;
import com.yanxiu.im.business.contacts.adapter.ContactsMemberAdapter;
import com.yanxiu.im.business.contacts.interfaces.ContactsContract;
import com.yanxiu.im.business.contacts.interfaces.impls.ContactsPresenter;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;
import com.yanxiu.im.business.msglist.activity.ImMsgListActivity;

import java.util.List;

/**
 * 通讯录
 * Created by Hu Chao on 18/5/17.
 */
public class ContactsActivity extends ImBaseActivity implements ContactsContract.IView, View.OnClickListener {

    private PublicLoadLayout mRootView;
    private ImageView iv_back;
    private SearchView search_view;
    private RelativeLayout rl_group_switch;
    private TextView tv_current_group_name;
    private ImageView iv_group_switch_arrow;
    private LinearLayout ll_contacts_group;
    private TextView tv_select_ok;

    private RecyclerView rv_contacts_groups;
    private RecyclerView rv_contacts_members;

    //班级列表adapter
    private ContactsGroupAdapter mGroupAdapter;
    //成员列表adapter
    private ContactsMemberAdapter mMemberAdapter;

    private ContactsPresenter mContactsPresenter;


    public static void invoke(Activity activity) {
        Intent intent = new Intent(activity, ContactsActivity.class);
        activity.startActivity(intent);
    }

    public static void invoke(android.support.v4.app.Fragment fragment) {
        Intent intent = new Intent(fragment.getActivity(), ContactsActivity.class);
        fragment.startActivityForResult(intent, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
        initListener();

        mContactsPresenter = new ContactsPresenter(this);
        mContactsPresenter.loadContacts();
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
        mGroupAdapter = new ContactsGroupAdapter();
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
        mMemberAdapter = new ContactsMemberAdapter();
        rv_contacts_members.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv_contacts_members.setAdapter(mMemberAdapter);
    }

    private void initListener() {
        iv_back.setOnClickListener(this);
        rl_group_switch.setOnClickListener(this);
        ll_contacts_group.setOnClickListener(this);
        mRootView.setRetryButtonOnclickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContactsPresenter.loadContacts();
            }
        });
        search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mMemberAdapter.getFilter().filter(newText);
                return true;
            }
        });
        mGroupAdapter.setItemOnClickListener(new RecyclerViewItemOnClickListener() {
            @Override
            public void onItemClicked(View view, int position) {
                mContactsPresenter.loadMembersByPosition(position);
                closeGroupListWindow();
            }
        });
        mMemberAdapter.setItemOnClickListener(new RecyclerViewItemOnClickListener() {
            @Override
            public void onItemClicked(View view, int position) {
                hideSoftInput();
                //事件统计 点击头像
                EventUpdate.onClickMsgContactImageEvent(ContactsActivity.this);
                if (mMemberAdapter.getDatas().get(position).getMemberInfo().imId == Constants.imId) {
                    // 不能给自己发消息
                    return;
                }
                //检查数据库
                final ImMember_new memberInfo = mMemberAdapter.getDatas().get(position).getMemberInfo();
                final ContactsGroupBean contactsGroupBean = mContactsPresenter.getGroupsBeans().get(mContactsPresenter.getCurrentGroupIndex());
                mContactsPresenter.checkMemberDbInfo(contactsGroupBean,memberInfo);

                ImMsgListActivity.invoke(ContactsActivity.this,
                       memberInfo.imId,memberInfo.memberName, mContactsPresenter.getCurrentGroupId(), ImMsgListActivity.REQUEST_CODE_MEMBERID);
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
    public void showContactsGroupsList(int selectedPosition, List<ContactsGroupBean> groupBeans) {
        mGroupAdapter.setDatas(groupBeans);
        mGroupAdapter.setSelectedPosition(selectedPosition);
        mGroupAdapter.notifyDataSetChanged();
    }

    @Override
    public void showContactsMembersList(List<ContactsMemberBean> memberBeans) {
        mMemberAdapter.setDatas(memberBeans);
        String keyword = search_view.getQuery().toString();
        if (keyword != null && !TextUtils.isEmpty(keyword.trim())) {
            mMemberAdapter.getFilter().filter(keyword);
        }
        mMemberAdapter.notifyDataSetChanged();
    }

    /**
     * 展开班级列表
     */
    private void openGroupListWindow() {
        iv_group_switch_arrow.setRotation(180);
        mContactsPresenter.loadGroupsList();
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
