package com.yanxiu.im.db;

import android.content.Context;
import android.content.SharedPreferences;

import com.yanxiu.im.ImApplication;

/**
 * sharePreference管理类
 * 只存储im相关数据
 *
 * @author 戴延枫
 */
public class ImSpManager {

    public static final String SP_NAME = "FaceShowIm";
    private static SharedPreferences mSharedPreferences = ImApplication.getContext()
            .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);

    private static ImSpManager instance;

    public static ImSpManager getInstance() {
        if (instance == null) {
            instance = new ImSpManager();
            mSharedPreferences = ImApplication.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }

        return instance;
    }

    /**
     * imhost
     */
    private final String IM_HOST = "ImHost";

    /**
     * 保存imhost
     */
    public void setImHost(String imHost) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(IM_HOST, imHost);
        editor.apply();
    }

    /**
     * imhost
     *
     * @return "" ：没记录
     */
    public String getImHost() {
        return mSharedPreferences.getString(IM_HOST, "");
    }



    /**
     * 聊聊设置
     * */

    //客户端类型
    public static final String APP_TYPE="APP_TYPE";
    //im 消息免打扰
    public static final String SETTING_PUSH="IM_SETTING_PUSH";
    //im
    public static final String SETTING_SILENT="IM_SETTING_SILENT";

    public void setImSetting(String key,String value){
        if (mSharedPreferences != null) {
            mSharedPreferences.edit().putString(key,value).apply();
        }
    }

    public String getImSetting(String key){
        if (mSharedPreferences != null) {
           return mSharedPreferences.getString(key,"");
        }
        return null;
    }

}
