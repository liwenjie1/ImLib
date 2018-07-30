package com.yanxiu.im.business.msglist.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.test.yanxiu.common_base.utils.EscapeCharacterUtils;
import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.msglist.interfaces.ImSettingContract;
import com.yanxiu.im.business.msglist.interfaces.impls.ImSettingPresetner;
import com.yanxiu.im.business.view.ImSettingItemView;
import com.yanxiu.im.business.view.ImSwitchButton;
import com.yanxiu.im.business.view.ImTitleLayout;

public class ImSetingActivity extends ImBaseActivity implements ImTitleLayout.TitlebarActionClickListener,ImSettingContract.IView{

    private final String TAG=getClass().getSimpleName();

    public static void invoke(Activity activity,long topicId){
        Intent intent=new Intent(activity,ImSetingActivity.class);
        intent.putExtra("topicId",topicId);
        activity.startActivityForResult(intent,0);
    }




    private ImTitleLayout mImTitleLayout;
    private ImSettingItemView mImNoticeSettingItem;
    private ImSettingItemView mImTalkSettingItem;

    private ImSettingPresetner mImSettingPresenter;

    private TextView im_setting_activity_classname_tv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.im_setting_activity);
        mImSettingPresenter=new ImSettingPresetner(this);
        initView();
        initListener();
        initData();
    }


    protected void initView(){
        im_setting_activity_classname_tv=findViewById(R.id.im_setting_activity_classname_tv);
        mImTitleLayout=findViewById(R.id.im_setting_title_layout);
        mImNoticeSettingItem=findViewById(R.id.im_notice_setting);
        mImTalkSettingItem=findViewById(R.id.im_talk_setting);
    }


    protected void initData(){
        mImTitleLayout.setTitle("聊聊设置");
        long topicId=getIntent().getLongExtra("topicId",-1);
        mImSettingPresenter.doGetTopicInfo(topicId);
        //学员端隐藏 禁言功能
        if (Constants.APP_TYPE==Constants.APP_TYPE_STUDENT) {

            mImTalkSettingItem.setVisibility(View.GONE);
        }else if (Constants.APP_TYPE==Constants.APP_TYPE_ADMIN){
            //管理端登录 需要判断当前用户在当前topic中的角色 如果是班主任才开启显示 禁言功能
            boolean isManagerMember= mImSettingPresenter.checkCurrentUserRole(topicId);
            mImTalkSettingItem.setVisibility(isManagerMember?View.VISIBLE:View.GONE);
        }
    }

    protected void initListener(){
        mImTitleLayout.setmTitlebarActionClickListener(this);
        // 推送设置 按钮
        mImNoticeSettingItem.setOnSwitchCheckedChangedListener(new ImSwitchButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(ImSwitchButton view, boolean isChecked) {
                mImSettingPresenter.dosetNotice(isChecked);
            }
        });
        // 禁言设置按钮
        mImTalkSettingItem.setOnSwitchCheckedChangedListener(new ImSwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ImSwitchButton view, boolean isChecked) {
                mImSettingPresenter.dosetSilent(isChecked);
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
        Toast.makeText(this, silent?"已经设置禁言":"已经取消禁言", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSetNotice(boolean notice) {
        Toast.makeText(this, notice?"已经设置免打扰":"已经取消免打扰", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTopicFound(TopicItemBean topicBean) {
        im_setting_activity_classname_tv.setText(EscapeCharacterUtils.unescape(topicBean.getGroup()));

        mImNoticeSettingItem.setSwitchBtnChecked(mImSettingPresenter.getNoticeSetting());
        mImTalkSettingItem.setSwitchBtnChecked(mImSettingPresenter.getSilentSetting());
    }
}
