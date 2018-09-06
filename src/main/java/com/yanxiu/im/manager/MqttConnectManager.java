package com.yanxiu.im.manager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.yanxiu.im.Constants;
import com.yanxiu.im.db.ImSpManager;
import com.yanxiu.im.event.MqttConnectedEvent;
import com.yanxiu.im.net.PolicyConfigRequest_new;
import com.yanxiu.im.net.PolicyConfigResponse_new;
import com.yanxiu.im.protobuf.ImMqttProto;
import com.yanxiu.im.protobuf.MemberOnlineProto;
import com.yanxiu.im.protobuf.MqttMsgProto;
import com.yanxiu.lib.yx_basic_library.YXApplication;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.UUID;

import okhttp3.Request;

/**
 * create by 朱晓龙 2018/8/27 上午10:22
 * 负责处理 mqtt service 的连接
 */
public class MqttConnectManager {
    private static MqttConnectManager INSTANCE;

    public static MqttConnectManager getInstance() {
        if (INSTANCE == null) {
            synchronized (MqttConnectManager.class) {
                if (INSTANCE == null) {
                    final Context context = YXApplication.getContext().getApplicationContext();
                    INSTANCE = new MqttConnectManager(context);
                }
            }
        }
        return INSTANCE;
    }

    private final String TAG = getClass().getSimpleName();

    //保存所有本机连接的 mqtt 服务器
    private HashMap<String, MqttAndroidClient> mqttConnections;


    private MqttAndroidClient mMqttClient;


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

    /*正式线mqtt 用户名和密码
 测试线没有对用户名和密码进行校验*/
    private String userName = "yxwork";
    private String passWord = "79A6g3pHb4tz2Bs8";
    private String clientId = "android01";

    private MqttConnectManager(Context applicationContext) {
        this.applicationContext = applicationContext;
    }


    private String createClientId() {
        return "android" + MqttClient.generateClientId();
//        return "android" + uuid++ + UUID.randomUUID().toString();
    }

    private void requestMqttHost(final GetMqttHostCallback callback) {
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
                String host = null;
                if (ret.code == 0 && ret.data != null) {
                    ImSpManager.getInstance().setImHost(ret.data.getMqttServer());
                }
                host = ImSpManager.getInstance().getImHost();
                if (!TextUtils.isEmpty(host)) {
                    callback.onGetHost(host);
                } else {
                    callback.onFailure();
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                YXLogger.d(TAG, "requestMqtt host fail ");
                String oldHost = ImSpManager.getInstance().getImHost();
                if (!TextUtils.isEmpty(oldHost)) {
                    callback.onGetHost(oldHost);
                } else {
                    callback.onFailure();
                }
            }
        });
    }

    public interface GetMqttHostCallback {
        void onGetHost(String host);

        void onFailure();
    }

    public boolean isConnected() {
        if (mMqttClient == null) {
            return false;
        }
        return mMqttClient.isConnected();
    }

    /**
     * 订阅 topic  当前项目为订阅  imtopic
     */
    public void subscribeTopics(long... topicId) {
        if (mMqttClient == null) {
            return;
        }
        String[] topics = new String[topicId.length];
        int[] qoss = new int[topicId.length];
        for (int i = 0; i < topics.length; i++) {
            topics[i] = constructTopicStr(topicId[i]);
            qoss[i] = 1;
        }

        if (topicId.length==0) {
            return;
        }
        try {
            mMqttClient.subscribe(topics, qoss, applicationContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "subscribe topic onSuccess: ");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "subscribe topic onFailure: ");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribeTopics(long... topicId) {
        String[] topics = new String[topicId.length];
        for (int i = 0; i < topics.length; i++) {
            topics[i] = constructTopicStr(topicId[i]);
        }
        try {
            mMqttClient.unsubscribe(topics);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribeMember(long imId) {
        try {
            mMqttClient.subscribe(constructMemberStr(imId), 1, applicationContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "subscribe member onSuccess: ");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "subscribe member onFailure: ");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribeMember(long imId) {
        try {
            mMqttClient.unsubscribe(constructMemberStr(imId));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private String constructTopicStr(long topidId) {
        return "im/v1.0/topic/" + topidId;
    }

    private String constructMemberStr(long imId) {
        return "im/v1.0/member/" + imId;
    }


    public void disconnectMqttServer() {
        if (mMqttClient != null) {
            try {
                onlineOrOfflinePublish(0);
                mQttHeartBeatManager.cancel();
                mMqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                YXLogger.e(TAG, e.getMessage());
            } catch (Exception e){
                e.printStackTrace();
            }
            mMqttClient = null;
        }
    }

    /**
     * 连接上一次连接的 mqtt 服务器 如果没有  直接回调失败
     */
    public void connectMqttServer(@Nullable final MqttServerConnectCallback connectCallback) {
        //获取 host
        requestMqttHost(new GetMqttHostCallback() {
            @Override
            public void onGetHost(String host) {
                ImSpManager.getInstance().setImHost(host);
                final String imHost = ImSpManager.getInstance().getImHost();
                if (!TextUtils.isEmpty(imHost)) {
                    connectMqttServer(host, connectCallback);
                } else {
                    if (connectCallback != null) {
                        connectCallback.onFailure();
                    }
                }
            }

            @Override
            public void onFailure() {
                if (connectCallback != null) {
                    connectCallback.onFailure();
                }
            }
        });


    }


    private boolean isConnectionLost = false;
    private boolean isReconnecting = false;
    private int retryTime = 999;
    private MqttReconnectManager mReconnectManager;

    private MqttHeartBeatManager mQttHeartBeatManager;

    /**
     * 连接到 目标mqtt服务器
     * 哪里需要 mqtt 哪里调用  基本为  ImTopicListFragment 与  MsgListActivity 部分会使用
     */
    public void connectMqttServer(@NonNull String host, final MqttServerConnectCallback connectCallback) {
        if (TextUtils.isEmpty(host)) {
            if (connectCallback != null) {
                connectCallback.onFailure();
            }
            return;
        }
        //设置连接参数
        final MqttConnectOptions options = getMqttConnectOptions();
        //检查 是否有其他 mqtt 连接在运行 如果有  断开连接
        if (mMqttClient != null && mMqttClient.isConnected()) {
            return;
        }
        disconnectClientOnExsist();
        mMqttClient = new MqttAndroidClient(applicationContext, "tcp://" + host, createClientId());
        //保存....
        mqttConnections.put(host, mMqttClient);
        //设置 mqtt 的监听
        mMqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                synchronized (MqttConnectManager.class) {
                    //连接丢失
                    isConnectionLost = true;
                    //进行重连 不限次数 30秒间隔
                    mReconnectManager = new MqttReconnectManager(-1, 30);
                    isReconnecting = true;
                    mReconnectManager.start(new MqttReconnectManager.AlarmCallback() {
                        @Override
                        public void onTick() {
                            Log.i(TAG, "onTick: attemp to reconnect");
                            doConnect(connectCallback, options);
                        }
                    });

                }
                Log.i(TAG, "connectionLost: ");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //收到新消息
                Log.i(TAG, "messageArrived: ");
                MqttProtobufManager.dealWithData(message.getPayload());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                //
                Log.i(TAG, "deliveryComplete: ");
            }
        });
        //进行连接
        //连接行为监听
        doConnect(connectCallback, options);
    }

    private void doConnect(final MqttServerConnectCallback connectCallback, MqttConnectOptions options) {
        if (mMqttClient == null) {
            return;
        }
        try {
            mMqttClient.connect(options, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (mReconnectManager != null) {
                        mReconnectManager.cancel();
                    }
                    YXLogger.d(TAG, "connect success");
                    //连接成功
                    onlineOrOfflinePublish(1);
                    if(mQttHeartBeatManager != null ){
                        mQttHeartBeatManager.cancel();
                    }
                    mQttHeartBeatManager = new MqttHeartBeatManager();
                    mQttHeartBeatManager.start();
                    //发送 eventbus 通知 mqtt 连接成功
                    EventBus.getDefault().post(new MqttConnectedEvent());
                    subscribeMember(Constants.imId);
                    if (connectCallback != null) {
                        connectCallback.onSuccess();
                    }
                    //发送 eventbus 通知 mqtt 连接成功
                    EventBus.getDefault().post(new MqttConnectedEvent());
                    subscribeMember(Constants.imId);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    YXLogger.d(TAG, "connect failure");
                    //失败
                    if (connectCallback != null) {
                        connectCallback.onFailure();
                    }
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开所有存在的连接
     */
    private void disconnectClientOnExsist() {
        if (mqttConnections == null) {
            mqttConnections = new HashMap<>();
            return;
        }
        for (String host : mqttConnections.keySet()) {
            MqttAndroidClient client = mqttConnections.get(host);
            if (client.isConnected()) {
                try {
                    client.disconnect();

                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
        mqttConnections.clear();
    }

    @NonNull
    private MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        //设置自动重连
        options.setAutomaticReconnect(true);
        //连接超时时间
        options.setConnectionTimeout(10);
        //设置 心跳包
        options.setKeepAliveInterval(60);
        options.setUserName(userName);
        options.setPassword(passWord.toCharArray());
        //定义遗言
        options.setWill("im/v1.0/upstream/online", onlineState(1), 0, false);
        return options;
    }


    public interface MqttServerConnectCallback {
        void onSuccess();

        void onFailure();
    }

    /**
     * APP端的离线、上线事件信息
     * wiki :http://wiki.yanxiu.com/pages/viewpage.action?pageId=12326692
     *
     * @param onlineState 状态：1-在线 0-离线
     */
    private byte[] onlineState(int onlineState) {
        byte[] result = null;
        try {
            ByteString member = MemberOnlineProto.MemberOnline.newBuilder()
                    .setBizSource(22)
                    .setMemberId(Constants.imId)
                    .setToken(Constants.imToken)
                    .setOnlineType(1)
                    .setOnlineState(onlineState)
                    .build()
                    .toByteString();

            ByteString imMqtt = ImMqttProto.ImMqtt.newBuilder()
                    .setImEvent(90)
                    .setReqId("2")
                    .addBody(member)
                    .build()
                    .toByteString();

            result = MqttMsgProto.MqttMsg.newBuilder()
                    .setVersion("1")
                    .setCodec("2")
                    .setSecurity("3")
                    .setType("4")
                    .setData(imMqtt)
                    .build().toByteArray();
            YXLogger.e("mqtt", result.toString());
        } catch (Exception e) {
            YXLogger.e("mqtt", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * APP端的离线、上线事件设置
     * wiki :http://wiki.yanxiu.com/pages/viewpage.action?pageId=12326692
     *
     * @param onlineState 状态：1-在线 0-离线
     */
    private void onlineOrOfflinePublish(int onlineState) {
        try {
            //连接成功，发送信息
            mMqttClient.publish("im/v1.0/upstream/online", onlineState(onlineState), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }



}