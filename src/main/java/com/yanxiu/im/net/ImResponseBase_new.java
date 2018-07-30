package com.yanxiu.im.net;

/**
 * im的respone基类
 * Created by cailei on 02/03/2018.
 */

public class ImResponseBase_new {
    public int code;
    public String message;

    public Object currentUser;
    public Error error;

    public class Error {
        public int code;
        public String title;
        public String message;
    }
}
