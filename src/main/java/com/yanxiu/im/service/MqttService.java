package com.yanxiu.im.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.test.yanxiu.common_base.utils.SrtLogger;
import com.yanxiu.im.manager.MqttProtobufManager;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;

/**
 * mqtt的service
 * 该service与MainActivity绑定
 * Created by cailei on 05/03/2018.
 */

public class MqttService extends Service {
    public interface MqttServiceCallback {
        void onDisconnect();

        void onConnect();
    }

    private MqttServiceCallback mMqttServiceCallback;

    public void setmMqttServiceCallback(MqttServiceCallback mMqttServiceCallback) {
        this.mMqttServiceCallback = mMqttServiceCallback;
    }

    public class MqttBinder extends Binder {
        public void init() {
            doInit();
        }

        // host为null时，采用默认
        public void init(String host, String user, String pwd) {
            YXLogger.d("mqtt", "init host:  " + host + "user :" + user + " pwd : " + pwd);
            MqttService.this.host = host;
            MqttService.this.userName = user;
            MqttService.this.passWord = pwd;
            doInit();
        }

        public void init(String host) {
            host = MqttService.this.host + host;
            init(host, MqttService.this.userName, MqttService.this.passWord);
        }

        public void connect() {
            doConnect();
        }



        public void disconnect() {
            doDisconnect();
        }

        public void subscribeTopic(String topicId) {
            doSubscribeTopic(topicId);
        }

        public void unsubscribeTopic(String topicId) {
            doUnsubscribeTopic(topicId);
        }

        public void subscribeMember(long memberId) {
            doSubscribeMember(memberId);
        }

        public void unsubscribeMember(long memberId) {
            doUnsubscribeMember(memberId);
        }

        public MqttService getService() {
            return MqttService.this;
        }
    }

    private String host = "tcp://";

    /*正式线mqtt 用户名和密码
   测试线没有对用户名和密码进行校验*/
    private String userName = "yxwork";
    private String passWord = "79A6g3pHb4tz2Bs8";
    private String clientId = "android01";

    protected MqttAndroidClient mClient;
    protected MqttConnectOptions mMqttConnectOptions;

    // 用EventBus通知Topic, Msg界面
    protected MqttCallback mCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            if (cause == null) {
                // 手动停止
                return;
            }

            SrtLogger.log("immqtt", "connection lost");
            if (mMqttServiceCallback != null) {
                mMqttServiceCallback.onDisconnect();
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            MqttProtobufManager.dealWithData(message.getPayload());
            // 有消息来了
            SrtLogger.log("immqtt", "mqtt msg arrived : " + topic);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    private int uuid = 0;

    private void doInit() {
        clientId = "android" + uuid++ + UUID.randomUUID().toString();

        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(userName) || TextUtils.isEmpty(passWord) || TextUtils.isEmpty(clientId)) {
            throw new NullPointerException("please call method setClientMessage(String host, String userName, String passWord, String clientId) first");
        }
        mClient = new MqttAndroidClient(this, host, clientId);
        if (mMqttConnectOptions == null) {
            mMqttConnectOptions = new MqttConnectOptions();
            // 清除缓存
            mMqttConnectOptions.setCleanSession(true);
            // 设置超时时间，单位：秒
            mMqttConnectOptions.setConnectionTimeout(10);
            // 心跳包发送间隔，单位：秒
            mMqttConnectOptions.setKeepAliveInterval(60);
        }
        mMqttConnectOptions.setUserName(userName);
        mMqttConnectOptions.setPassword(passWord.toCharArray());
        mClient.setCallback(mCallback);
    }

    private void doConnect() {
        if ((mClient != null) && (!mClient.isConnected())) {
            try {
                mClient.connect(mMqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {

                        SrtLogger.log("immqtt", "mqtt connectted");
                        if (mMqttServiceCallback != null) {
                            mMqttServiceCallback.onConnect();
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        SrtLogger.log("immqtt", "mqtt failed to connect :" + exception.getMessage());
                        if (mMqttServiceCallback != null) {
                            mMqttServiceCallback.onDisconnect();
                        }
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    private void doDisconnect() {
        if ((mClient != null) && mClient.isConnected()) {
            //mClient.disconnect();
            mClient.unregisterResources();
            mClient.close();
            mClient = null;
        }
    }

    public void doSubscribeTopic(String topicId) {
        if ((mClient != null) && mClient.isConnected()) {
            try {
                mClient.subscribe("im/v1.0/topic/" + topicId, 1, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
//                        SrtLogger.log("immqtt", "mqtt subscribe topic successfully");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                        SrtLogger.log("immqtt", "mqtt subscribe topic failed");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 取消 topic 的订阅
     */
    public void doUnsubscribeTopic(String topicId) {
        if ((mClient != null) && mClient.isConnected()) {
            try {
                mClient.unsubscribe("im/v1.0/topic/" + topicId, MqttService.this, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i("immqtt", "onSuccess: ");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("immqtt", "onFailure: ", exception.getCause());
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }


    public void doSubscribeMember(long memberId) {
        if ((mClient != null) && mClient.isConnected()) {
            try {
                mClient.subscribe("im/v1.0/member/" + Long.toString(memberId), 1, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        //SrtLogger.log("immqtt", "mqtt subscribe member successfully");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        //SrtLogger.log("immqtt", "mqtt subscribe member failed");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void doUnsubscribeMember(long memberId) {
        if ((mClient != null) && mClient.isConnected()) {
            try {
                mClient.unsubscribe("im/v1.0/member/" + Long.toString(memberId), MqttService.this, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        //SrtLogger.log("immqtt", "mqtt subscribe member successfully");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        //SrtLogger.log("immqtt", "mqtt subscribe member failed");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        SrtLogger.log("im service", "bind");
//        host = host + intent.getStringExtra("host");
        return new MqttBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        SrtLogger.log("im service", "rebind");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        SrtLogger.log("im service", "unbind");
        doDisconnect();
        return true;
    }


}
