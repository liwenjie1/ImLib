package com.yanxiu.im.business.topiclist.adapter.topicviewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;

/**
 * create by 朱晓龙 2018/7/26 下午3:47
 */
public abstract class ImTopicBaseViewHolder<E extends TopicItemBean> extends RecyclerView.ViewHolder {
    public ImTopicBaseViewHolder(View itemView) {
        super(itemView);
    }


    public abstract void setData(E data);

    public abstract void setGroupTopicData(E groupData);

    public abstract void setPrivateTopicData(E abstractData);

    public abstract void setLatestMsgInfo(MsgItemBean latestMsg, boolean showSenderName);

    //目前没有sender信息 重置
    public abstract void resetView();


    /**
     * 对于私聊topic 加载 member头像
     */
    public abstract void loadTopicAvaral(ImageView imageView, String imgUrl);
}
