package com.yanxiu.im.business.topiclist.adapter.topicviewholder;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.utils.ImDateFormateUtils;
import com.yanxiu.im.business.view.CircleView;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.lib.yx_basic_library.util.logger.YXLogger;

/**
 * create by 朱晓龙 2018/7/26 下午3:36
 */
public class ImGroupTopicViewHolder<E extends TopicItemBean> extends ImTopicBaseViewHolder<E> {
    private TextView topicNameTv;
    private TextView latestMsgContent;
    private TextView latestMsgTime;
    private ImageView topicImage;
    private CircleView redDot;
    private ImageView quiteImageView;

    public ImGroupTopicViewHolder(@NonNull View itemView) {
        super(itemView);
        topicImage = itemView.findViewById(R.id.avatar_imageview);
        latestMsgTime = itemView.findViewById(R.id.time_textView);
        latestMsgContent = itemView.findViewById(R.id.msg_textview);
        topicNameTv = itemView.findViewById(R.id.sender_textview);
        redDot = itemView.findViewById(R.id.reddot_circleview);
        quiteImageView = itemView.findViewById(R.id.im_quite_icon);
    }

    @Override
    public void setData(E data) {
        //判断topic 类型
        setGroupTopicData(data);
        //获取最新一条消息
        MsgItemBean latestMsg = data.getLatestMsg();
        setLatestMsgInfo(latestMsg, true);
        redDot.setVisibility(data.isShowDot() ? View.VISIBLE : View.INVISIBLE);
        quiteImageView.setVisibility(data.isBlockNotice() ? View.VISIBLE : View.INVISIBLE);

    }

    @Override
    public void setGroupTopicData(E groupData) {
        //设置icon
        topicImage.setImageResource(R.drawable.im_icon_chat_class);
        //设置topic name
        String groupName = groupData.getGroup();
        topicNameTv.setText("班级群聊(" + groupName + ")");
        setItemVisiable(!groupData.isAlreadyDeletedLocalTopic());
    }

    @Override
    public void setPrivateTopicData(E privateData) {
        //获取私聊对话的对象
        DbMember member = null;
        for (DbMember memberNew : privateData.getMembers()) {
            if (memberNew.getImId() != Constants.imId) {
                member = memberNew;
                break;
            }
        }
        //设置icon
        if (member != null) {
            loadTopicAvaral(topicImage, member.getAvatar());
            //对转义字符进行处理
            topicNameTv.setText(member.getName());
        } else {
            YXLogger.e(getClass().getSimpleName(), "topic的member信息为空");
            loadTopicAvaral(topicImage, "");
        }
    }

    @Override
    public void setLatestMsgInfo(MsgItemBean latestMsg, boolean showSenderName) {
        //设置时间
        if (latestMsg == null) {
            resetView();
            return;
        }

        latestMsgTime.setText(ImDateFormateUtils.timeStr(latestMsg.getSendTime()));
        //设置发送者
        DbMember sender = latestMsg.getMember();
        StringBuilder msgContent = new StringBuilder();


        //有发送者信息 并且 不是自己发送的消息  需要添加 发送者名称
        if (sender != null) {
            if (showSenderName) {
                msgContent.append(sender.getName());
                msgContent.append(":");
            }

        } else {
            //如果发送这信息 为空 需要有：
            msgContent.append("  :");
        }
        boolean isImage = latestMsg.getContentType() == 20;
        msgContent.append(isImage ? "[图片]" : latestMsg.getMsg());
        latestMsgContent.setText(msgContent.toString());
    }

    //目前没有sender信息 重置
    @Override
    public void resetView() {
        latestMsgTime.setText("");
        latestMsgContent.setText("");
    }


    /**
     * 对于私聊topic 加载 member头像
     */
    @Override
    public void loadTopicAvaral(ImageView imageView, String imgUrl) {
        imageView.setImageResource(R.drawable.im_chat_default);
    }
}
