package com.yanxiu.im.business.topiclist.interfaces.impls;

import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.topiclist.interfaces.MqttConnectContract;
import com.yanxiu.im.manager.MqttConnectManager;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import java.util.List;

/**
 * Created by 朱晓龙 on 2018/5/21 15:42.
 * 这里处理 mqtt service的连接
 */

public class MqttConnectPresenter implements MqttConnectContract.Presenter {

    private final String TAG = getClass().getSimpleName();
    private MqttConnectContract.View view;


    public MqttConnectPresenter(MqttConnectContract.View view) {
        this.view = view;
    }

    /*检查 mqtt 的连接情况*/
    public boolean checkMqttState() {
        return false;
    }

    public void subscribeTopics(List<TopicItemBean> topics) {
        //空判断
        if (topics == null) {
            YXLogger.e("immqtt", "topiclist is null ");
            return;
        }
        for (TopicItemBean topic : topics) {
            subScribeTopic(topic.getTopicId());
        }
    }

    public void connectMqttServer() {
        //request  mqtt host
        String host = "";
        MqttConnectManager.getInstance().connectMqttServer(host, new MqttConnectManager.MqttServerConnectCallback() {
            @Override
            public void onSuccess() {
                view.onConnected();
            }

            @Override
            public void onFailure() {
                view.onDisconnected();
            }
        });
    }

    @Override
    public void subScribeTopic(long topicId) {
    }

    @Override
    public void unsubScribeTopic(long topicId) {
    }
}
