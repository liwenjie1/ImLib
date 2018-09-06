package com.yanxiu.im.manager;

import com.yanxiu.im.Constants;
import com.yanxiu.im.net.HeartBeatRequest;
import com.yanxiu.im.net.ImResponseBase_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Request;

/**
 * im当前用户心跳轮训管理类
 * create by 戴延枫
 */
public class MqttHeartBeatManager {

    private long reconnectDelay = 60;//重连尝试间隔 单位 秒


    private TimerTask alarmTask;
    private Timer mTimer;

    private boolean runFlag = false;


    public MqttHeartBeatManager() {
    }


    public boolean start() {
        if (runFlag) {
            return false;
        }
        mTimer = new Timer("MqttHeartBeat");
        alarmTask = new TimerTask() {
            @Override
            public void run() {
                startRequest();
            }
        };
        runFlag = true;
        mTimer.schedule(alarmTask, 0, reconnectDelay * 1000);
        return true;
    }

    public void cancel() {
        if (mRequest != null) {
            mRequest.cancelRequest();
        }
        if (alarmTask != null) {
            alarmTask.cancel();
        }
        if (mTimer != null) {
            mTimer.purge();
        }
        mTimer = null;
        mRequest = null;
        runFlag = false;
    }

    private HeartBeatRequest mRequest;

    private void startRequest() {
        if (mRequest != null) {
            mRequest.cancelRequest();
        } else {
            mRequest = new HeartBeatRequest();
        }
        mRequest.imToken = Constants.imToken;
        mRequest.startRequest(ImResponseBase_new.class, new IYXHttpCallback<ImResponseBase_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, ImResponseBase_new ret) {


            }

            @Override
            public void onFail(YXRequestBase request, Error error) {

            }
        });
    }
}
