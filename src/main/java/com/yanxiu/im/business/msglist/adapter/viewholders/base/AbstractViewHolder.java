package com.yanxiu.im.business.msglist.adapter.viewholders.base;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.business.msglist.CornersTransform;

/**
 * Created by 朱晓龙 on 2018/5/2 18:50.
 */

public abstract class AbstractViewHolder<E extends MsgItemBean> extends RecyclerView.ViewHolder implements View.OnClickListener{

    public AbstractViewHolder(View itemView, Context mContext) {
        super(itemView);
        this.mContext = mContext;
        initView(itemView);
        initListener();
        mCornersTransform=new CornersTransform(mContext,15);
    }

    protected Context mContext;
    protected ImageView senderAraval;
    protected TextView dateTime;
    protected View bottomPaddingView;
    protected CornersTransform mCornersTransform;

    protected BaseMyMsgItemViewClickListener mBaseMyMsgItemViewClickListener;
    abstract protected void initView(View itemView);
    abstract protected void initListener();
    abstract public void setData(E data);
    abstract public void preSizeContentView(int w,int h);
    abstract public void recycleSenderListener();
    abstract public void showBottomPadding(boolean show);
    public void setBaseMyMsgItemViewClickListener(BaseMyMsgItemViewClickListener baseMyMsgItemViewClickListener) {
        mBaseMyMsgItemViewClickListener = baseMyMsgItemViewClickListener;
    }

    protected Context getContext(){
        return mContext;
    }



    /**
     * 基本的 点击监听
     * */
    public interface BaseMyMsgItemViewClickListener{
        void onFailFlagClicked();
        void onSendingPbClicked();
        void onMsgContentClicked();
        void onSenderClicked();
    }
}
