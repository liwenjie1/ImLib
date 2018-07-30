package com.yanxiu.im.business.msglist.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.business.msglist.adapter.viewholders.ImImgMsgItemViewHolder;
import com.yanxiu.im.business.msglist.adapter.viewholders.ImImgMyMsgItemViewHolder;
import com.yanxiu.im.business.msglist.adapter.viewholders.ImTextMsgItemViewHolder;
import com.yanxiu.im.business.msglist.adapter.viewholders.ImTextMyMsgItemViewHolder;
import com.yanxiu.im.business.msglist.adapter.viewholders.base.AbstractViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * create by 朱晓龙 2018/4/23 下午10:21
 */
public class ImMsgListAdapter<E extends MsgItemBean> extends RecyclerView.Adapter<AbstractViewHolder> {
    public static final int ITEM_TYPE_MYMSG_TEXT = 0X11;
    public static final int ITEM_TYPE_MYMSG_IMAGE = 0X12;
    public static final int ITEM_TYPE_MSG_TEXT = 0X21;
    public static final int ITEM_TYPE_MSG_IMAGE = 0X22;


    private String TAG = getClass().getSimpleName();
    private Context mContext;
    private List<E> dataList;

    public ImMsgListAdapter(Context mContext, ArrayList<E> dataList) {
        this.mContext = mContext;
        this.dataList = dataList;
    }

    public List<E> getDataList() {
        return dataList;
    }

    public void setDataList(List<E> dataList) {
        this.dataList = dataList;
    }

    @Override
    public int getItemViewType(int position) {
        MsgItemBean dataBean = dataList.get(position);
        if (dataBean.getSenderId() == Constants.imId) {
            if (dataBean.getContentType() == 10) {
                return ITEM_TYPE_MYMSG_TEXT;
            } else if (dataBean.getContentType() == 20) {
                return ITEM_TYPE_MYMSG_IMAGE;
            }
        } else {
            if (dataBean.getContentType() == 10) {
                return ITEM_TYPE_MSG_TEXT;
            } else if (dataBean.getContentType() == 20) {
                return ITEM_TYPE_MSG_IMAGE;
            }
        }
        return ITEM_TYPE_MSG_TEXT;
    }

    @NonNull
    @Override
    public AbstractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case ITEM_TYPE_MSG_TEXT:
                view = LayoutInflater.from(mContext).inflate(R.layout.im_msglist_recyclerview_msg_text_item_layout, null);
                return new ImTextMsgItemViewHolder(view, mContext);
            case ITEM_TYPE_MSG_IMAGE:
                view = LayoutInflater.from(mContext).inflate(R.layout.im_msglist_recyclerview_msg_img_item_layout, null);
                return new ImImgMsgItemViewHolder(view, mContext);
            case ITEM_TYPE_MYMSG_TEXT:
                view = LayoutInflater.from(mContext).inflate(R.layout.im_msglist_recyclerview_mymsg_text_item_layout, null);
                return new ImTextMyMsgItemViewHolder(view, mContext);
            case ITEM_TYPE_MYMSG_IMAGE:
                view = LayoutInflater.from(mContext).inflate(R.layout.im_msglist_recyclerview_mymsg_img_item_layout, null);
                return new ImImgMyMsgItemViewHolder(view, mContext);
            default:
                return null;
        }
    }

    @Override
    public void onViewRecycled(AbstractViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycleSenderListener();
    }

    @Override
    public void onBindViewHolder(@NonNull final AbstractViewHolder holder, final int position) {
        Log.i(TAG, "onBindViewHolder: " + position);
        MsgItemBean msgItemBean = dataList.get(position);
        holder.setData(msgItemBean);
        holder.showBottomPadding(position == 0);
    }


    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

}
