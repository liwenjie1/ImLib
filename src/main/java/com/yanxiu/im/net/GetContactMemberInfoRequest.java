package com.yanxiu.im.net;

import com.test.yanxiu.common_base.utils.UrlRepository;

public class GetContactMemberInfoRequest extends ImRequestBase_new {

    public String method = "sysUser.userInfo";
    public String userId;

    @Override
    protected boolean shouldLog() {
        return false;
    }

    @Override
    protected String urlServer() {
        return UrlRepository.getInstance().getChooseClassServer();
    }

    @Override
    protected String urlPath() {
        return null;
    }
}
