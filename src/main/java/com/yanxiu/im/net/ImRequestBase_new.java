package com.yanxiu.im.net;

import com.test.yanxiu.common_base.net.FaceShowAppBaseRequest;
import com.test.yanxiu.common_base.utils.SrtLogger;
import com.test.yanxiu.common_base.utils.UrlRepository;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.UUID;

import okhttp3.Request;

/**
 * im的request基类
 * Created by cailei on 02/03/2018.
 */
public class ImRequestBase_new extends FaceShowAppBaseRequest {
    public String bizSource;  // 来源，移动端用1
    public String bizId;      // 业务id，研修宝用1
    public String bizToken;         // App用的Token
    public String imToken;          // 专门为im用的Token
    public String reqId;         // 客户端生成的，保证唯一性的32位uuid

    ImRequestBase_new() {
        bizSource = "22";
        bizId = null;
        reqId = UUID.randomUUID().toString();
    }

    @Override
    protected boolean shouldLog() {
        return false;
    }

    @Override
    protected String urlServer() {
        return UrlRepository.getInstance().getImServer();
    }

    @Override
    protected String urlPath() {
        return null;
    }

    @Override
    protected String fullUrl() throws NullPointerException, IllegalAccessException, IllegalArgumentException {
        String url = super.fullUrl();
        SrtLogger.log("im http", url);
        return url;
    }

    @Override
    public <T> UUID startRequest(Class<T> clazz, final IYXHttpCallback<T> callback) {
        return super.startRequest(clazz, new IYXHttpCallback<T>() {
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
                ImResponseBase_new retbase = (ImResponseBase_new) ret;
                if (retbase.code != 0) {
                    callback.onFail(request, new Error(((ImResponseBase_new) ret).message));
                    return;
                }

                callback.onSuccess(request, ret);
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                callback.onFail(request, error);
            }
        });
    }
}
