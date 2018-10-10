package com.yanxiu.im.business.contacts.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.test.yanxiu.common_base.ui.PublicLoadLayout;
import com.test.yanxiu.common_base.utils.areas.FileUtils;
import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.business.contacts.areas.AreaBean;
import com.yanxiu.im.business.msglist.activity.ImMsgListActivity;
import com.yanxiu.im.business.view.ImTitleLayout;
import com.yanxiu.im.net.GetContactMemberInfoRequest;
import com.yanxiu.im.net.GetContactMemberInfoResponse;
import com.yanxiu.im.net.GetContactMembersResponse_new;
import com.yanxiu.im.net.GetImIdByUserIdResponse;
import com.yanxiu.im.net.GetImIdByUseridRequest;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;
import com.yanxiu.lib.yx_basic_library.util.YXToastUtil;

import java.util.ArrayList;
import java.util.UUID;

import okhttp3.Request;

public class TopicMemberInfoActivity extends ImBaseActivity {


    private ImTitleLayout mImTitleLayout;

    private GetContactMembersResponse_new.AdressBookPeople mPeople;


    private static final String KEY_USERID = "key_userid";
    private static final String KEY_ISTEACHER = "key_isteacher";

    private Context mContext;
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

    private UUID mGetImIdByUseridRequest;
    private long mImMemberId;//im member id
    private String mTopicGroup;

    private PublicLoadLayout mPublicLoadLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPublicLoadLayout = new PublicLoadLayout(this);
        mPublicLoadLayout.setContentView(R.layout.adressbook_activity_personaldetails_hubei);
        setContentView(mPublicLoadLayout);
        mImTitleLayout = findViewById(R.id.im_title_layout);

        Bundle data = getIntent().getBundleExtra("data");
        if (data != null) {
            mPeople = (GetContactMembersResponse_new.AdressBookPeople) data.getSerializable("data");
            mTopicGroup = data.getString("topicId");
        }
        initView();
        requestData();

    }

    public static void incoke(Activity activity, Bundle bundle) {
        Intent intent = new Intent(activity, TopicMemberInfoActivity.class);
        if (bundle != null) {
            intent.putExtra("data", bundle);
        }
        activity.startActivity(intent);
    }


    public void setdata(GetContactMembersResponse_new.AdressBookPeople data) {


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


        mImTitleLayout.setTitle("资料详情");
        mImTitleLayout.setmTitlebarActionClickListener(new ImTitleLayout.TitlebarActionClickListener() {
            @Override
            public void onLeftComponentClicked() {
                onBackPressed();
            }

            @Override
            public void onRightComponpentClicked() {

            }
        });
//        if (isTeacher) {
//            ll_is_teacher.setVisibility(View.GONE);
//        } else {
//            ll_is_teacher.setVisibility(View.VISIBLE);
//        }
        if (TextUtils.isEmpty(mTopicGroup)||mPeople.userId==Constants.userId) {
            iv_chat.setVisibility(View.GONE);
        }
        iv_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestImId(true, mPeople.userId);
            }
        });
    }

    /**
     * 根据用户id获取Im id
     */
    private void requestImId(final boolean isFirst, final long userId) {
        mPublicLoadLayout.showLoadingView();
        GetImIdByUseridRequest getImIdRequest = new GetImIdByUseridRequest();
        getImIdRequest.userId = String.valueOf(userId);
        getImIdRequest.imToken = Constants.imToken;

        getImIdRequest.fromGroupTopicId = mTopicGroup;
        getImIdRequest.startRequest(GetImIdByUserIdResponse.class, new IYXHttpCallback<GetImIdByUserIdResponse>() {

            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetImIdByUserIdResponse ret) {
                mPublicLoadLayout.hiddenLoadingView();
                if (ret != null && ret.data != null && ret.code == 0) {
                    mImMemberId = ret.data.memberId;
                    mTopicGroup = ret.data.topic.topicGroup;

                    if (mImMemberId == Constants.imId) {
                        return;
                    }

                    ImMsgListActivity.invoke(TopicMemberInfoActivity.this,
                            mImMemberId,
                            mPeople.realName,
                            mPeople.avatar,
                            -1,
                            mTopicGroup, ImMsgListActivity.REQUEST_CODE_MEMBERID);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                mPublicLoadLayout.hiddenLoadingView();
                YXToastUtil.showToast(error.getMessage());
            }
        });
    }


    /**
     * 个人详情
     */
    private void requestData() {
        mPublicLoadLayout.showLoadingView();
        GetContactMemberInfoRequest mDetailsRequest = new GetContactMemberInfoRequest();
        mDetailsRequest.userId = String.valueOf(mPeople.userId);
        mDetailsRequest.startRequest(GetContactMemberInfoResponse.class, new IYXHttpCallback<GetContactMemberInfoResponse>() {

            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetContactMemberInfoResponse ret) {
                mPublicLoadLayout.hiddenLoadingView();
                if (ret != null && ret.data != null && ret.code == 0) {
                    setPersonalMessage(ret.data);
                } else {
                    mPublicLoadLayout.showOtherErrorView(ret.message);
                }
            }

            @Override
            public void onFail(YXRequestBase request, final Error error) {
                mPublicLoadLayout.hiddenLoadingView();
                mPublicLoadLayout.showNetErrorView();
                mPublicLoadLayout.setRetryButtonOnclickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestData();
                    }
                });
            }
        });
    }

    private void setPersonalMessage(GetContactMemberInfoResponse.PersonalDetailsData message) {
        Glide.with(this).load(message.avatar).asBitmap().placeholder(R.drawable.im_chat_default).centerCrop().into(iv_head_img);
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
        setAreaInfo(message);

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

    public class AreaData {
        public ArrayList<AreaBean> data;

    }
    private void setAreaInfo(GetContactMemberInfoResponse.PersonalDetailsData message) {

        AreaData data = FileUtils.parserJsonFromAssets(AreaData.class, "area.json",TopicMemberInfoActivity.this);
        ArrayList<AreaBean> areaBeans = data.data;

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


}
