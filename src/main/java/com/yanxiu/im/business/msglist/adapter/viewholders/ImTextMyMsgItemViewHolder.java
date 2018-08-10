package com.yanxiu.im.business.msglist.adapter.viewholders;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.business.msglist.adapter.viewholders.base.ImBaseMyMsgItemViewHolder;
import com.yanxiu.im.business.utils.ImDateFormateUtils;
import com.yanxiu.im.sender.ISender;
import com.yanxiu.im.sender.ISenderListener;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * create by 朱晓龙 2018/5/12 下午1:13
 */
public class ImTextMyMsgItemViewHolder extends ImBaseMyMsgItemViewHolder {

    public ImTextMyMsgItemViewHolder(View itemView, Context mContext) {
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

    private TextView mMsgTextView;

    @Override
    protected void initView(View item) {
        senderAraval =item.findViewById(R.id.sender_avaral);


        mMsgTextView = item.findViewById(R.id.msg_textview);
        bottomPaddingView=itemView.findViewById(R.id.bottom_padding_view);
        dateTime = item.findViewById(R.id.date);
        sendingPb = item.findViewById(R.id.state_sending_progressbar);
        failFlag = item.findViewById(R.id.state_fail_imageview);
    }

    @Override
    protected void initListener() {
        //我本人的消息

        dateTime.setOnClickListener(this);
        senderAraval.setOnClickListener(this);
        mMsgTextView.setOnClickListener(this);
        sendingPb.setOnClickListener(this);
        failFlag.setOnClickListener(this);
    }

    private MsgItemBean currentMsgBean;

    @Override
    public void setData(MsgItemBean msgData) {
        if (msgData == null) {
            YXLogger.e(getClass().getSimpleName()," MsgItemBean 为空");
            return;
        }
        currentMsgBean = msgData;
        loadAvaral(Constants.imAvatar);
        mMsgTextView.setText(msgData.getMsg());
        if (msgData.getISender() != null) {
            msgData.getISender().addSenderListener(mSenderListener);
        }
        if (msgData.getState()==0) {
            //消息 为发送成功状态
            setMsgSuccessState();
        }else if (msgData.getState()==1){
            setMsgProgress();
            //消息 为发送中
        }else if (msgData.getState()==2){
            //消息 为发送发失败状态
            setMsgFailState();
        }
        dateTime.setText(ImDateFormateUtils.timeStrWithTime(msgData.getSendTime()));
        dateTime.setVisibility(msgData.isShowDate()?VISIBLE:GONE);
    }
    private ISenderListener mSenderListener=new ISenderListener() {
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
            setMsgProgress();
        }
    };
    @Override
    public void preSizeContentView(int w, int h) {}


    private void setMsgSuccessState() {
        //状态 view
        sendingPb.setVisibility(GONE);
        failFlag.setVisibility(GONE);
    }
    private void setMsgProgress(){
        if (sendingPb.getVisibility()==GONE) {
            sendingPb.setVisibility(VISIBLE);
        }
        if (failFlag.getVisibility()==VISIBLE) {
            failFlag.setVisibility(GONE);
        }
    }


    private void setMsgFailState() {
        //状态 view
        sendingPb.setVisibility(GONE);
        failFlag.setVisibility(VISIBLE);
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (mBaseMyMsgItemViewClickListener == null) {
            return;
        }
        if (id == R.id.msg_textview) {
            //点击了文字
            mBaseMyMsgItemViewClickListener.onMsgContentClicked();
        } else if (id == R.id.date) {
            //点击了日期
        } else if ( id == R.id.sender_avaral) {
            //点击了发送者头像或名字
        } else if (id == R.id.state_sending_progressbar) {
            //点击 sending progressbar
            mBaseMyMsgItemViewClickListener.onSendingPbClicked();
        } else if (id == R.id.state_fail_imageview) {
            //点击 发送失败标志  ->重发操邹
            mBaseMyMsgItemViewClickListener.onFailFlagClicked();
        }
    }
}
