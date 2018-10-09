package com.yanxiu.im.business.contacts.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.yanxiu.im.R;
import com.yanxiu.im.net.GetTopicMemberListResponse;

import java.util.ArrayList;

public class TopicMemberAdapter extends RecyclerView.Adapter<TopicMemberAdapter.TopicMemberViewHolder> {

    private ArrayList<GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean> mDatas;

    public void setDatas(ArrayList<GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean> datas) {
        mDatas = datas;
    }

    @Override
    public TopicMemberAdapter.TopicMemberViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_item_contacts_member, parent, false);
        return new TopicMemberViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TopicMemberAdapter.TopicMemberViewHolder holder, int position) {
        GetTopicMemberListResponse.DataBean.ContactsBeanX.GroupsBean.ContactsBean contactsBean = mDatas.get(position);
        holder.username.setText(contactsBean.getMemberInfo().getMemberName());
        Glide.with(holder.itemView.getContext()).load(contactsBean.getMemberInfo().getAvatar()).placeholder(R.drawable.im_chat_default).into(holder.avaral);
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
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

}
