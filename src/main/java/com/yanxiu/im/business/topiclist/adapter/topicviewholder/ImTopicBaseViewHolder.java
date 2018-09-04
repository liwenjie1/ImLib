package com.yanxiu.im.business.topiclist.adapter.topicviewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.lib.yx_basic_library.util.YXScreenUtil;

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

    public void setItemVisiable(boolean visiable){
        RecyclerView.LayoutParams param = (RecyclerView.LayoutParams) itemView.getLayoutParams();
        if (visiable) {
            param.height = YXScreenUtil.dpToPxInt(itemView.getContext(),71);// 这里注意使用自己布局的根布局类型
            param.width = RelativeLayout.LayoutParams.MATCH_PARENT;// 这里注意使用自己布局的根布局类型
            itemView.setVisibility(View.VISIBLE);
        } else {
            itemView.setVisibility(View.GONE);
            param.height = 0;
            param.width = 0;
        }
        itemView.setLayoutParams(param);
    }

    //目前没有sender信息 重置
    public abstract void resetView();


    /**
     * 对于私聊topic 加载 member头像
     */
    public abstract void loadTopicAvaral(ImageView imageView, String imgUrl);
}
