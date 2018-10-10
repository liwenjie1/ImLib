package com.yanxiu.im.business.contacts.interfaces.impls;

import android.text.TextUtils;
import android.util.Log;

import com.yanxiu.im.Constants;
import com.yanxiu.im.business.contacts.interfaces.UserContactsContract;
import com.yanxiu.im.net.GetClazsListRequest;
import com.yanxiu.im.net.GetClazsListResponse;
import com.yanxiu.im.net.GetContactMembersRequest_new;
import com.yanxiu.im.net.GetContactMembersResponse_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;
import com.yanxiu.lib.yx_basic_library.util.YXToastUtil;

import okhttp3.Request;

public class UserContactsPresenter implements UserContactsContract.IPresenter {
    private String mKeyWords = "";
    private UserContactsContract.IView mIView;
    private final int PAGE_SIZE = 20;

    private int studentOffset = 0;

    private final String TAG = getClass().getSimpleName();

    public UserContactsPresenter(UserContactsContract.IView IView) {
        mIView = IView;
    }

    @Override
    public void doLoadClazsList() {
        loadClazsList();
    }

    private void loadClazsList() {

        GetClazsListRequest getSudentClazsesRequest = new GetClazsListRequest();
        getSudentClazsesRequest.imToken = null;
        getSudentClazsesRequest.reqId = null;
        getSudentClazsesRequest.token = Constants.token;
        getSudentClazsesRequest.bizSource = null;
        mIView.showLoading();
        getSudentClazsesRequest.startRequest(GetClazsListResponse.class, new IYXHttpCallback<GetClazsListResponse>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetClazsListResponse ret) {
                mIView.hideError();
                mIView.hideLoading();
                if (ret != null && ret.code == 0) {
                    if (ret.getData() != null && ret.getData().getClazsInfos() != null && ret.getData().getClazsInfos().size() > 0) {
                        mIView.showContactsGroupsList(0, ret.getData().getClazsInfos());
                    } else {
                        if (!TextUtils.isEmpty(ret.message)) {
                            mIView.showOtherError(ret.message);
                        } else {
                            mIView.showOtherError("请求失败");
                        }
                    }
                } else {
                    if (!TextUtils.isEmpty(ret.message)) {
                        mIView.showOtherError(ret.message);
                    } else {
                        mIView.showOtherError("请求失败");
                    }
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                mIView.hideLoading();
                mIView.hideError();
                mIView.showNetError();
            }
        });
    }

    @Override
    public void doLoadMembersList(String clazsId, final int offset, int limit) {
        GetContactMembersRequest_new request = new GetContactMembersRequest_new();
        request.clazsId = clazsId;
        request.offset = String.valueOf(studentOffset);
        request.pageSize = String.valueOf(PAGE_SIZE);
        request.imToken = null;
        request.reqId = null;
        request.bizSource = null;
        request.token = Constants.token;
        request.keyWords = mKeyWords;
        request.startRequest(GetContactMembersResponse_new.class, new IYXHttpCallback<GetContactMembersResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetContactMembersResponse_new ret) {
                Log.i(TAG, "onSuccess: ");
                if (ret != null && ret.code == 0 && ret.data != null) {
                    //记录 offset
                    studentOffset = ret.data.students.offset + ret.data.students.pageSize;
                    mIView.addLoadMember(ret.data);
                } else {
                    if (ret != null) {
                        if (!TextUtils.isEmpty(ret.message)) {
                            YXToastUtil.showToast(ret.message);
                        }else {
                            YXToastUtil.showToast("请求失败");
                        }
                        mIView.complet();
                    }
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                Log.i(TAG, "onFail: ");
                if (!TextUtils.isEmpty(error.getMessage())) {
                    YXToastUtil.showToast(error.getMessage());
                }else {
                    YXToastUtil.showToast("请求失败");
                }
                mIView.complet();
            }
        });


    }

    public void setKeyWords(String keyWords) {
        mKeyWords = keyWords;
    }

    @Override
    public void doGetMembersList(String clazsId) {
        GetContactMembersRequest_new request = new GetContactMembersRequest_new();
        request.clazsId = clazsId;
        request.offset = String.valueOf(0);
        request.pageSize = String.valueOf(PAGE_SIZE);
        request.imToken = null;
        request.reqId = null;
        request.bizSource = null;
        request.token = Constants.token;
        request.keyWords = mKeyWords;
        request.startRequest(GetContactMembersResponse_new.class, new IYXHttpCallback<GetContactMembersResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetContactMembersResponse_new ret) {
                Log.i(TAG, "onSuccess: ");
                if (ret != null && ret.code == 0 && ret.data != null) {
                    studentOffset = ret.data.students.offset + ret.data.students.pageSize;
                    mIView.showContactsMembersList(ret.data);
                } else {
                    if (ret != null) {
                        if (!TextUtils.isEmpty(ret.message)) {
                            YXToastUtil.showToast(ret.message);
                        }else {
                            YXToastUtil.showToast("请求失败");
                        }
                        mIView.complet();
                    }
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                Log.i(TAG, "onFail: ");
                if (!TextUtils.isEmpty(error.getMessage())) {
                    YXToastUtil.showToast(error.getMessage());
                }else {
                    YXToastUtil.showToast("请求失败");
                }
                mIView.complet();
            }
        });

    }
}
