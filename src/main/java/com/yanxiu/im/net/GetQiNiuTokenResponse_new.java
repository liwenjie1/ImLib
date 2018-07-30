package com.yanxiu.im.net;


/**
 * 1.获取七牛上传token接口
 * Created by frc on 2018/1/22.
 * wiki :http://wiki.yanxiu.com/pages/viewpage.action?pageId=12322622
 */

public class GetQiNiuTokenResponse_new extends ImResponseBase_new {


    private DataBean data;

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {

        private String uid;
        private String uname;
        private String host;
        private String token;

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getUname() {
            return uname;
        }

        public void setUname(String uname) {
            this.uname = uname;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
