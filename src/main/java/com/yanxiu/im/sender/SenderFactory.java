package com.yanxiu.im.sender;


import com.yanxiu.im.bean.MsgItemBean;

/**
 * 用于创建不同类型sender
 * Created by Hu Chao on 18/5/15.
 */
public class SenderFactory {

    /**
     * 根据msg类型创建不同的sender
     *
     * @param bean
     * @return
     */
    public static ISender createSender(MsgItemBean bean) {
        ISender sender = null;
        switch (bean.getContentType()) {
            case 10:
                sender = new TextSender(bean);
                break;
            case 20:
                sender = new ImageSender(bean);
                break;
            default:
                break;
        }
        return sender;
    }

}
