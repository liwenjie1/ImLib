package com.yanxiu.im.business.msglist.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.yanxiu.im.business.msglist.interfaces.ImSettingContract;
import com.yanxiu.im.business.msglist.interfaces.impls.ImSettingPresetner;
import com.yanxiu.im.business.view.ImSettingItemView;
import com.yanxiu.im.business.view.ImSwitchButton;
import com.yanxiu.im.business.view.ImTitleLayout;
import com.yanxiu.im.db.DbMember;

public class ImSetingActivity extends ImBaseActivity implements ImTitleLayout.TitlebarActionClickListener, ImSettingContract.IView {

    private final String TAG = getClass().getSimpleName();

    public static void invoke(Activity activity, long topicId) {
        Intent intent = new Intent(activity, ImSetingActivity.class);
        intent.putExtra("topicId", topicId);
        activity.startActivityForResult(intent, 0);
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
                }
                long topicId = getIntent().getLongExtra("topicId", -1);
                mImSettingPresenter.dosetNotice(topicId, isChecked);
            }
        });
        // 禁言设置按钮
        mImTalkSettingItem.setOnSwitchCheckedChangedListener(new ImSwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ImSwitchButton view, boolean isChecked) {
                if (currentTopic != null) {
                    currentTopic.setSilence(isChecked);
                }
                long topicId = getIntent().getLongExtra("topicId", -1);
                mImSettingPresenter.dosetSilent(topicId, isChecked);
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
        currentTopic = topicBean;
        im_setting_activity_classname_tv.setText(topicBean.getGroup());

        mImNoticeSettingItem.setSwitchBtnChecked(mImSettingPresenter.getNoticeSetting());
        mImTalkSettingItem.setSwitchBtnChecked(mImSettingPresenter.getSilentSetting());
        //如果是群聊
        if (TextUtils.equals("2", topicBean.getType())) {
            im_setting_group_info_layout.setVisibility(View.VISIBLE);
            im_setting_private_info_layout.setVisibility(View.GONE);
            //设置 群聊信息 名称
            TextView groupName = findViewById(R.id.im_setting_activity_classname_tv);
            groupName.setText(topicBean.getGroup() + "");
            //根据 topic 禁言字段显示 toggle禁言状态
            //判断当前 im 是否是 topic 的管理员
            mImTalkSettingItem.setVisibility(View.GONE);

        } else {
            im_setting_group_info_layout.setVisibility(View.GONE);
            im_setting_private_info_layout.setVisibility(View.VISIBLE);
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
            //私聊不存在 禁言
            mImTalkSettingItem.setVisibility(View.GONE);
        }
        //设置是否显示学院禁言功能

        //设置 toggle state
        mImNoticeSettingItem.setSwitchBtnChecked(currentTopic.isBlockNotice());
        mImTalkSettingItem.setSwitchBtnChecked(currentTopic.isSilence());
    }
}
