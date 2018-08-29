package com.yanxiu.im.business.topiclist.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.test.yanxiu.common_base.utils.SharedSingleton;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.interfaces.RecyclerViewItemLongClickListener;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;
import com.yanxiu.im.business.topiclist.adapter.topicviewholder.ImGroupTopicViewHolder;
import com.yanxiu.im.business.topiclist.adapter.topicviewholder.ImPrivateTopicViewHolder;
import com.yanxiu.im.business.topiclist.adapter.topicviewholder.ImTopicBaseViewHolder;
import com.yanxiu.im.manager.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 朱晓龙 on 2018/5/7 16:33.
 */

public final class ImTopicListRecyclerViewAdapter<E extends TopicItemBean> extends RecyclerView.Adapter<ImTopicBaseViewHolder> {

    /**
     * 数据集合
     */
    private List<E> dataList;

    private Context mContext;

    /**
     * viewtype
     */
    public final int TOPIC_TYPE_GROUP = 1;
    public final int TOPIC_TYPE_PRIVATE = 2;


    /**
     * recyclerview 点击监听
     */
    private RecyclerViewItemOnClickListener mRecyclerViewItemOnClickListener;

    public void setmRecyclerViewItemOnClickListener(RecyclerViewItemOnClickListener mRecyclerViewItemOnClickListener) {
        this.mRecyclerViewItemOnClickListener = mRecyclerViewItemOnClickListener;
    }

    public ImTopicListRecyclerViewAdapter(Context mContext) {
        this.mContext = mContext;
        dataList = new ArrayList<>();
    }

    public void setDataList(List<E> dataList) {
        this.dataList.clear();
        if (dataList != null) {

            this.dataList.addAll(dataList);
        }
    }

    public List<E> getDataList() {
        //test 数据被回收 的情况 获取会 null 重新由数据库读取一遍
        if (SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST)==null) {
            final List<TopicItemBean> beans = DatabaseManager.topicsFromDb();
            SharedSingleton.getInstance().set(SharedSingleton.KEY_TOPIC_LIST,beans==null?new ArrayList<TopicItemBean>():beans);
        }
        return SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
    }

    @Override
    public int getItemViewType(int position) {
        if (TextUtils.equals(dataList.get(position).getType(), "1")) {//私聊
            return TOPIC_TYPE_PRIVATE;
        } else {//其他 默认 群聊
            return TOPIC_TYPE_GROUP;
        }
    }

    @NonNull
    @Override
    public ImTopicBaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.im_topiclist_recyclerview_item_layout, parent, false);

        if (viewType == TOPIC_TYPE_PRIVATE) {
            return new ImPrivateTopicViewHolder(view);
        } else {
            return new ImGroupTopicViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ImTopicBaseViewHolder holder, final int position) {
        if (dataList != null) {
            holder.setData(dataList.get(position));
        }
        if (holder.itemView != null) {
            if (mRecyclerViewItemOnClickListener != null) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mRecyclerViewItemOnClickListener.onItemClicked(holder.itemView, position);
                    }
                });
            }
            if (mRecyclerViewItemLongClickListener != null) {
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        //长按删除
                        return mRecyclerViewItemLongClickListener.onItemLongClicked(v, position);
                    }
                });
            }
        }
    }


    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }


    private RecyclerViewItemLongClickListener mRecyclerViewItemLongClickListener;

    public void setRecyclerViewItemLongClickListener(RecyclerViewItemLongClickListener recyclerViewItemLongClickListener) {
        mRecyclerViewItemLongClickListener = recyclerViewItemLongClickListener;
    }
}
