package com.yanxiu.im.business.topiclist.interfaces.impls;

import android.app.Activity;

import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.topiclist.interfaces.MqttConnectContract;
import com.yanxiu.im.manager.MqttConnectManager;
import com.yanxiu.im.manager.MqttReconnectManager;
import com.yanxiu.im.service.MqttService;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import java.util.List;
import java.util.Timer;

/**
 * Created by 朱晓龙 on 2018/5/21 15:42.
 * 这里处理 mqtt service的连接
 */

public class MqttConnectPresenter implements MqttConnectContract.Presenter, MqttConnectManager.MqttServiceConnectListener {

    private final String TAG = getClass().getSimpleName();
    private MqttService.MqttBinder binder = null;
    private Timer reconnectTimer = new Timer();
    private MqttConnectContract.View view;

    private MqttReconnectManager mReconnectManager;

    public MqttConnectPresenter(MqttConnectContract.View view) {
        this.view = view;
        MqttConnectManager.getInstance().setMqttServiceConnectListener(this);
        mReconnectManager = new MqttReconnectManager(10, 30);
    }


    public void subscribeTopics(List<TopicItemBean> topics) {
        //空判断
        if (topics == null) {
            YXLogger.e("immqtt", "topiclist is null ");
            return;
        }
        for (TopicItemBean topic : topics) {
            subscribeTopic(topic.getTopicId());
        }
    }

    public void subscribeTopic(long topicId) {
        MqttConnectManager.getInstance().subscribeTopic(topicId);
    }

    public void unsubscribeTopic(long topicId) {
        MqttConnectManager.getInstance().unsubscribeTopic(topicId);
    }

    @Override
    public void doConnectMqtt(final Activity activity) {
        MqttConnectManager.getInstance().requestMqttHost(new MqttConnectManager.GetImHostCallBack() {
            @Override
            public void onSuccess(String host) {
                MqttConnectManager.getInstance().connectMqttServer(host);
            }
        });
    }

    @Override
    public void doDisConnectMqtt(final Activity activity) {
        MqttConnectManager.getInstance().disconnectMqttService();
    }


    /*
     mqtt 相关回调
     * */
    @Override
    public void onLocalServiceBinded() {
        //MqttService 连接了 这里一般收不到
        YXLogger.d(TAG, "onLocalServiceBinded");
    }

    @Override
    public void onLocalServiceUnbinded() {
        //MqttService 断开了
        YXLogger.d(TAG, "onLocalServiceUnbinded");
    }

    @Override
    public void onMqttServerConnected() {
        //mqtt 服务器连接成功
        YXLogger.d(TAG, "onMqttServerConnected");
        mReconnectManager.cancel();
        if (view != null) {
            view.onMqttConnected();
        }
    }

    @Override
    public void onMqttServerDisconnected() {
        //mqtt 服务器 断开了 设计重连机制
        YXLogger.d(TAG, "onMqttServerDisconnected");
        if (view != null) {
            view.onMqttDisconnected();
        }
        //尝试重连
        final boolean started = mReconnectManager.start(new MqttReconnectManager.AlarmCallback() {
            @Override
            public void onTick() {
                YXLogger.d(TAG,"onTick");
                doConnectMqtt(null);
            }
        });

        //如果成功开启重连

    }
}
