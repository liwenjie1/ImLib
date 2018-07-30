package com.yanxiu.im.sender;

/**
 * 该接口主要是定义Im发送回调
 * Created by 杨小明 on 2018/5/7.
 */

public interface ISenderListener {
    //发送成功
    void OnSuccess(ISender sender);

    //发送失败
    void OnFail(ISender sender);

    //发送进度
    void OnProgress(double progress);
}
