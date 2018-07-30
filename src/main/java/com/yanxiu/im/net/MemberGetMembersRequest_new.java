package com.yanxiu.im.net;

/**
 * Created by cailei on 02/03/2018.
 */

// 2.2 获取im用户信息
public class MemberGetMembersRequest_new extends ImRequestBase_new {
    private String method = "member.getMembers";

    public String imMemberIds;      // IM用户id，多个用逗号分隔
}
