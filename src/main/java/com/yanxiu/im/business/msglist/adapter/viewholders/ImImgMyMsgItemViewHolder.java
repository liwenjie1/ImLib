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
import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.business.msglist.adapter.viewholders.base.ImBaseMyMsgItemViewHolder;
import com.yanxiu.im.business.utils.ImDateFormateUtils;
import com.yanxiu.im.business.utils.ImageFileUtils;
import com.yanxiu.im.business.view.ProgressImageContainer_new;
import com.yanxiu.im.sender.ISender;
import com.yanxiu.im.sender.ISenderListener;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * create by 朱晓龙 2018/5/12 下午1:14
 */
public class ImImgMyMsgItemViewHolder extends ImBaseMyMsgItemViewHolder {


    private ProgressImageContainer_new mProgressImageContainerNew;

    public ImImgMyMsgItemViewHolder(View itemView, Context mContext) {
        super(itemView, mContext);
    }

    @Override
    public void recycleSenderListener() {
        if (currentMsgBean != null && currentMsgBean.getISender() != null) {
            currentMsgBean.getISender().removeSenderListener(mSenderListener);
        }
        setMsgSuccessState();
    }

    @Override
    public void showBottomPadding(boolean show) {
        bottomPaddingView.setVisibility(show?VISIBLE:GONE);
    }

    @Override
    protected void initView(View item) {
        senderAraval = item.findViewById(R.id.sender_avaral);
        dateTime = item.findViewById(R.id.date);
        mProgressImageContainerNew = item.findViewById(R.id.msg_imageView);
        sendingPb = item.findViewById(R.id.state_sending_progressbar);
        failFlag = item.findViewById(R.id.state_fail_imageview);
        bottomPaddingView=itemView.findViewById(R.id.bottom_padding_view);
    }

    @Override
    protected void initListener() {
        //我本人的消息
        dateTime.setOnClickListener(this);
        senderAraval.setOnClickListener(this);
        mProgressImageContainerNew.setOnClickListener(this);
        sendingPb.setOnClickListener(this);
        failFlag.setOnClickListener(this);
    }

    private MsgItemBean currentMsgBean;

    //绑定 msg 信息到 view 并设置状态回调
    @Override
    public void setData(MsgItemBean msgData) {
        //本次新 msg 的状态设定

        if (msgData == null) {
            YXLogger.e(getClass().getSimpleName()," MsgItemBean 为空");
            return;
        }
        currentMsgBean = msgData;
        loadAvaral(Constants.imAvatar);
        Integer[] size = ImageFileUtils.getPicShowWH(getContext(), msgData.getWidth(), msgData.getHeight());
        int showW = size[0];
        int showH = size[1];
        preSizeContentView(showW, showH);

        String url = null;
        //判断本地图片是否存在
        if (ImageFileUtils.isImgFileExsist(msgData.getLocalViewUrl())) {
            url = msgData.getLocalViewUrl();
        } else {
            url = msgData.getViewUrl();
        }
        loadImage(url, showW, showH);
        //设置发送状态回调
        if (msgData.getISender() != null) {
            msgData.getISender().addSenderListener(mSenderListener);
        }
        if (msgData.getState() == 0) {
            //消息 为发送成功状态
            setMsgSuccessState();
        } else if (msgData.getState() == 1) {
            //消息 为发送中
            setMsgProgressState((int) (msgData.getProgress() * 100));
        } else if (msgData.getState() == 2) {
            //消息 为发送发失败状态
            setMsgFailState();
        }
        dateTime.setText(ImDateFormateUtils.timeStrWithTime(msgData.getSendTime()));
        dateTime.setVisibility(msgData.isShowDate() ? VISIBLE : GONE);
    }

    @Override
    public void preSizeContentView(int w, int h) {
        ViewGroup.LayoutParams layoutParams = mProgressImageContainerNew.getLayoutParams();
        layoutParams.width = w;
        layoutParams.height = h;
        mProgressImageContainerNew.setLayoutParams(layoutParams);
    }

    private ISenderListener mSenderListener = new ISenderListener() {
        @Override
        public void OnSuccess(ISender sender) {
            setMsgSuccessState();
        }

        @Override
        public void OnFail(ISender sender) {
            setMsgFailState();
        }

        @Override
        public void OnProgress(double progress) {
            setMsgProgressState((int) (progress * 100));
        }
    };



    private void setMsgSuccessState() {
        //状态 view
        sendingPb.setVisibility(GONE);
        failFlag.setVisibility(GONE);
        //progress
        mProgressImageContainerNew.setProgress(100);
        mProgressImageContainerNew.clearOverLayer();
    }

    private void setMsgProgressState(int progress) {
        if (sendingPb.getVisibility() == GONE) {
            sendingPb.setVisibility(VISIBLE);
        }
        if (failFlag.getVisibility() == VISIBLE) {
            failFlag.setVisibility(GONE);
        }
        mProgressImageContainerNew.setProgress(progress);
    }

    private void setMsgFailState() {
        //状态 view
        if (sendingPb.getVisibility() == VISIBLE) {
            sendingPb.setVisibility(GONE);
        }
        if (failFlag.getVisibility() == GONE) {
            failFlag.setVisibility(VISIBLE);
        }

        //progress
        mProgressImageContainerNew.setProgress(0);
        mProgressImageContainerNew.clearOverLayer();
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
        if (id == R.id.msg_imageView) {
            //点击了图片消息
            mBaseMyMsgItemViewClickListener.onMsgContentClicked();
        } else if (id == R.id.date) {
            //点击了日期
        } else if (id == R.id.sender_avaral) {
            //点击了发送者头像或名字
            mBaseMyMsgItemViewClickListener.onSenderClicked();
        } else if (id == R.id.state_sending_progressbar) {
            //点击 sending progressbar
            mBaseMyMsgItemViewClickListener.onSendingPbClicked();
        } else if (id == R.id.state_fail_imageview) {
            //点击 发送失败标志  ->重发操邹
            mBaseMyMsgItemViewClickListener.onFailFlagClicked();
        }
    }
}
