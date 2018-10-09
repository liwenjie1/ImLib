package com.yanxiu.im.business.contacts.adapter;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.yanxiu.im.R;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;
import com.yanxiu.im.net.GetTopicMemberListResponse;

import java.util.ArrayList;
import java.util.List;

public class TopicMemberAdapter extends RecyclerView.Adapter<TopicMemberAdapter.TopicMemberViewHolder> implements Filterable {

    private ArrayList<GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean> mDatas;
    private ArrayList<GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean> mFilterDatas;

    public void setDatas(ArrayList<GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean> datas) {
        mDatas = datas;
        mFilterDatas=datas;
    }

    public ArrayList<GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean> getDatas() {
        return mFilterDatas;
    }

    @Override
    public TopicMemberAdapter.TopicMemberViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_item_contacts_member, parent, false);
        return new TopicMemberViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TopicMemberAdapter.TopicMemberViewHolder holder, final int position) {
        GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean contactsBean = mFilterDatas.get(position);
        holder.username.setText(contactsBean.getMemberInfo().getMemberName());
        Glide.with(holder.itemView.getContext()).load(contactsBean.getMemberInfo().getAvatar()).placeholder(R.drawable.im_chat_default).into(holder.avaral);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecyclerViewItemOnClickListener != null) {
                    mRecyclerViewItemOnClickListener.onItemClicked(v, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mFilterDatas.size();
    }


    private RecyclerViewItemOnClickListener mRecyclerViewItemOnClickListener;

    public void setRecyclerViewItemOnClickListener(RecyclerViewItemOnClickListener recyclerViewItemOnClickListener) {
        mRecyclerViewItemOnClickListener = recyclerViewItemOnClickListener;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new MemberFilter();
        }
        return mFilter;
    }

    public static class TopicMemberViewHolder extends RecyclerView.ViewHolder {


        private ImageView avaral;
        private TextView username;
        private TextView phoneNum;

        public TopicMemberViewHolder(View itemView) {
            super(itemView);
            phoneNum = itemView.findViewById(R.id.tv_phone_number);
            avaral = itemView.findViewById(R.id.iv_avatar);
            username = itemView.findViewById(R.id.tv_name);
        }
    }

    //过滤后的数据
    private MemberFilter mFilter;

    /**
     * 成员过滤器
     * Created by Hu Chao on 18/5/17.
     */
    private class MemberFilter extends Filter {
        /**
         * 执行过滤的方法
         *
         * @param prefix
         * @return
         */
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults filterResults = new FilterResults();
            //过滤关键字为空，则使用源数据
            if (TextUtils.isEmpty(prefix)) {
                filterResults.values = mDatas;
            } else {
                String charString = prefix.toString().toLowerCase();
                List<GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean> filterDatas = new ArrayList<>();
                for (GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean bean : mDatas) {
                    //根据需求，添加匹配规则
                    if (bean.getMemberInfo().getMemberName().toLowerCase().contains(charString)) {
                        filterDatas.add(bean);
                    }
                }
                filterResults.values = filterDatas;
            }
            return filterResults;
        }

        /**
         * 得到过滤结果
         *
         * @param prefix
         * @param results
         */
        @Override
        protected void publishResults(CharSequence prefix, FilterResults results) {
            mFilterDatas = (ArrayList<GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean>) results.values;
            notifyDataSetChanged();
        }
    }

}
