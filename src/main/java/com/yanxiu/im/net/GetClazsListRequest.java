package com.yanxiu.im.net;

import com.test.yanxiu.common_base.utils.UrlRepository;

public class GetClazsListRequest extends ImRequestBase_new {
    public String method = "app.clazs.getStudentClazses";


//    public String osType = Constants.osType;
//    public String pcode = Constants.pcode;
    public String token;
//    public String trace_uid;
//    public String version = Constants.version;

    @Override
    protected String urlServer() {
        return UrlRepository.getInstance().getChooseClassServer();
    }

    @Override
    protected String urlPath() {
        return null;
    }
}
