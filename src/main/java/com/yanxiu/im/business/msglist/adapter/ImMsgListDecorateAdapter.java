package com.yanxiu.im.business.msglist.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yanxiu.im.R;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.business.msglist.adapter.viewholders.base.AbstractViewHolder;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by 朱晓龙 on 2018/5/2 18:32.
 * 负责 添加或删除 headerview、fotterView
 * 目前只实现单个 header footer的添加
 * msglistadapte 仅负责 数据的增减
 * 具体状态由 {@link AbstractViewHolder}的相关子类 负责控制
 */

public class ImMsgListDecorateAdapter<E extends MsgItemBean> extends RecyclerView.Adapter<AbstractViewHolder> {

    private  final String TAG = getClass().getSimpleName();
    private ImMsgListAdapter<E> innerAdapter;

    public static final int ITEM_TYPE_FOOTER = 0X03;

    private View headerView;
    private View footerView;

    private Context context;

    public ImMsgListDecorateAdapter(Context context, ArrayList<E> datalist) {
        this.context = context;
        innerAdapter = new ImMsgListAdapter<E>(context, datalist);
    }

    public List<E> getDataList(){
        return innerAdapter.getDataList();
    }


    private boolean hasFooterView=false;
    /**
     * 每次 拉取到新数据 并 加入到数据集更新UI 后 添加
     * */
    public void addFooterView(){
        if (hasFooterView) {
            return;
        }
        hasFooterView=true;
        notifyItemInserted(getItemCount());
    }
    /**
     * 每次拉取到数据后 删除foot
     * */
    public void removeFooterView(){
        Log.i(TAG, "removeFooterView: ");
        if (!hasFooterView) {
            return;
        }
        hasFooterView=false;
        notifyItemRemoved(getItemCount());
    }


    @Override
    public int getItemViewType(int position) {
        if (position == innerAdapter.getItemCount()) {
            return ITEM_TYPE_FOOTER;
        }
        return innerAdapter.getItemViewType(position);
    }

    @NonNull
    @Override
    public AbstractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_FOOTER) {
            footerView = LayoutInflater.from(context).inflate(R.layout.im_msglist_header_view_item_layout, parent, false);
            return new HeadFooterViewHolder(footerView,context);
        }
        return innerAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final AbstractViewHolder holder, final int position) {
        //判断是否是 data 数据
        int type = getItemViewType(position);
        if (type == ITEM_TYPE_FOOTER) {
        } else  {
            if (mMsgItemViewClickListener!=null) {
                holder.setBaseMyMsgItemViewClickListener(new AbstractViewHolder.BaseMyMsgItemViewClickListener() {
                    @Override
                    public void onFailFlagClicked() {
                        Log.i(TAG, "onFailFlagClicked: ");
                        mMsgItemViewClickListener.onFailFlagClicked(position);
                    }

                    @Override
                    public void onSendingPbClicked() {
                        Log.i(TAG, "onSendingPbClicked: ");
                    }

                    @Override
                    public void onMsgContentClicked() {
                        Log.i(TAG, "onMsgContentClicked: ");
                        mMsgItemViewClickListener.onContentClicked(position,getDataList().get(position).getContentType());
                    }

                    @Override
                    public void onSenderClicked() {
                        Log.i(TAG, "onSenderClicked: ");
                        mMsgItemViewClickListener.onIconClicked(position);
                    }
                });
            }
            innerAdapter.onBindViewHolder( holder, position);
        }
    }

    @Override
    public void onViewRecycled(AbstractViewHolder holder) {
        super.onViewRecycled(holder);
//        holder.recycleSenderListener();
        innerAdapter.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        //判断footview是否存在
        int size= innerAdapter.getItemCount()+(hasFooterView?1:0);
        return size;
    }



            public void setDataList(List<E> dataList) {
        innerAdapter.setDataList(dataList);
    }

    public static class HeadFooterViewHolder extends AbstractViewHolder {


        public HeadFooterViewHolder(View itemView, Context mContext) {
            super(itemView, mContext);
        }

        @Override
        protected void initView(View itemView) {

        }

        @Override
        protected void initListener() {

        }

        @Override
        public void setData(MsgItemBean data) {

        }

        @Override
        public void preSizeContentView(int w, int h) {

        }

        @Override
        public void recycleSenderListener() {

        }

        @Override
        public void showBottomPadding(boolean show) {

        }

        @Override
        public void onClick(View v) {

        }
    }



    /**
     * msg 内容各个部分的点击回调
     * 1、头像 2、图片内容 3、重发操作
     * */
    private MsgItemViewClickListener mMsgItemViewClickListener;

    public void setMsgItemViewClickListener(MsgItemViewClickListener msgItemViewClickListener) {
        mMsgItemViewClickListener = msgItemViewClickListener;
    }

    public interface MsgItemViewClickListener{
        void onContentClicked(int p,int ContentType);
        void onIconClicked(int p);
        void onFailFlagClicked(int p);
   }


}
