package com.yanxiu.im.business.contacts.interfaces.impls;

import android.util.Log;

import com.yanxiu.im.Constants;
import com.yanxiu.im.business.contacts.interfaces.TopicMemberListContract;
import com.yanxiu.im.net.GetContactMembersRequest_new;
import com.yanxiu.im.net.GetContactMembersResponse_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.ArrayList;

import okhttp3.Request;

public class ContactsMemberListPresenter implements TopicMemberListContract.IPresenter {

    private final String TAG = getClass().getSimpleName();
    private TopicMemberListContract.IView mIView;

    public ContactsMemberListPresenter(TopicMemberListContract.IView IView) {
        mIView = IView;
    }

    @Override
    public void doGetMemberList(String topicId) {
        GetContactMembersRequest_new request_new = new GetContactMembersRequest_new();
        request_new.topicId = topicId;
        request_new.token= Constants.token;
        request_new.reqId=null;
        request_new.startRequest(GetContactMembersResponse_new.class, new IYXHttpCallback<GetContactMembersResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetContactMembersResponse_new ret) {
                Log.i(TAG, "onSuccess: ");
                if (ret.code == 0) {
                    mIView.onGetMemberList(new ArrayList());
                } else {
                    mIView.onException(ret.message);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                Log.i(TAG, "onFail: ");
                mIView.onException(error != null ? error.getMessage() : "请求失败");

            }
        });
    }

    @Override
    public void doLoadMoreMemberList(String topicId, int offset, int limit) {
        GetContactMembersRequest_new request_new = new GetContactMembersRequest_new();
        request_new.topicId = topicId;
        request_new.startRequest(GetContactMembersResponse_new.class, new IYXHttpCallback<GetContactMembersResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetContactMembersResponse_new ret) {
                Log.i(TAG, "onSuccess: ");
                if (ret.code == 0) {
                    mIView.onLoadMemberList(new ArrayList());
                } else {
                    mIView.onException(ret.message);
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                Log.i(TAG, "onFail: ");
                mIView.onException(error != null ? error.getMessage() : "请求失败");

            }
        });
    }
}
