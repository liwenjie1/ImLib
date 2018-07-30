package com.yanxiu.im.net;

import com.test.yanxiu.common_base.utils.UrlRepository;


/**
 * 1.获取七牛上传token接口
 * Created by frc on 2018/1/22.
 * wiki :http://wiki.yanxiu.com/pages/viewpage.action?pageId=12322622
 */

public class GetQiNiuTokenRequest_new extends ImRequestBase_new {
    private String method = "upload.token";
    public String type;
    public String size;
    public String name;
    public String token;
    public String lastModifiedDate;
    public String shareType;
    //必填
    public String dtype;
    public String from;


    @Override
    protected String urlPath() {
        return null;
    }

    @Override
    protected String urlServer() {
        return UrlRepository.getInstance().getQiNiuServer();
    }
}
