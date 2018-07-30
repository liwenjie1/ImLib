package com.yanxiu.im.sender;

import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.net.SaveTextMsgRequest_new;
import com.yanxiu.im.net.SaveTextMsgResponse_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import okhttp3.Request;

/**
 * 该类主要是用于实现Im文本发送和停止逻辑
 * Created by 杨小明 on 2018/5/7.
 */

public class TextSender extends SenderBase {

    protected TextSender(MsgItemBean msgItemBean) {
        super(msgItemBean);
    }

    /**
     * 发送文本消息
     */
    @Override
    public void startSend() {
        SaveTextMsgRequest_new saveTextMsgRequest = new SaveTextMsgRequest_new();
        saveTextMsgRequest.imToken = Constants.imToken;
        saveTextMsgRequest.topicId = Long.toString(mMsgItemBean.getTopicId());
        saveTextMsgRequest.msg = mMsgItemBean.getMsg();
        saveTextMsgRequest.reqId = mMsgItemBean.getReqId();
        saveTextMsgRequest.startRequest(SaveTextMsgResponse_new.class, new IYXHttpCallback<SaveTextMsgResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, SaveTextMsgResponse_new ret) {
                try {
                    ImMsg_new imMsg = ret.data.topicMsg.get(0);
                    mMsgItemBean.setRealMsgId(imMsg.msgId);
                    handleSuccess();
                } catch (Exception e) {
                    handleFail();
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                handleFail();
            }
        });
    }

    @Override
    public void stopSend() {

    }

}
