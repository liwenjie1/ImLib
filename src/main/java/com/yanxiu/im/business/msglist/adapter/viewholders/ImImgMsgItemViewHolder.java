package com.yanxiu.im.business.msglist.adapter.viewholders;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.business.msglist.adapter.viewholders.base.ImBaseMsgItemViewHolder;
import com.yanxiu.im.business.utils.ImDateFormateUtils;
import com.yanxiu.im.business.utils.ImageFileUtils;
import com.yanxiu.im.business.view.ProgressImageContainer_new;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * create by 朱晓龙 2018/5/12 下午1:14
 */
public class ImImgMsgItemViewHolder extends ImBaseMsgItemViewHolder<MsgItemBean> {


    public ImImgMsgItemViewHolder(View itemView, Context mContext) {
        super(itemView, mContext);
    }

    @Override
    public void setData(MsgItemBean msgData) {
        if (msgData == null) {
            YXLogger.e(getClass().getSimpleName()," MsgItemBean 为空");
            return;
        }
        DbMember sender = msgData.getMember();
        if (sender != null) {
            loadAvaral(sender.getAvatar());
            senderName.setText(sender.getName());
        }else {
            loadAvaral("");
            senderName.setText("");
        }

        Integer[] size= ImageFileUtils.getPicShowWH(getContext(),msgData.getWidth(),msgData.getHeight());
        int showW=size[0];
        int showH=size[1];
        preSizeContentView(showW,showH);
        loadImage(msgData.getViewUrl(),showW,showH);
        dateTime.setText(ImDateFormateUtils.timeStrWithTime(msgData.getSendTime()));
        dateTime.setVisibility(msgData.isShowDate() ? VISIBLE : GONE);
    }
    private ProgressImageContainer_new mProgressImageContainerNew;

    @Override
    protected void initView(View itemView) {
        senderAraval = itemView.findViewById(R.id.sender_avaral);
        senderName = itemView.findViewById(R.id.sender_name);
        dateTime = itemView.findViewById(R.id.date);
        mProgressImageContainerNew = itemView.findViewById(R.id.msg_imageView);
        bottomPaddingView=itemView.findViewById(R.id.bottom_padding_view);
    }

    @Override
    protected void initListener() {
        senderAraval.setOnClickListener(this);
        senderName.setOnClickListener(this);
        mProgressImageContainerNew.setOnClickListener(this);
    }


    @Override
    public void preSizeContentView(int w, int h) {
        ViewGroup.LayoutParams layoutParams = mProgressImageContainerNew.getLayoutParams();
        layoutParams.width=w;
        layoutParams.height=h;
        mProgressImageContainerNew.setLayoutParams(layoutParams);
    }

    @Override
    public void recycleSenderListener() {}

    @Override
    public void showBottomPadding(boolean show) {
        bottomPaddingView.setVisibility(show?VISIBLE:GONE);
    }

    private void loadAvaral(String url) {
        Glide.with(getContext())
                .load(url)
                .centerCrop()
                .dontAnimate()
                .dontTransform()
                .placeholder(R.drawable.im_chat_default)
                .error(R.drawable.im_chat_default)
                .into(senderAraval);
    }

    private void loadImage(String url, int w, int h) {
        Glide.with(getContext())
                .load(url)
                .centerCrop()
                .dontAnimate()
                .dontTransform()
                .override(w, h)
                .transform(mCornersTransform)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.im_pic_holder_view_bg)
                .error(R.drawable.im_pic_holder_view_bg)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        //发生异常  保证 显示没关 设置灰色背景 与 研修宝 logn 图片显示
                        mProgressImageContainerNew.mRoundCornerImage.setBackgroundResource(R.drawable.im_pic_holder_bg);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        //由于成功获取 了 图片资源 可以 取消灰色背景
                        mProgressImageContainerNew.mRoundCornerImage.setBackgroundColor(Color.TRANSPARENT);
                        return false;
                    }
                })
                .into(mProgressImageContainerNew.mRoundCornerImage);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (mBaseMyMsgItemViewClickListener == null) {
            return;
        }
        if (id == R.id.sender_avaral || id == R.id.sender_name) {
            mBaseMyMsgItemViewClickListener.onSenderClicked();
        } else if (id == R.id.msg_imageView) {
            mBaseMyMsgItemViewClickListener.onMsgContentClicked();
        }
    }
}
