package com.yanxiu.im.net;

import com.yanxiu.im.bean.net_bean.ImMember_new;

import java.util.List;

/**
 * Created by cailei on 02/03/2018.
 */

public class MemberGetMembersResponse_new extends ImResponseBase_new {
    public Data data;

    public class Data {
        public long imEvent;
        public String reqId;
        // 还有一堆不确定干什么用，暂时没写

        public List<ImMember_new> members;

    }
}
