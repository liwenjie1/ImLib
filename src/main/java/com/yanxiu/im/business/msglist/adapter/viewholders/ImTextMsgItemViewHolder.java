package com.yanxiu.im.business.msglist.adapter.viewholders;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.test.yanxiu.common_base.utils.EscapeCharacterUtils;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.business.msglist.adapter.viewholders.base.ImBaseMsgItemViewHolder;
import com.yanxiu.im.business.utils.ImDateFormateUtils;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * create by 朱晓龙 2018/5/12 下午1:12
 */
public class ImTextMsgItemViewHolder extends ImBaseMsgItemViewHolder {


    private TextView msgTextView;

    public ImTextMsgItemViewHolder(View itemView, Context mContext) {
        super(itemView, mContext);
    }


    @Override
    protected void initView(View itemView) {
        senderAraval = itemView.findViewById(R.id.sender_avaral);
        senderName = itemView.findViewById(R.id.sender_name);
        dateTime = itemView.findViewById(R.id.date);
        msgTextView = itemView.findViewById(R.id.msg_textview);
        bottomPaddingView=itemView.findViewById(R.id.bottom_padding_view);
    }

    @Override
    protected void initListener() {
        senderAraval.setOnClickListener(this);
        senderName.setOnClickListener(this);
        msgTextView.setOnClickListener(this);
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
            YXLogger.e(getClass().getSimpleName()," MsgItemBean sender信息为空");
            loadAvaral("");
            senderName.setText("");
        }

        msgTextView.setText(EscapeCharacterUtils.unescape(msgData.getMsg()));
        dateTime.setText(ImDateFormateUtils.timeStrWithTime(msgData.getSendTime()));
        dateTime.setVisibility(msgData.isShowDate() ? VISIBLE : GONE);
    }

    @Override
    public void preSizeContentView(int w, int h) {}

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


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (mBaseMyMsgItemViewClickListener == null) {
            return;
        }
        if (id == R.id.sender_avaral || id == R.id.sender_name) {
            mBaseMyMsgItemViewClickListener.onSenderClicked();
        } else if (id == R.id.msg_textview) {
            mBaseMyMsgItemViewClickListener.onMsgContentClicked();
        }
    }
}
