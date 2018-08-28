package com.yanxiu.im.business.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.util.List;

/**
 * create by 朱晓龙 2018/8/28 上午9:49
 */
public class ProcessUtils {
    private static final String TAG="ProcessUtils";
    public static String getProcessName(Context context) {
        int pid = Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            Log.i(TAG, "getProcessName: "+procInfo.processName);
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }

    /**
     * 这会获取 手机上的所有运行中的服务
     * */
    public static void getRunningServices(Context context){
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo process : runningAppProcesses) {
//            Log.i(TAG, "getRunningServices: process "+process.processName);
        }
        final List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(50);
        for (ActivityManager.RunningServiceInfo runningService : runningServices) {
            Log.i(TAG, "getRunningServices: service "+runningService.service);
        }
    }

}
