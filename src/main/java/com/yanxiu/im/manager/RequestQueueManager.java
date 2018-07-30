package com.yanxiu.im.manager;


import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;

/**
 * Created by cailei on 05/03/2018.
 * 用于顺序执行添加进来的Requests
 */

public class RequestQueueManager {
    public class Item<T> {
        YXRequestBase request;
        Class<T> clazz;
        IYXHttpCallback<T> callback;
    }

    private List<Object> items = new ArrayList<>();
    private boolean hasOngoingRequest = false;
    private YXRequestBase ongoingRequest;
    private boolean bPaused;

    private RequestQueueCallBack mCallBack;

    public <T> void addRequest(YXRequestBase request, final Class<T> clazz, final IYXHttpCallback<T> callback) {
        IYXHttpCallback<T> queueCallback = new IYXHttpCallback<T>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {
                callback.onRequestCreated(request);
            }

            @Override
            public void onSuccess(YXRequestBase request, T ret) {
                hasOngoingRequest = false;
                ongoingRequest = null;
                removeFromQueue(request);
                callback.onSuccess(request, ret);
                doNextRequest();
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {

                hasOngoingRequest = false;
                ongoingRequest = null;
                removeFromQueue(request);
                callback.onFail(request, error);    // 如果需要失败重试，则外部在onfail时重新加queue到index 0即可
                doNextRequest();
            }
        };

        Item<T> item = new Item<>();
        item.request = request;
        item.clazz = clazz;
        item.callback = queueCallback;
        items.add(item);
        doNextRequest();
    }

    private void removeFromQueue(YXRequestBase request) {
        for (Object item : items) {
            final Item<Object> i = (Item<Object>) item;
            if (i.request == request) {
                items.remove(i);
                break;
            }
        }
    }

    private void doNextRequest() {
        if (bPaused) {
            return;
        }

        if (hasOngoingRequest) {
            return;
        }
        if (items.size() == 0) {
            if (mCallBack != null)
                mCallBack.requestAllFinish();
            return;
        }

        final Item<Object> item = (Item<Object>) items.get(0);
        hasOngoingRequest = true;
        item.request.startRequest(item.clazz, item.callback);
        ongoingRequest = item.request;
    }

    public void setPause(boolean pause) {
        bPaused = true;
        if (ongoingRequest != null) {
            ongoingRequest.cancelRequest();
        }
    }

    public void setResume(boolean resume) {
        bPaused = false;
        doNextRequest();
    }

    public interface RequestQueueCallBack {
        /**
         * 队列里请求全部结束
         */
        void requestAllFinish();
    }

    public RequestQueueCallBack getmCallBack() {
        return mCallBack;
    }

    public void setmCallBack(RequestQueueCallBack mCallBack) {
        this.mCallBack = mCallBack;
    }
}
