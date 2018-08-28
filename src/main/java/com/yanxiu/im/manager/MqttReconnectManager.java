package com.yanxiu.im.manager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * create by 朱晓龙 2018/8/27 上午11:38
 * 重连 辅助类
 * 1、重新连接 mqtt 服务器
 * 2、重新绑定 mqttservice 到 applicationg
 */
public class MqttReconnectManager {

    private int reconnectTimes = 10;// 重连次数
    private long reconnectDelay = 10;//重连尝试间隔 单位 秒


    private TimerTask alarmTask;
    private Timer mTimer;

    private boolean runFlag = false;



    public MqttReconnectManager(int reconnectTimes, long reconnectDelay) {
        this.reconnectTimes = reconnectTimes;
        this.reconnectDelay = reconnectDelay;
    }


    public boolean start(AlarmCallback alarmCallback) {
        if (runFlag) {
            return false;
        }
        setAlarmCallback(alarmCallback);
        mTimer = new Timer("mqtt reconnect");
        alarmTask = new TimerTask() {
            @Override
            public void run() {
                if (mAlarmCallback != null) {
                    mAlarmCallback.onTick();
                }
            }
        };
        runFlag = true;
        mTimer.schedule(alarmTask, 0, reconnectDelay * 1000);
        return true;
    }

    public void cancel() {
        mAlarmCallback = null;
        if (alarmTask != null) {
            alarmTask.cancel();
        }
        if (mTimer != null) {
            mTimer.purge();
        }
        runFlag = false;
    }

    private AlarmCallback mAlarmCallback;

    public void setAlarmCallback(AlarmCallback alarmCallback) {
        mAlarmCallback = alarmCallback;
    }

    public interface AlarmCallback {
        void onTick();
    }


}
