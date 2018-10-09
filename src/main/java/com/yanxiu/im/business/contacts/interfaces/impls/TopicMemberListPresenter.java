package com.yanxiu.im.business.contacts.interfaces.impls;

import android.util.Log;

import com.yanxiu.im.Constants;
import com.yanxiu.im.business.contacts.interfaces.TopicMemberListContract;
import com.yanxiu.im.net.GetTopicMemberListRequest;
import com.yanxiu.im.net.GetTopicMemberListResponse;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.ArrayList;

import okhttp3.Request;

public class TopicMemberListPresenter implements TopicMemberListContract.IPresenter {

    private final String TAG = getClass().getSimpleName();
    private TopicMemberListContract.IView mIView;

    public TopicMemberListPresenter(TopicMemberListContract.IView IView) {
        mIView = IView;
    }

    @Override
    public void doGetMemberList(String topicId) {
        GetTopicMemberListRequest request = new GetTopicMemberListRequest();
        request.topicId = topicId;
        request.imToken= Constants.imToken;
        request.startRequest(GetTopicMemberListResponse.class, new IYXHttpCallback<GetTopicMemberListResponse>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetTopicMemberListResponse ret) {
                Log.i(TAG, "onSuccess: ");
                if (ret != null && ret.code == 0) {
                    if (ret.getData() == null//data 为空
                            || ret.getData().getContacts() == null//通讯录为空
                            || ret.getData().getContacts().getGroups() == null//group 为空
                            || ret.getData().getContacts().getGroups().size() == 0) {
                        mIView.onGetMemberList(new ArrayList());
                    } else {
                        mIView.onGetMemberList((ArrayList) ret.getData().getContacts().getGroups().get(0).getContacts());
                    }
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

    }
}
