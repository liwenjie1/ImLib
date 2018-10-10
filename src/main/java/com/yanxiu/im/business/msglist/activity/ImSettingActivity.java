package com.yanxiu.im.business.msglist.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.contacts.activity.TopicMemberListActivity;
import com.yanxiu.im.business.msglist.interfaces.ImSettingContract;
import com.yanxiu.im.business.msglist.interfaces.impls.ImSettingPresetner;
import com.yanxiu.im.business.view.ImSettingItemView;
import com.yanxiu.im.business.view.ImSwitchButton;
import com.yanxiu.im.business.view.ImTitleLayout;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.lib.yx_basic_library.util.YXToastUtil;

import java.util.List;

public class ImSettingActivity extends ImBaseActivity implements ImTitleLayout.TitlebarActionClickListener, ImSettingContract.IView {

    private final String TAG = getClass().getSimpleName();

    public static void invoke(Activity activity, long topicId, int requestCode) {
        Intent intent = new Intent(activity, ImSettingActivity.class);
        intent.putExtra("topicId", topicId);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void invoke(Activity activity, String memberName, String memberAvaral, long memberId, String topicName, int requestCode) {
        Intent intent = new Intent(activity, ImSettingActivity.class);
        intent.putExtra("topicId", -1);
        intent.putExtra("memberId", memberId);
        intent.putExtra("memberName", memberName);
        intent.putExtra("memberAvaral", memberAvaral);
        intent.putExtra("topicName", topicName);
        activity.startActivityForResult(intent, requestCode);
    }


    private TopicItemBean currentTopic;
    private ImTitleLayout mImTitleLayout;
    private ImSettingItemView mImNoticeSettingItem;
    private ImSettingItemView mImTalkSettingItem;

    private ImSettingPresetner mImSettingPresenter;

    private TextView im_setting_activity_classname_tv;


    /*私聊 成员信息 */
    private LinearLayout im_setting_private_info_layout;
    private LinearLayout im_setting_group_info_layout;

    private TextView im_member_from_textview;

    private LinearLayout im_setting_member_list_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.im_setting_activity);
        mImSettingPresenter = new ImSettingPresetner(this);
        initView();
        initListener();
        initData();
    }


    protected void initView() {
        im_setting_activity_classname_tv = findViewById(R.id.im_setting_activity_classname_tv);
        mImTitleLayout = findViewById(R.id.im_setting_title_layout);
        mImNoticeSettingItem = findViewById(R.id.im_notice_setting);
        mImTalkSettingItem = findViewById(R.id.im_talk_setting);

        im_setting_private_info_layout = findViewById(R.id.ll_im_setting_private_topic_info);
        im_setting_group_info_layout = findViewById(R.id.ll_group_info);

        im_member_from_textview = findViewById(R.id.im_setting_member_from);

        im_setting_member_list_layout = findViewById(R.id.im_members_layout);
    }


    protected void initData() {
        mImTitleLayout.setTitle("聊聊设置");
        long topicId = getIntent().getLongExtra("topicId", -1);
        if (!Constants.showTopicSilent) {
            mImTalkSettingItem.setVisibility(View.GONE);
        } else {
            boolean isManagerMember = mImSettingPresenter.checkCurrentUserRole(topicId);
            mImTalkSettingItem.setVisibility(isManagerMember ? View.VISIBLE : View.GONE);
        }

        if (topicId == -1) {//预设 数据
            String memberName = getIntent().getStringExtra("memberName");
            String topicName = getIntent().getStringExtra("topicName");
            String url = getIntent().getStringExtra("memberAvaral");
            Log.i(TAG, "setPrivateTopicInfo: ");
            im_setting_group_info_layout.setVisibility(View.GONE);
            im_setting_private_info_layout.setVisibility(View.VISIBLE);
            im_setting_member_list_layout.setVisibility(View.GONE);
            //设置私聊 对象信息
            ImageView memberAvaral = findViewById(R.id.im_setting_member_avaral);
            Glide.with(this)
                    .load(url)
                    .dontAnimate()
                    .dontTransform()
                    .placeholder(R.drawable.im_chat_default)
                    .into(memberAvaral);

            TextView memberNameTv = findViewById(R.id.im_setting_member_name);
            memberNameTv.setText(memberName + "");
            //私聊没有 禁言功能
            mImTalkSettingItem.setVisibility(View.GONE);
            //获取  来自
            im_member_from_textview.setText(topicName + "");
        }

        mImSettingPresenter.doGetTopicInfo(topicId);
    }

    protected void initListener() {
        mImTitleLayout.setmTitlebarActionClickListener(this);
        // 推送设置 按钮
        mImNoticeSettingItem.setOnSwitchCheckedChangedListener(new ImSwitchButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(ImSwitchButton view, boolean isChecked) {
                if (currentTopic != null) {
                    currentTopic.setBlockNotice(isChecked);
                    long topicId = getIntent().getLongExtra("topicId", -1);
                    mImSettingPresenter.dosetNotice(topicId, isChecked);
                } else {
                    YXToastUtil.showToast("会话成员不存在");
                }
            }
        });
        // 禁言设置按钮
        mImTalkSettingItem.setOnSwitchCheckedChangedListener(new ImSwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ImSwitchButton view, boolean isChecked) {
                if (currentTopic != null) {
                    currentTopic.setSilence(isChecked);
                    long topicId = getIntent().getLongExtra("topicId", -1);
                    mImSettingPresenter.dosetSilent(topicId, isChecked);
                } else {
                    YXToastUtil.showToast("会话成员不存在");
                }
            }
        });

        im_setting_member_list_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到  群成员列表界面
                if (currentTopic != null) {
                    TopicMemberListActivity.invoke(ImSettingActivity.this, currentTopic.getTopicId());
                }
            }
        });

    }


    @Override
    public void onLeftComponentClicked() {
        onBackPressed();
    }

    @Override
    public void onRightComponpentClicked() {

    }

    @Override
    public void onSetSilent(boolean silent) {
        Toast.makeText(this, silent ? "已经设置禁言" : "已经取消禁言", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSetNotice(boolean notice) {
        Toast.makeText(this, notice ? "已经设置免打扰" : "已经取消免打扰", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTopicFound(TopicItemBean topicBean) {
        Log.i(TAG, "onTopicFound: ");
        currentTopic = topicBean;
        im_setting_activity_classname_tv.setText(topicBean.getGroup());

        mImNoticeSettingItem.setSwitchBtnChecked(currentTopic.isBlockNotice());
        mImTalkSettingItem.setSwitchBtnChecked(currentTopic.isSilence());
        //如果是群聊
        if (TextUtils.equals("2", topicBean.getType())) {
            setGroupTopicInfo(topicBean);
        } else {
            setPrivateTopicInfo(topicBean);
        }
    }

    @Override
    public void onFromTopicFound(TopicItemBean fromTopicBean) {
        Log.i(TAG, "onFromTopicFound: ");
        if (fromTopicBean != null) {
            im_member_from_textview.setText(fromTopicBean.getName() + "");
        }
    }

    /**
     * 设置私聊 内容显示
     */
    private void setPrivateTopicInfo(TopicItemBean topicBean) {
        Log.i(TAG, "setPrivateTopicInfo: ");
        im_setting_group_info_layout.setVisibility(View.GONE);
        im_setting_private_info_layout.setVisibility(View.VISIBLE);
        //如果是私聊  隐藏 群成员 item
        im_setting_member_list_layout.setVisibility(View.GONE);
        //设置私聊 对象信息
        for (DbMember dbMember : topicBean.getMembers()) {
            if (dbMember.getImId() != Constants.imId) {
                ImageView memberAvaral = findViewById(R.id.im_setting_member_avaral);
                Glide.with(this)
                        .load(dbMember.getAvatar())
                        .dontAnimate()
                        .dontTransform()
                        .placeholder(R.drawable.im_chat_default)
                        .into(memberAvaral);

                TextView memberName = findViewById(R.id.im_setting_member_name);
                memberName.setText(dbMember.getName() + "");
                break;
            }
        }
        //私聊没有 禁言功能
        mImTalkSettingItem.setVisibility(View.GONE);

        //获取  来自

        findViewById(R.id.im_from_layout).setVisibility(TextUtils.isEmpty(topicBean.getGroup()) ? View.GONE : View.VISIBLE);
        im_member_from_textview.setText(topicBean.getGroup() + "");
    }

    /**
     * 设置群聊内容显示
     */
    private void setGroupTopicInfo(TopicItemBean topicBean) {
        Log.i(TAG, "setGroupTopicInfo: ");
        im_setting_group_info_layout.setVisibility(View.VISIBLE);
        im_setting_private_info_layout.setVisibility(View.GONE);
        //设置 群聊信息 名称
        TextView groupName = findViewById(R.id.im_setting_activity_classname_tv);
        groupName.setText(topicBean.getGroup() + "");
        //如果 学员端 的群聊  显示 群成员 item
        if (Constants.APP_TYPE == Constants.APP_TYPE_STUDENT) {
            im_setting_member_list_layout.setVisibility(View.VISIBLE);
        }
        //判断当前 im 是否是 topic 的管理员
        mImTalkSettingItem.setVisibility(View.GONE);
        final List<Long> managers = topicBean.getManagers();
        if (managers != null) {
            for (Long id : managers) {
                if (id == Constants.imId) {
                    //根据 topic 禁言字段显示 toggle禁言状态
                    mImTalkSettingItem.setVisibility(View.VISIBLE);
                    break;
                }
            }
        }
        //外 不设置是否显示 禁言功能
        mImTalkSettingItem.setVisibility(Constants.showTopicSilent ? View.VISIBLE : View.GONE);

    }

}
