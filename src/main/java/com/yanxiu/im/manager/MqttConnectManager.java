package com.yanxiu.im.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;

import com.yanxiu.im.Constants;
import com.yanxiu.im.db.ImSpManager;
import com.yanxiu.im.net.PolicyConfigRequest_new;
import com.yanxiu.im.net.PolicyConfigResponse_new;
import com.yanxiu.im.service.MqttService;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import java.util.ArrayList;
import java.util.UUID;

import okhttp3.Request;

/**
 * create by 朱晓龙 2018/8/27 上午10:22
 * 负责处理 mqtt service 的连接
 */
public class MqttConnectManager {
    private static MqttConnectManager INSTANCE;

    public static MqttConnectManager getInstance() {
        return INSTANCE;
    }

    private final String TAG = getClass().getSimpleName();
    private MqttReconnectManager mReconnectManager;
    //
    private ArrayList<String> subscribedIds;


    public static void init(Context appContext) {
        if (INSTANCE == null) {
            synchronized (MqttConnectManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MqttConnectManager(appContext);
                }
            }
        }
    }


    private Context applicationContext;

    public MqttConnectManager(Context applicationContext) {
        this.applicationContext = applicationContext;
        //重连器    按30秒间隔 无限重试
        mReconnectManager = new MqttReconnectManager(-1, 30);
        subscribedIds = new ArrayList<>();
    }


    //服务绑定标志
    private boolean serviceBindedFlat = false;
    //binder
    private MqttService.MqttBinder mqttBinder;
    //本地 mqttservice 绑定回调
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            YXLogger.d(TAG, "MQTT 服务器service connected 到 application");
            serviceBindedFlat = true;
            mqttBinder = (MqttService.MqttBinder) service;

            mqttBinder.getService().setmMqttServiceCallback(mMqttServiceCallback);

            if (mMqttLocalServiceConnectListener != null) {
                mMqttLocalServiceConnectListener.onLocalServiceBinded();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            YXLogger.d(TAG, "MQTT  service Disconnected");
            serviceBindedFlat = false;
            mqttBinder.disconnect();
            mqttBinder = null;
            if (mMqttLocalServiceConnectListener != null) {
                mMqttLocalServiceConnectListener.onLocalServiceUnbinded();
            }
        }
    };

    /**
     * mqtt 服务器 连接回调
     */
    private MqttService.MqttServiceCallback mMqttServiceCallback = new MqttService.MqttServiceCallback() {
        @Override
        public void onDisconnect() {
            YXLogger.d(TAG, "onDisconnect");
            if (mMqttServerConnectListener != null) {
                mMqttServerConnectListener.onMqttServerDisconnected();
            }
            //重连 开始
        }

        @Override
        public void onConnect() {
            //连接成功 取消重连
            YXLogger.d(TAG, "onConnect");
            //连接 mqtt 服务器成功 订阅 immember 信息
            mqttBinder.subscribeMember(Constants.imId);
            if (mMqttServerConnectListener != null) {
                mMqttServerConnectListener.onMqttServerConnected();
            }
        }
    };

    //绑定 本地 service
    public boolean bindMqttService() {
        if (this.applicationContext != null) {
            Intent serviceIntent = new Intent(applicationContext, MqttService.class);
            return this.applicationContext.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
        return false;
    }

    //解绑本地 service
    public void unbindMqttService() {
        disconnectMqttService();
        this.applicationContext.unbindService(mServiceConnection);
    }

    /**
     * 请求获取 mqtt host
     */
    private void requestMqttHost(final GetImHostCallBack callBack) {
        PolicyConfigRequest_new policyConfigRequest = new PolicyConfigRequest_new();
        final UUID hostRequestUUID = policyConfigRequest.startRequest(PolicyConfigResponse_new.class, new IYXHttpCallback<PolicyConfigResponse_new>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, PolicyConfigResponse_new ret) {
                YXLogger.d(TAG, "requestMqtt host onSuccess ");
                if (ret.code == 0 && ret.data != null) {
                    ImSpManager.getInstance().setImHost(ret.data.getMqttServer());
                    if (callBack != null) {
                        callBack.onSuccess(ret.data.getMqttServer());
                    }
                } else {
                    String oldHost = ImSpManager.getInstance().getImHost();
                    if (TextUtils.isEmpty(oldHost)) {
                        requestMqttHost(callBack);
                    } else {
                        if (callBack != null) {
                            callBack.onSuccess(oldHost);
                        }
                    }
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                YXLogger.d(TAG, "requestMqtt host fail ");
                String oldHost = ImSpManager.getInstance().getImHost();
                if (TextUtils.isEmpty(oldHost)) {
                    requestMqttHost(callBack);
                } else {
                    if (callBack != null) {
                        callBack.onSuccess(oldHost);
                    }
                }
            }
        });
    }

    public interface GetImHostCallBack {
        void onSuccess(String host);

        void onFailure(String msg);
    }

    /**
     * 本地服务状态监听   mqtt 服务器连接状态监听
     */
    private MqttLocalServiceConnectListener mMqttLocalServiceConnectListener;
    private MqttServerConnectListener mMqttServerConnectListener;

    public void setMqttServerConnectListener(MqttServerConnectListener mqttServerConnectListener) {
        mMqttServerConnectListener = mqttServerConnectListener;
    }

    public void setMqttLocalServiceConnectListener(MqttLocalServiceConnectListener mqttLocalServiceConnectListener) {
        mMqttLocalServiceConnectListener = mqttLocalServiceConnectListener;
    }

    public interface MqttLocalServiceConnectListener {
        void onLocalServiceBinded();

        void onLocalServiceUnbinded();


    }

    public interface MqttServerConnectListener {
        void onMqttServerConnected();

        void onMqttServerDisconnected();
    }


    /*订阅与 取消订阅*/
    public void subscribeTopic(long topicId) {
        if (mqttBinder != null) {
            mqttBinder.subscribeTopic(topicId + "");
        }
    }

    public void unsubscribeTopic(long topicId) {
        if (mqttBinder != null) {
            mqttBinder.unsubscribeTopic(topicId + "");
        }
    }


    /**
     * 请求 mqtt 连接
     * 在获取用户 imtoken 调用
     * 调用了 当前方法 证明 正常获取了用户信息
     * 如果获取失败了 ？
     */
    public void connectMqttServer() {
        //首先请求 mqtthost
        requestMqttHost(new GetImHostCallBack() {
            @Override
            public void onSuccess(String host) {
                //成功获取 mqtt host 进行 mqtt 服务器连接
                mqttBinder.init(host);
                mqttBinder.connect();
            }

            @Override
            public void onFailure(String msg) {
                //获取 mqtt host 失败了  进行重试
                YXLogger.e(TAG, msg + "");
            }
        });
    }

    /**
     * 断开与 mqtt 服务器的连接
     * 在更换用户 或是 关闭 app 时调用
     */
    public void disconnectMqttService() {
        //取消所有订阅
        for (String subscribedId : subscribedIds) {
            mqttBinder.unsubscribeTopic(subscribedId);
        }
        //取消 member 消息订阅
        mqttBinder.subscribeMember(Constants.imId);
        mqttBinder.disconnect();
    }


}
