package com.yanxiu.im.business.msglist.adapter.viewholders.base;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.yanxiu.im.bean.MsgItemBean;

/**
 * create by 朱晓龙 2018/5/12 下午12:25
 */
public abstract class ImBaseMsgItemViewHolder<E extends MsgItemBean> extends AbstractViewHolder<E>{

    protected TextView senderName;
    public ImBaseMsgItemViewHolder(View itemView, Context mContext) {
        super(itemView, mContext);
    }
}
