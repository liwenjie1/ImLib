package com.yanxiu.im.business.msglist.adapter.viewholders.base;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * create by 朱晓龙 2018/5/12 下午1:11
 */
public  abstract class ImBaseMyMsgItemViewHolder extends AbstractViewHolder {

    protected ImageView failFlag;
    protected ProgressBar sendingPb;

    public ImBaseMyMsgItemViewHolder(View itemView, Context mContext) {
        super(itemView, mContext);
    }


}
