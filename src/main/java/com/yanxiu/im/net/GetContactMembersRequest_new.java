package com.yanxiu.im.net;

public class GetContactMembersRequest_new extends ImRequestBase_new {

    public String method = "app.manage.sysUser.getClazsMember";
    public String clazsId;
    public String offset;
    public String keyWords;
    public String pageSize = "10";

    @Override
    protected String urlPath() {
        return null;
    }

}
