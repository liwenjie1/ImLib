package com.yanxiu.im.business.msglist.interfaces;

import com.yanxiu.im.bean.TopicItemBean;

/**
 * Created by 朱晓龙 on 2018/6/6 10:52.
 */

public interface ImSettingContract {


    interface IView{
        void onSetSilent(boolean silent);
        void onSetNotice(boolean notice);

        void onTopicFound(TopicItemBean topicBean);
    }


    interface IPresenter{
        /**
         * 执行禁言设置
         * */
        void dosetSilent(long topicId,boolean silent);
        /**
         * 执行 免打扰设置
         *
         */
        void dosetNotice(long topicId,boolean shouldNotice);


        void doGetTopicInfo(long topicId);
    }

}
