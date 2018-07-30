package com.yanxiu.im.business.msglist.interfaces.impls;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.test.yanxiu.common_base.utils.SharedSingleton;
import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.msglist.interfaces.ImSettingContract;
import com.yanxiu.im.business.utils.TopicInMemoryUtils;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.db.ImSpManager;

import java.util.List;
import java.util.Random;

/**
 * Created by 朱晓龙 on 2018/6/6 10:54.
 */

public class ImSettingPresetner implements ImSettingContract.IPresenter {

    /**
     * 模拟网络接口
     */
    private Handler mockHttpHandler = new Handler(Looper.getMainLooper());
    private Random mRandom = new Random();


    private ImSettingContract.IView mIView;

    public ImSettingPresetner(ImSettingContract.IView IView) {
        mIView = IView;
    }

    /**
     * 检查当前用户是否有禁言权限 前提是 管理端登录
     * 检查当前用户在目标topic 的角色
     * */
    public boolean checkCurrentUserRole(long topicId){
        List<TopicItemBean> topics=SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        TopicItemBean targetTopic = TopicInMemoryUtils.findTopicByTopicId(topicId, topics);
        if (targetTopic == null) {
            return false;
        }

        if (targetTopic.getMembers() == null) {
            return false;
        }

        for (DbMember dbMember : targetTopic.getMembers()) {
            if (dbMember.getImId()== Constants.imId) {
              return  dbMember.getRole()==0;
            }
        }
        return false;
    }

    /**
     * 执行禁言设置
     *
     * @param silent
     */
    @Override
    public void dosetSilent(boolean silent) {
        // TODO: 2018/6/6 网络请求
        mockHttpHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mIView.onSetSilent(mRandom.nextBoolean());
            }
        }, 200);


    }

    /**
     * 执行 免打扰设置
     *
     * @param shouldNotice
     */
    @Override
    public void dosetNotice(boolean shouldNotice) {
        // TODO: 2018/6/6 网络请求
        mockHttpHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mIView.onSetNotice(mRandom.nextBoolean());
            }
        }, 200);
    }

    @Override
    public void doGetTopicInfo(long topicId) {
        List<TopicItemBean> topics = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        TopicItemBean targetTopic = TopicInMemoryUtils.findTopicByTopicId(topicId, topics);
        if (targetTopic != null) {
            mIView.onTopicFound(targetTopic);
        }
    }

    public boolean getNoticeSetting() {
        String seting = ImSpManager.getInstance().getImSetting(ImSpManager.SETTING_PUSH);
        if (!TextUtils.isEmpty(seting)) {
            return Boolean.parseBoolean(seting);
        }
        return false;
    }

    public boolean getSilentSetting() {
        String seting = ImSpManager.getInstance().getImSetting(ImSpManager.SETTING_SILENT);
        if (!TextUtils.isEmpty(seting)) {
            return Boolean.parseBoolean(seting);
        }
        return false;
    }
}
