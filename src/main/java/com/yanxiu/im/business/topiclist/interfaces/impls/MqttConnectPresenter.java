package com.yanxiu.im.business.topiclist.interfaces.impls;

import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.topiclist.interfaces.MqttConnectContract;
import com.yanxiu.im.manager.MqttConnectManager;
import com.yanxiu.im.manager.MqttReconnectManager;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import java.util.List;

/**
 * Created by 朱晓龙 on 2018/5/21 15:42.
 * 这里处理 mqtt service的连接
 */

public class MqttConnectPresenter implements MqttConnectContract.Presenter {

    private final String TAG = getClass().getSimpleName();
    private MqttConnectContract.View view;

    private MqttReconnectManager mReconnectManager;

    public MqttConnectPresenter(MqttConnectContract.View view) {
        this.view = view;
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

    @Override
    public void subScribeTopic(long topicId) {
        MqttConnectManager.getInstance().subscribeTopic(topicId);
    }

    @Override
    public void unsubScribeTopic(long topicId) {
        MqttConnectManager.getInstance().unsubscribeTopic(topicId);
    }
}
