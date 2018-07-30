package com.yanxiu.im.net;

import com.yanxiu.im.bean.net_bean.ImDataForUpdateMemberInfo_new;

/**
 * Created by srt on 2018/3/27.
 */

public class GetTopicMembersInfoResponse_new extends ImResponseBase_new {



    private ImDataForUpdateMemberInfo_new data;

    public ImDataForUpdateMemberInfo_new getData() {
        return data;
    }

    public void setData(ImDataForUpdateMemberInfo_new data) {
        this.data = data;
    }
}
