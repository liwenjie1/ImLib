package com.yanxiu.im.business.topiclist.interfaces.impls;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;

import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.topiclist.interfaces.MqttConnectContract;
import com.yanxiu.im.db.ImSpManager;
import com.yanxiu.im.net.PolicyConfigRequest_new;
import com.yanxiu.im.net.PolicyConfigResponse_new;
import com.yanxiu.im.service.MqttService;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import okhttp3.Request;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by 朱晓龙 on 2018/5/21 15:42.
 * 这里处理 mqtt service的连接
 */

public class MqttConnectPresenter implements MqttConnectContract.Presenter {


    private MqttService.MqttBinder binder = null;
    private Timer reconnectTimer = new Timer();
    private MqttConnectContract.View view;

    public MqttConnectPresenter(MqttConnectContract.View view) {
        this.view = view;
    }


    public void subscribeTopics(List<TopicItemBean> topics) {
        for (TopicItemBean topic : topics) {
            subscribeTopic(topic.getTopicId());
        }
    }

    public void subscribeTopic(long topicId) {
        binder.subscribeTopic(topicId + "");
    }

    public void unsubscribeTopic(long topicId) {
        binder.unsubscribeTopic(topicId + "");
    }

    /**
     * 这是个异步过程可能会存在的问题
     * 在何处进行网络请求-->目前可以放在登录完毕后或者进入主页中 但是考虑到module的完整性并且此处做了mqtt连接失败会重连的策略  其实可以放在此处获取host的
     * 但是如果一些极端现象：现在server从host1切花到host2  但是host1仍然可用，这种情况下用户第一次进来还是连的host1  只有退出后再进入才会连host2  考虑到目前切换的频率极低所以就先这样写
     * 数据存储于Sp中：本来sp应用用统一的管理，但是这是个独立的module并且公用的spManager在App中没有放到CommonBase中,而且此处存储的host只在此处获取，就不去动app module中的SPManager了
     */
    private void getImHostRequest(final GetImHostCallBack iGetImHostCallBack) {
        PolicyConfigRequest_new policyConfigRequest = new PolicyConfigRequest_new();
        final UUID hostRequestUUID = policyConfigRequest.startRequest(PolicyConfigResponse_new.class, new IYXHttpCallback<PolicyConfigResponse_new>() {
            /**
             * startRequest()中生成get url，post body以后，调用OkHttp Request之前调用此回调
             *
             * @param request OkHttp Request
             */
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, PolicyConfigResponse_new ret) {
                if (ret.code == 0 && ret.data != null) {
//                    saveHost2Sp(ret.data.getMqttServer());
                    ImSpManager.getInstance().setImHost(ret.data.getMqttServer());
                    if (iGetImHostCallBack != null) {
                        iGetImHostCallBack.onSuccess(ret.data.getMqttServer());
                    }
                } else {
//                    String oldHost = getHostBySp();
                    String oldHost = ImSpManager.getInstance().getImHost();
                    if (TextUtils.isEmpty(oldHost)) {
                        getImHostRequest(iGetImHostCallBack);
                    } else {
                        if (iGetImHostCallBack != null) {
                            iGetImHostCallBack.onSuccess(oldHost);
                        }
                    }
                }
            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
//                String oldHost = getHostBySp();
                String oldHost = ImSpManager.getInstance().getImHost();
                if (TextUtils.isEmpty(oldHost)) {
                    getImHostRequest(iGetImHostCallBack);
                } else {
                    if (iGetImHostCallBack != null) {
                        iGetImHostCallBack.onSuccess(oldHost);
                    }
                }
            }
        });
    }

    private interface GetImHostCallBack {
        void onSuccess(String host);
    }

    /**
     * qmtt的服务
     */
    public ServiceConnection mqttServiceConnection = new ServiceConnection() {


        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (MqttService.MqttBinder) iBinder;
            binder.getService().setmMqttServiceCallback(new MqttService.MqttServiceCallback() {
                @Override
                public void onDisconnect() {
                    // 每30秒重试一次
                    if (reconnectTimer != null) {
                        reconnectTimer.cancel();
                        reconnectTimer.purge();
                        reconnectTimer = null;
                    }

                    if (view != null) {
                        view.onMqttDisconnected();
                    }

                    reconnectTimer = new Timer();
                    reconnectTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // 重连必须重新给一个clientId，否则直接失败
                            binder.init();
                            binder.connect();
                        }
                    }, 30 * 1000);
                }

                @Override
                public void onConnect() {
                    if (reconnectTimer != null) {
                        reconnectTimer.cancel();
                        reconnectTimer.purge();
                        reconnectTimer = null;
                    }

                    binder.subscribeMember(Constants.imId);
                    if (view != null) {
                        view.onMqttConnected();
                    }
                }
            });

            binder.init();
            binder.connect();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (reconnectTimer != null) {
                reconnectTimer.cancel();
                reconnectTimer.purge();
                reconnectTimer = null;
            }
        }
    };

    @Override
    public void doConnectMqtt(final Activity activity) {
        if (activity == null) {
            //异常
            return;
        }
        getImHostRequest(new GetImHostCallBack() {
            @Override
            public void onSuccess(String host) {
                // TODO: 2018/5/23  出现一次 crash  
                Intent intent = new Intent(activity, MqttService.class);
                intent.putExtra("host", host);
                activity.bindService(intent, mqttServiceConnection, BIND_AUTO_CREATE);
            }
        });
    }

    @Override
    public void doDisConnectMqtt(final Activity activity) {
        if (activity != null) {
            //打开 app 在还没有 bind service 就执行杀进程操作
            //认为 binder 不为空  mqttService 已经绑定
            if (binder != null) {
                //保险起见  try catch 一下
                try {
                    activity.unbindService(mqttServiceConnection);
                } catch (Exception e) {
                    YXLogger.e("MQTT",e.getMessage());
                }
            }
        }

    }
}
