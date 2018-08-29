package com.yanxiu.im.manager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.yanxiu.im.db.ImSpManager;
import com.yanxiu.lib.yx_basic_library.YXApplication;
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

import java.util.HashMap;

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


    private int uuid = 0;
    private String host = "tcp://";

    private MqttConnectManager(Context applicationContext) {
        this.applicationContext = applicationContext;
    }


    private String createClientId() {
        return "android" + MqttClient.generateClientId();
//        return "android" + uuid++ + UUID.randomUUID().toString();
    }

    /**
     * 订阅 topic  当前项目为订阅  imtopic
     * */
    public void subscribeTopics(long... topicId) {
        String[] topics=new String[topicId.length];
        int[] qoss=new int[topicId.length];
        for (int i = 0; i < topics.length; i++) {
            topics[i]=constructTopicStr(topicId[i]);
            qoss[i]= 1;
        }
        try {
            mMqttClient.subscribe(topics,qoss);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribeTopics(long... topicId) {
        String[] topics=new String[topicId.length];
        for (int i = 0; i < topics.length; i++) {
            topics[i]=constructTopicStr(topicId[i]);
        }
        try {
            mMqttClient.unsubscribe(topics);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribeMember(long imId) {
        try {
            mMqttClient.subscribe(constructMemberStr(imId),1);
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
                mMqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 连接上一次连接的 mqtt 服务器 如果没有  直接回调失败
     */
    public void connectMqttServer(MqttServerConnectCallback connectCallback) {
        final String imHost = ImSpManager.getInstance().getImHost();
        if (!TextUtils.isEmpty(imHost)) {
            this.host += imHost;
            connectMqttServer(host, connectCallback);
        } else {
            connectCallback.onFailure();
        }
    }

    /**
     * 连接到 目标mqtt服务器
     * 哪里需要 mqtt 哪里调用  基本为  ImTopicListFragment 与  MsgListActivity 部分会使用
     */
    public void connectMqttServer(String host, final MqttServerConnectCallback connectCallback) {
        if (TextUtils.isEmpty(host)) {
            if (connectCallback != null) {
                connectCallback.onFailure();
            }
            return;
        }
        //设置连接参数
        MqttConnectOptions options = getMqttConnectOptions();
        //检查 是否有其他 mqtt 连接在运行 如果有  断开连接
        disconnectClientOnExsist();

        mMqttClient = new MqttAndroidClient(applicationContext, host, createClientId());
        //保存....
        mqttConnections.put(host, mMqttClient);
        //设置 mqtt 的监听
        mMqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                //连接丢失
                Log.i(TAG, "connectionLost: ");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //收到新消息
                Log.i(TAG, "messageArrived: ");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                //
                Log.i(TAG, "deliveryComplete: ");
            }
        });
        //进行连接
        //连接行为监听
        try {
            mMqttClient.connect(options, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    YXLogger.d(TAG, "connect success");
                    //连接成功
                    if (connectCallback != null) {
                        connectCallback.onSuccess();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    YXLogger.d(TAG, "connect failure");
                    //失败
                    connectCallback.onFailure();
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
        for (String host : mqttConnections.keySet()) {
            MqttAndroidClient client = mqttConnections.get(host);
            if (client.isConnected()) {
                try {
                    client.disconnect();

                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
            client.close();
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
        return options;
    }


    public interface MqttServerConnectCallback {
        void onSuccess();

        void onFailure();
    }


}