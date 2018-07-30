package com.yanxiu.im.business.utils;

import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

/**
 * Created by 朱晓龙 on 2018/6/6 10:21.
 * 检查服务器 数据格式是否正确
 */

public class ImServerDataChecker {
    public static final String TAG="ImServerDataChecker";

    /**
     * 检查 服务器返回的消息 数据是否合法
     */
    public static boolean imMsgCheck(ImMsg_new msgNew) {
        if (msgNew.msgId<0) {
            YXLogger.e(TAG,"服务器返回 msg id < 0");
            return false;
        }

        if (msgNew.contentData == null) {
            YXLogger.e(TAG,"服务器返回 msg contentData 为 null");
            return false;
        }

        return true;
    }

    /**
     * 检查服务器 返回的topic 数据格式 防止空指针
     * */
    public static boolean imTopicCheck(ImTopic_new topicNew){



        return true;
    }


}
