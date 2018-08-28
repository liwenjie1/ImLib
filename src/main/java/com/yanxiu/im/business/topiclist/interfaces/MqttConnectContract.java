package com.yanxiu.im.business.topiclist.interfaces;

/**
 * Created by 朱晓龙 on 2018/5/21 15:41.
 */

public interface MqttConnectContract {

    interface View {
    }

    interface Presenter {
        void subScribeTopic(long topicId);

        void unsubScribeTopic(long topicId);
    }
}
