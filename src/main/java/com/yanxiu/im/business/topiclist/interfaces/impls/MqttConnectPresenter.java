package com.yanxiu.im.business.topiclist.interfaces.impls;

import android.util.Log;

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

//        long[] ids=new long[topics.size()];
//        for (int i = 0; i < ids.length; i++) {
//            ids[i]=topics.get(i).getTopicId();
//        }
//        MqttConnectManager.getInstance().subscribeTopics(ids);
        for (TopicItemBean topic : topics) {
            subScribeTopic(topic.getTopicId());
        }
    }


    @Override
    public void subScribeTopic(long topicId) {
        Log.i(TAG, "subScribeTopic: ");
        MqttConnectManager.getInstance().subscribeTopics(topicId);
    }

    @Override
    public void unsubScribeTopic(long topicId) {
        Log.i(TAG, "unsubScribeTopic: ");
        MqttConnectManager.getInstance().unsubscribeTopics(topicId);
    }
}
