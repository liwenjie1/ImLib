package com.yanxiu.im.business.utils;

import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.db.DbMyMsg;
import com.yanxiu.im.manager.DatabaseManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by 朱晓龙 on 2018/5/11 13:51.
 * 负责处理 时间日期的格式化
 */

public class ImDateFormateUtils {
    public  static String timeStr(long timestamp) {
        String ret = null;
        Date now = new Date();
        Date date = new Date(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        String nowStr = formatter.format(now);
        String dateStr = formatter.format(date);
        // 由于server time可能有误差，所有未来时间也当做今天
        if ((nowStr.equals(dateStr)) || (date.getTime() > now.getTime())) {
            // 在同一天，显示"上午 10:36"
            SimpleDateFormat dateFormat = new SimpleDateFormat("a hh:mm", Locale.CHINA);
            ret = dateFormat.format(date);
            return ret;
        }

        Date nowZero = null; // 今天零点
        try {
            nowZero = formatter.parse(nowStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if ((nowZero.getTime() - date.getTime()) < 24 * 60 * 60 * 1000) {
            // 昨天 显示"昨天 19:36"
            SimpleDateFormat dateFormat = new SimpleDateFormat(" HH:mm", Locale.CHINA);
            ret = dateFormat.format(date);
            ret = "昨天"+ret;
            return ret;
        }
        //如果日期小于6天 显示星期
        if ((nowZero.getTime()-date.getTime())<6*24*60*60*1000){
            // 星期三 周三->星期三 19:60
            SimpleDateFormat formatter2 = new SimpleDateFormat("EEEE", Locale.CHINA);
            ret = formatter2.format(date);
            return ret;
        }

        //时间早于6天  显示具体日期 19:60
        SimpleDateFormat format=new SimpleDateFormat("MM月dd日",Locale.CHINA);
        ret=format.format(date);
        return ret;
    }


    public  static String timeStrWithTime(long timestamp) {
        String ret = null;
        Date now = new Date();
        Date date = new Date(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        String nowStr = formatter.format(now);
        String dateStr = formatter.format(date);
        // 由于server time可能有误差，所有未来时间也当做今天
        if ((nowStr.equals(dateStr)) || (date.getTime() > now.getTime())) {
            // 在同一天，显示"上午 10:36"
            SimpleDateFormat dateFormat = new SimpleDateFormat("a hh:mm", Locale.CHINA);
            ret = dateFormat.format(date);
            return ret;
        }

        Date nowZero = null; // 今天零点
        try {
            nowZero = formatter.parse(nowStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if ((nowZero.getTime() - date.getTime()) < 24 * 60 * 60 * 1000) {
            // 昨天 显示"昨天 19:36"
            SimpleDateFormat dateFormat = new SimpleDateFormat(" HH:mm", Locale.CHINA);
            ret = dateFormat.format(date);
            ret = "昨天"+ret;
            return ret;
        }
        //如果日期小于6天 显示星期
        if ((nowZero.getTime()-date.getTime())<6*24*60*60*1000){
            // 星期三 周三->星期三 19:60
            SimpleDateFormat formatter2 = new SimpleDateFormat("EEEE HH:mm", Locale.CHINA);
            ret = formatter2.format(date);
            return ret;
        }

        //时间早于6天  显示具体日期 19:60
        SimpleDateFormat format=new SimpleDateFormat("MM月dd日 HH:mm",Locale.CHINA);
        ret=format.format(date);
        return ret;
    }


    public static MsgItemBean createTextMsgBean(String msgStr, TopicItemBean belongTopic){
        MsgItemBean msgItemBean = new MsgItemBean(MsgItemBean.MSG_TYPE_MYSELF, 10);
        msgItemBean.setState(DbMyMsg.State.Sending.ordinal());
        msgItemBean.setLocalViewUrl("");
        msgItemBean.setMsgId(belongTopic.generateMyMsgId());
        msgItemBean.setTopicId(belongTopic.getTopicId());
        msgItemBean.setMsg(msgStr);
        msgItemBean.setSenderId(Constants.imId);
        msgItemBean.setReqId(UUID.randomUUID().toString());
        msgItemBean.setSendTime(System.currentTimeMillis());

        belongTopic.setLatestMsgTime(msgItemBean.getSendTime());
        //保存数据库
        DatabaseManager.createOrUpdateMyMsg(msgItemBean);
        return msgItemBean;
    }
    public static MsgItemBean createImgMsgBean(String imgUrl, TopicItemBean belongTopic) {
        MsgItemBean msgItemBean = new MsgItemBean(MsgItemBean.MSG_TYPE_MYSELF, 20);
        msgItemBean.setState(DbMyMsg.State.Sending.ordinal());
        msgItemBean.setLocalViewUrl(imgUrl);
        msgItemBean.setMsgId(belongTopic.generateMyMsgId());
        msgItemBean.setTopicId(belongTopic.getTopicId());
        msgItemBean.setMsg("");
        msgItemBean.setSenderId(Constants.imId);
        msgItemBean.setReqId(UUID.randomUUID().toString());
        msgItemBean.setSendTime(System.currentTimeMillis());
        Integer[] size = ImageFileUtils.getPicWithAndHeight(imgUrl);
        msgItemBean.setWidth(size[0]);
        msgItemBean.setHeight(size[1]);

        belongTopic.setLatestMsgTime(msgItemBean.getSendTime());
        //保存数据库
        DatabaseManager.createOrUpdateMyMsg(msgItemBean);
        return msgItemBean;
    }

}
