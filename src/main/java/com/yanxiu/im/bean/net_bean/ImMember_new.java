package com.yanxiu.im.bean.net_bean;

import com.google.gson.annotations.SerializedName;

/**
 * Created by cailei on 02/03/2018.
 */

public class ImMember_new {
    @SerializedName("id")
    public long imId;       // im体系内用的id
    public long userId;     // app体系内用的id
    public String memberName;
    public String avatar;   // 头像url

    // 以下暂时不知道干什么用
    public int memberType;
    public int state;
}
