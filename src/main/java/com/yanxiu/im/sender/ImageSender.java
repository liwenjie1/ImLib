package com.yanxiu.im.sender;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.test.yanxiu.common_base.ui.BaseApplication;
import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.net.GetQiNiuTokenRequest_new;
import com.yanxiu.im.net.GetQiNiuTokenResponse_new;
import com.yanxiu.im.net.SaveImageMsgRequest_new;
import com.yanxiu.im.net.SaveImageMsgResponse_new;
import com.yanxiu.lib.yx_basic_library.YXApplication;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import okhttp3.Request;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * 该类主要是用于实现Im图片发送和停止逻辑
 * Created by 杨小明 on 2018/5/7.
 */

public class ImageSender extends SenderBase {
    //  1：图片压缩  进度占比
    private static final double COMPRESS_WEIGHT = 2;
    //  2：获取七牛token  进度占比
    private static final double GET_QINIU_TOKEN_WEIGHT = 1;
    //  3：七牛图片上传  进度占比
    private static final double UPLOAD_IMAGE_WEIGHT = 16;
    //  4：上传研修接口 进度占比
    private static final double SEND_IMAGE_WEIGHT = 1;
    //  总比例
    private static final double TOTAL_WEIGHT = COMPRESS_WEIGHT + GET_QINIU_TOKEN_WEIGHT
            + UPLOAD_IMAGE_WEIGHT + SEND_IMAGE_WEIGHT;
    //七牛上传
    private UploadManager mUploadManager;

    protected ImageSender(MsgItemBean msgItemBean) {
        super(msgItemBean);
        mUploadManager = new UploadManager(SenderUtil.config);
    }

    /**
     * Im发送图片分为以下几步如下：
     * 1：图片压缩
     * 2：获取七牛token
     * 3：图片上传
     * 4：上传研修接口
     */
    @Override
    public void startSend() {
        imageCompress();
    }

    @Override
    public void stopSend() {
    }

    /**
     * 图片压缩
     */
    private void imageCompress() {
        //鲁班压缩
        Luban.with(YXApplication.getContext())
                .load(mMsgItemBean.getLocalViewUrl())
                .setTargetDir(SenderUtil.getCompressPath())
                .ignoreBy(200)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        handleProgress(0);
                    }

                    @Override
                    public void onSuccess(File file) {
                        Integer[] wh = SenderUtil.getImgWithAndHeight(file.getAbsolutePath());
                        mMsgItemBean.setWidth(wh[0]);
                        mMsgItemBean.setHeight(wh[1]);
                        mMsgItemBean.setLocalViewUrl(file.getAbsolutePath());
                        handleProgress(COMPRESS_WEIGHT / TOTAL_WEIGHT);
                        requestQiNiuToken();
                    }

                    @Override
                    public void onError(Throwable e) {
                        handleFail();
                    }
                }).launch();
    }

    /**
     * 获取七牛token,有了七牛token才能上传，
     * 目前为了结构清晰每次都去获取token 省去了数据库中加qiniuKey字段
     * 获取七牛token进度占发送图片的5% （整体加上压缩20%则为25%）
     * 见wiki：http://wiki.yanxiu.com/pages/viewpage.action?pageId=12322622
     */
    private void requestQiNiuToken() {
        GetQiNiuTokenRequest_new getQiNiuTokenRequest = new GetQiNiuTokenRequest_new();
        getQiNiuTokenRequest.from = "100";//100:前端存储小图片，APP上传七牛
        getQiNiuTokenRequest.dtype = "app";//app：APP使用,web：网页前端
        getQiNiuTokenRequest.token = Constants.token;
        getQiNiuTokenRequest.startRequest(GetQiNiuTokenResponse_new.class, new IYXHttpCallback<GetQiNiuTokenResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, GetQiNiuTokenResponse_new ret) {
                try {
                    handleProgress((COMPRESS_WEIGHT + GET_QINIU_TOKEN_WEIGHT) / TOTAL_WEIGHT);
                    uploadImgByQiNiu(ret.getData().getToken());
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

    /**
     * 上传图片到七牛服务器
     *
     * @param qiNiuToken 七牛token
     */
    private void uploadImgByQiNiu(final String qiNiuToken) {
        mUploadManager.put(mMsgItemBean.getLocalViewUrl(), null, qiNiuToken, new UpCompletionHandler() {
            @Override
            public void complete(String s, ResponseInfo responseInfo, JSONObject jsonObject) {
                if (responseInfo.isOK()) {
                    try {
                        String key = jsonObject.getString("key");
                        requestSendImg(key);
                    } catch (JSONException e) {
                        handleFail();
                    }
                } else {
                    handleFail();
                }
            }
        }, new UploadOptions(null, null, false, new UpProgressHandler() {
            @Override
            public void progress(String s, double v) {
                handleProgress((COMPRESS_WEIGHT + GET_QINIU_TOKEN_WEIGHT
                        + UPLOAD_IMAGE_WEIGHT * v) / TOTAL_WEIGHT);
            }

        }, null));
    }

    /**
     * 上传七牛成功之后请求研修服务获取完整的图片地址
     */
    private void requestSendImg(String imgKey) {
        SaveImageMsgRequest_new saveImageMsgRequest = new SaveImageMsgRequest_new();
        saveImageMsgRequest.imToken = Constants.imToken;
        saveImageMsgRequest.topicId = Long.toString(mMsgItemBean.getTopicId());
        saveImageMsgRequest.rid = imgKey;
        saveImageMsgRequest.height = String.valueOf(mMsgItemBean.getHeight());
        saveImageMsgRequest.width = String.valueOf(mMsgItemBean.getWidth());
        saveImageMsgRequest.reqId = mMsgItemBean.getReqId();
        saveImageMsgRequest.startRequest(SaveImageMsgResponse_new.class, new IYXHttpCallback<SaveImageMsgResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {
            }

            @Override
            public void onSuccess(YXRequestBase request, SaveImageMsgResponse_new ret) {
                try {
                    ImMsg_new imMsg = ret.data.topicMsg.get(0);
                    mMsgItemBean.setViewUrl(imMsg.contentData.viewUrl);
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

}
