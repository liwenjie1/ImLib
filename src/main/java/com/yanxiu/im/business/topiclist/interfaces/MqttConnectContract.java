package com.yanxiu.im.business.topiclist.interfaces;

import android.app.Activity;

/**
 * Created by 朱晓龙 on 2018/5/21 15:41.
 */

public interface MqttConnectContract {

    interface View {
        void onMqttConnected();
        void onMqttDisconnected();
    }

    interface Presenter{
        void doConnectMqtt(Activity activity);
        void doDisConnectMqtt(Activity activity);
    }
}
