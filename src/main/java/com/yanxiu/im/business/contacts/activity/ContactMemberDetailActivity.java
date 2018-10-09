package com.yanxiu.im.business.contacts.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.test.yanxiu.common_base.ui.PublicLoadLayout;
import com.yanxiu.im.R;
import com.yanxiu.im.business.msglist.activity.ImMsgListActivity;
import com.yanxiu.im.net.GetContactMemberInfoRequest;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;
import com.yanxiu.lib.yx_basic_library.util.YXSystemUtil;
import com.yanxiu.lib.yx_basic_library.util.YXToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Request;

public class ContactMemberDetailActivity extends ImBaseActivity implements View.OnClickListener {
    private static final String KEY_USERID = "key_userid";
    private static final String KEY_ISTEACHER = "key_isteacher";

    private Context mContext;
    private PublicLoadLayout rootView;
    private ImageView mBackView;
    private TextView mTitleView;

    private ImageView iv_head_img;
    private TextView tv_name;
    private ImageView iv_chat;
    private TextView tv_mobile;//手机号
    private TextView tv_sex;
    private TextView tv_stage;
    private TextView tv_subject;

    private TextView tv_province;//省
    private TextView tv_city;//市
    private TextView tv_county;//区
    private TextView tv_school;//学校
    private TextView tv_idCard;//身份证
    private TextView tv_childprojectId;//子项目编号
    private TextView tv_childprojectName;//子项目名称
    private TextView tv_organizer;//承训单位
    private TextView tv_area;//学校所在区域
    private TextView tv_schoolType;//学校类别
    private TextView tv_nation;//民族
    private TextView tv_title;//职称
    private TextView tv_job;//职务
    private TextView tv_recordeducation;//最高学历
    private TextView tv_graduation;//毕业院校
    private TextView tv_professional;//所学专业
    private TextView tv_telephone;//电话
    private TextView tv_email;//电子邮箱

    private View ll_is_teacher;//是老师的话，这个view不显示
    private TextView mRateView;
    private ImageView mSginRecordView;

    private boolean isTeacher = false;
    private String mUserId;
    private String mUserName;

    private GetContactMemberInfoRequest mDetailsRequest;
//    private PersonalDetailsSignRequest mSignRequest;
//    private UUID mGetImIdByUseridRequest;
//    private long mImMemberId;//im member id
//    private String mTopicGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        EventBus.getDefault().register(mContext);
        rootView = new PublicLoadLayout(mContext);
        rootView.setContentView(R.layout.adressbook_activity_personaldetails_hubei);
        setContentView(rootView);
        initData();
        initView();
        initListener();
        requestData();
        checkCurrentClassImEnable();
        requestImId(true);
    }

    /**
     * 根据用户id获取Im id
     */
    private void requestImId(final boolean isFirst) {
        GetImIdByUseridRequest getImIdRequest = new GetImIdByUseridRequest();
        getImIdRequest.userId = mUserId;
        getImIdRequest.imToken = UserInfoManager.getInstance().getUserInfo().getImTokenInfo().imToken;
        getImIdRequest.bizSource = "22";
        getImIdRequest.fromGroupTopicId = UserInfoManager.getInstance().getCurrentClazsInfo().getTopicId() + "";
        mGetImIdByUseridRequest = getImIdRequest.startRequest(GetImIdByUserIdResponse.class, new IYXHttpCallback<GetImIdByUserIdResponse>() {

            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetImIdByUserIdResponse ret) {
                mGetImIdByUseridRequest = null;
                if (ret != null && ret.data != null && ret.getCode() == 0) {
                    mImMemberId = ret.data.memberId;
                    mTopicGroup = ret.data.topic.topicGroup;
                    if (!isFirst) {
                        ImMsgListActivity.invoke(PersonalDetailsActivity_Hubei.this,
                                mImMemberId,
                                tv_name.getText().toString(),
                                "",
                                UserInfoManager.getInstance().getCurrentClazsInfo().getTopicId(),
                                mTopicGroup, ImMsgListActivity.REQUEST_CODE_MEMBERID);
                    }
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                mGetImIdByUseridRequest = null;
                YXToastUtil.showToast(error.getMessage());
            }
        });
    }

    /**
     * 检查当前班级是否在当前用户管理的班级内
     * 请求项目班级信息
     */
    private void checkCurrentClassImEnable() {
//        GetClazzListRequest getClazzListRequest = new GetClazzListRequest();
//        getClazzListRequest.platId = UserInfoManager.getInstance().getPlatformsId();
//        getClazzListRequest.startRequest(GetClazzListResponse.class, new IYXHttpCallback<GetClazzListResponse>() {
//
//            @Override
//            public void onRequestCreated(Request request) {
//
//            }
//
//            @Override
//            public void onSuccess(YXRequestBase request, GetClazzListResponse ret) {
//                if (ret != null && ret.getCode() == 0 && ret.getData().getClazsInfos() != null && ret.getData().getClazsInfos().size() > 0) {
//                    List<ClazsInfoBean> clazsInfoBeanList = ret.getData().getClazsInfos();
//                    ClazsInfoBean currentClassInfo = UserInfoManager.getInstance().getCurrentClazsInfo();
//                    for (int i = 0; i < clazsInfoBeanList.size(); i++) {
//                        if (currentClassInfo != null) {
//                            if (currentClassInfo.getId() == clazsInfoBeanList.get(i).getId()) {
//                                iv_chat.setVisibility(View.VISIBLE);
//                            }
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onFail(YXRequestBase request, Error error) {
//
//            }
//        });
    }

    private void initData() {
        mUserId = getIntent().getStringExtra(KEY_USERID);
        isTeacher = getIntent().getBooleanExtra(KEY_ISTEACHER, false);
    }

    private void initView() {
        iv_head_img = findViewById(R.id.iv_head_img);
        tv_name = findViewById(R.id.tv_name);
        iv_chat = findViewById(R.id.iv_chat);
        tv_mobile = findViewById(R.id.tv_mobile);
        tv_sex = findViewById(R.id.tv_sex);
        tv_stage = findViewById(R.id.tv_stage);
        tv_subject = findViewById(R.id.tv_subject);
        tv_province = findViewById(R.id.tv_province);
        tv_city = findViewById(R.id.tv_city);
        tv_county = findViewById(R.id.tv_county);
        tv_school = findViewById(R.id.tv_school);
        tv_idCard = findViewById(R.id.tv_idCard);
        tv_childprojectId = findViewById(R.id.tv_childprojectId);
        tv_childprojectName = findViewById(R.id.tv_childprojectName);
        tv_organizer = findViewById(R.id.tv_organizer);
        tv_area = findViewById(R.id.tv_area);
        tv_schoolType = findViewById(R.id.tv_schoolType);
        tv_nation = findViewById(R.id.tv_nation);
        tv_title = findViewById(R.id.tv_title);
        tv_job = findViewById(R.id.tv_job);
        tv_recordeducation = findViewById(R.id.tv_recordeducation);
        tv_graduation = findViewById(R.id.tv_graduation);
        tv_professional = findViewById(R.id.tv_professional);
        tv_telephone = findViewById(R.id.tv_telephone);
        tv_email = findViewById(R.id.tv_email);


        ll_is_teacher = findViewById(R.id.ll_is_teacher);
        mRateView = findViewById(R.id.tv_rate);
        mSginRecordView = findViewById(R.id.iv_sign_record);


        mTitleView.setText("资料详情");
        if (isTeacher) {
            ll_is_teacher.setVisibility(View.GONE);
        } else {
            ll_is_teacher.setVisibility(View.VISIBLE);
        }
    }

    private void initListener() {
        mBackView.setOnClickListener(this);
        mSginRecordView.setOnClickListener(this);
        rootView.setRetryButtonOnclickListener(this);
        iv_chat.setOnClickListener(this);
        tv_mobile.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_layout_left_img:
                this.finish();
                break;
            case R.id.retry_button:
                requestData();
                break;
            case R.id.iv_sign_record:
                EventUpdate.onSeeStudentCheckKinRecord(mContext);
                SignRecordActivity.LuanchActivity(mContext, mUserId, mUserName);
                break;
            case R.id.iv_chat:
                if (mImMemberId > 0) {
                    ImMsgListActivity.invoke(PersonalDetailsActivity_Hubei.this,
                            mImMemberId,
                            tv_name.getText().toString(),
                            "",
                            UserInfoManager.getInstance().getCurrentClazsInfo().getTopicId(), mTopicGroup, ImMsgListActivity.REQUEST_CODE_MEMBERID);
                } else {
                    requestImId(false);
                }
                break;
            case R.id.tv_mobile:
                final CallPhoneDialog callPhoneDialog = new CallPhoneDialog(this);
                callPhoneDialog.setOnConfirmTelClickListener(new CallPhoneDialog.OnConfirmTelClickListener() {
                    @Override
                    public void onOkClick() {
                        String[] perms = {Manifest.permission.CALL_PHONE};
                        requestPermissions(perms, new OnPermissionCallback() {
                            @Override
                            public void onPermissionsGranted(@Nullable List<String> deniedPermissions) {
                                YXSystemUtil.makeCall(PersonalDetailsActivity_Hubei.this, tv_mobile.getText().toString());
                            }

                            @Override
                            public void onPermissionsDenied(@Nullable List<String> deniedPermissions) {
                            }
                        });
                        callPhoneDialog.dismiss();

                    }

                    @Override
                    public void onCancelClick() {
                        callPhoneDialog.dismiss();
                    }
                });
                callPhoneDialog.show();
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SignRecordSuccessEvent bean) {
        if (bean != null) {
            startPersonalDetailsSignRequest();
        }
    }

    /**
     * 签到率
     */
    private void startPersonalDetailsSignRequest() {
        mSignRequest = new PersonalDetailsSignRequest();
        mSignRequest.userId = mUserId;
        mSignRequest.startRequest(PersonalDetailsSignResponse.class, new IYXHttpCallback<PersonalDetailsSignResponse>() {

            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, PersonalDetailsSignResponse ret) {
                rootView.finish();
                if (ret != null && ret.data != null && ret.getCode() == 0) {
                    setPersonalSignMessage(ret.data);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                mSignRequest = null;
                rootView.showNetErrorView();
                YXToastUtil.showToast(error.getMessage());
            }
        });
    }

    /**
     * 个人详情
     */
    private void requestData() {
        rootView.showLoadingView();
        mDetailsRequest = new PersonalDetailsRequest_Hubei();
        mDetailsRequest.userId = mUserId;
        mDetailsRequest.startRequest(PersonalDetailsResponse.class, new IYXHttpCallback<PersonalDetailsResponse>() {

            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, PersonalDetailsResponse ret) {
                if (ret != null && ret.data != null && ret.getCode() == 0) {
                    setPersonalMessage(ret.data);
                    startPersonalDetailsSignRequest();
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                rootView.showNetErrorView();
                YXToastUtil.showToast(error.getMessage());
            }
        });
    }

    private void setPersonalMessage(PersonalDetailsResponse.PersonalDetailsData message) {
        Glide.with(mContext).load(message.avatar).asBitmap().placeholder(R.drawable.classcircle_headimg_small).centerCrop().into(new CornersImageTarget(mContext, iv_head_img, 10));
        tv_mobile.setText(message.mobilePhone);
        tv_name.setText(message.realName);
        mUserName = message.realName;

        if (!TextUtils.isEmpty(message.stageName)) {
            tv_stage.setText(message.stageName);
        } else {
            tv_stage.setText("暂无");
        }
        if (!TextUtils.isEmpty(message.subjectName)) {
            tv_subject.setText(message.subjectName);
        } else {
            tv_subject.setText("暂无");
        }
        String sex;
        switch (message.sex) {
            case 0:
                sex = "女";
                break;
            case 1:
                sex = "男";
                break;
            default:
                sex = "未知";
                break;
        }
        tv_sex.setText(sex);

        if (!TextUtils.isEmpty(message.school)) {
            tv_school.setText(message.school);
        } else {
            tv_school.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.email)) {
            tv_email.setText(message.email);
        } else {
            tv_email.setText("暂无");
        }

        if (message.aui == null)
            return;
        ArrayList<AreaBean> areaBeans = AreaManager.getAreaData();
        AreaBean provinceBean = null;
        AreaBean cityBean = null;
        if (!TextUtils.isEmpty(message.aui.province)) {
            provinceBean = getProvinceBean(areaBeans, message.aui.province);
            if (provinceBean != null) {
                tv_province.setText(provinceBean.getName());
            } else {
                tv_province.setText("暂无");
            }
        } else {
            tv_province.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.city) && provinceBean != null) {
            cityBean = getCityBean(provinceBean, message.aui.city);
            if (cityBean != null) {
                tv_city.setText(cityBean.getName());
            } else {
                tv_city.setText("暂无");
            }

        } else {
            tv_city.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.country) && cityBean != null) {
            AreaBean countyBean = getCountyBean(cityBean, message.aui.country);
            if (countyBean != null) {
                tv_county.setText(countyBean.getName());
            } else {
                tv_county.setText("暂无");
            }
        } else {
            tv_county.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.idCard)) {
            tv_idCard.setText(message.aui.idCard);
        } else {
            tv_idCard.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.area)) {
            tv_area.setText(message.aui.area);
        } else {
            tv_area.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.schoolType)) {
            tv_schoolType.setText(message.aui.schoolType);
        } else {
            tv_schoolType.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.nation)) {
            tv_nation.setText(message.aui.nation);
        } else {
            tv_nation.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.title)) {
            tv_title.setText(message.aui.title);
        } else {
            tv_title.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.recordeducation)) {
            tv_recordeducation.setText(message.aui.recordeducation);
        } else {
            tv_recordeducation.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.graduation)) {
            tv_graduation.setText(message.aui.graduation);
        } else {
            tv_graduation.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.professional)) {
            tv_professional.setText(message.aui.professional);
        } else {
            tv_professional.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.childprojectId)) {
            tv_childprojectId.setText(message.aui.childprojectId);
        } else {
            tv_childprojectId.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.childprojectName)) {
            tv_childprojectName.setText(message.aui.childprojectName);
        } else {
            tv_childprojectName.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.organizer)) {
            tv_organizer.setText(message.aui.organizer);
        } else {
            tv_organizer.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.job)) {
            tv_job.setText(message.aui.job);
        } else {
            tv_job.setText("暂无");
        }

        if (!TextUtils.isEmpty(message.aui.telephone)) {
            tv_telephone.setText(message.aui.telephone);
        } else {
            tv_telephone.setText("暂无");
        }
    }

    private AreaBean getProvinceBean(ArrayList<AreaBean> areaBeans, String provinceId) {
        if (TextUtils.isEmpty(provinceId))
            return null;
        ArrayList<AreaBean> provinceList = areaBeans;
        for (AreaBean provinceBean : provinceList) {
            if (provinceBean.getId().equals(provinceId)) {
                return provinceBean;
            }
        }
        return null;
    }

    private AreaBean getCityBean(AreaBean provinceBean, String cityId) {
        if (TextUtils.isEmpty(cityId) || provinceBean == null)
            return null;
        ArrayList<AreaBean> cityList = provinceBean.getSub();
        for (AreaBean cityBean : cityList) {
            if (cityBean.getId().equals(cityId)) {
                return cityBean;
            }
        }
        return null;
    }

    private AreaBean getCountyBean(AreaBean cityBean, String countyId) {
        if (TextUtils.isEmpty(countyId) || cityBean == null)
            return null;
        ArrayList<AreaBean> countyList = cityBean.getSub();
        for (AreaBean countyBean : countyList) {
            if (countyBean.getId().equals(countyId)) {
                return countyBean;
            }
        }
        return null;
    }

    private void setPersonalSignMessage(PersonalDetailsSignResponse.PersonalDetailsSignData data) {
        mRateView.setText(getPercent(data.userSigninNum, data.totalSigninNum));
    }

    private String getPercent(int y, int z) {
        if (y == 0) {
            return "0%";
        } else {
            String baifenbi;// 接受百分比的值
            double baiy = y * 1.0;
            double baiz = z * 1.0;
            double fen = baiy / baiz;
            // NumberFormat nf = NumberFormat.getPercentInstance(); 注释掉的也是一种方法
            // nf.setMinimumFractionDigits( 2 ); 保留到小数点后几位
            DecimalFormat df1 = new DecimalFormat("##%"); // ##.00%
            // 百分比格式，后面不足2位的用0补齐
            // baifenbi=nf.format(fen);
            baifenbi = df1.format(fen);
            System.out.println(baifenbi);
            return baifenbi;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(mContext);
//        if (mSignRequest != null) {
//            mSignRequest.cancelRequest();
//            mSignRequest = null;
//        }
        if (mDetailsRequest != null) {
            mDetailsRequest.cancelRequest();
            mDetailsRequest = null;
        }

//        if (mGetImIdByUseridRequest != null) {
//            YXRequestBase.cancelRequestWithUUID(mGetImIdByUseridRequest);
//            mGetImIdByUseridRequest = null;
//        }
    }

    public static void invoke(Context context, String userId, boolean isTeacher) {
        Intent intent = new Intent(context, ContactMemberDetailActivity.class);
        intent.putExtra(KEY_USERID, userId);
        intent.putExtra(KEY_ISTEACHER, isTeacher);
        context.startActivity(intent);
    }
}
