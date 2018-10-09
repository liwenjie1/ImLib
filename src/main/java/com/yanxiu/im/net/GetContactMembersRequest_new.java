package com.yanxiu.im.net;

import com.test.yanxiu.common_base.utils.UrlRepository;
import com.yanxiu.im.Constants;

public class GetContactMembersRequest_new extends ImRequestBase_new {

    public String method="app.manage.sysUser.getClazsMember";
    public String clazsId;
    public String offset;
    public String keyWords;
    public String pageSize="10";
    public String token;

    public String osType = Constants.osType;
    public String pcode = Constants.pcode;
    public String trace_uid;
    public String version = Constants.version;

    @Override
    protected String urlPath() {
        return null;
    }

    @Override
    protected String urlServer() {
        //app层的  server 地址
        return UrlRepository.getInstance().getServer();
    }
}
