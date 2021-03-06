package com.yanxiu;

import android.text.TextUtils;

import com.yanxiu.im.Constants;
import com.yanxiu.im.TopicsReponsery;
import com.yanxiu.im.bean.net_bean.ImTokenInfo_new;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.manager.MqttConnectManager;
import com.yanxiu.lib.yx_basic_library.YXApplication;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

/**
 * Im初始化操作，在这个文件里统一操作
 * Created by 戴延枫 on 2018/5/15.
 */

public class ImConfig {

    /**
     * app type  管理端与学员端的 im 有功能差异 通过 type 来控制界面以及功能的使能状态
     */
    private ClientType type;

    public enum ClientType {
        AdminClient, StudentClient
    }

    /**
     * imConfig 成功初始化的标志位
     */
    private static boolean hasInitialazed = false;

    public static boolean isHasInitialazed() {
        return hasInitialazed;
    }

    public static void clear(){
        hasInitialazed=false;
        Constants.imId =-1;
        Constants.imAvatar = null;
        Constants.imToken = null;
        Constants.token = null;
        TopicsReponsery.getInstance().releaseResource();
    }

    /**
     * 将登陆获取的  intokeninfor 信息进行提取 保存
     */
    public static void init(ImTokenInfo_new imTokenInfo, String token) {
        //没能正常获取 im 信息
        if (imTokenInfo == null) {
            YXLogger.e("im", "imTokenInfo is null");
            return;
        }
        //获取的 IM 用户信息为空
        if (imTokenInfo.imMember == null) {
            YXLogger.e("im", "im user  is null");
            return;
        }

        Constants.imId = imTokenInfo.imMember.imId;
        Constants.imAvatar = imTokenInfo.imMember.avatar;
        Constants.imToken = imTokenInfo.imToken;
        Constants.token = token;
        // 成功初始化的标志  imid 有效  imtoken 有值   token 为 app 层级的参数 有 app 层级进行判断
        hasInitialazed = (Constants.imId > 0 && !TextUtils.isEmpty(Constants.imToken));
        if (hasInitialazed) {
            DatabaseManager.useDbForUser(Long.toString(Constants.imId) + "_db");
            MqttConnectManager.init(YXApplication.getContext().getApplicationContext());
            MqttConnectManager.getInstance().connectMqttServer(new MqttConnectManager.MqttServerConnectCallback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure() {

                }
            });
        }
    }
    /**
     * 将登陆获取的  intokeninfor 信息进行提取 保存
     */
    public static void init(ImTokenInfo_new imTokenInfo,long userId, String token) {
       Constants.userId=userId;
       init(imTokenInfo, token);
    }

    public static void setClientType(ClientType type) {
        switch (type) {
            case AdminClient:
                Constants.APP_TYPE = Constants.APP_TYPE_ADMIN;
                enableImSilence(true);
                break;
            case StudentClient:
                Constants.APP_TYPE = Constants.APP_TYPE_STUDENT;
                enableImSilence(false);
                break;
        }
    }

    /**
     * 是否开启 im 通讯录功能
     */
    public static void enableImContact(boolean enable) {
        Constants.showContacts = enable;
    }

    /**
     * 是否开启 im topic 的设置功能
     */
    public static void enableImSetting(boolean enable) {
        Constants.showTopicSetting = enable;
    }

    /**
     * 是否开启 im topic 设置中的 禁言设置
     */
    public static void enableImSilence(boolean enable) {
        Constants.showTopicSilent = enable;
    }


}
