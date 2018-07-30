package com.yanxiu.im.business.contacts.adapter;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.test.yanxiu.common_base.utils.EscapeCharacterUtils;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.ContactsGroupBean;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;

import java.util.List;


/**
 * 班级列表adapter
 * Created by Hu Chao on 18/5/17.
 */

public class ContactsGroupAdapter extends RecyclerView.Adapter<ContactsGroupAdapter.ContactsGroupViewHolder> {


    private RecyclerViewItemOnClickListener mItemOnClickListener;

    private List<ContactsGroupBean> mDatas;
    public int mCurrentSelectedPosition;

    public void setItemOnClickListener(RecyclerViewItemOnClickListener onItemClickListener) {
        this.mItemOnClickListener = onItemClickListener;
    }

    public void setDatas(List<ContactsGroupBean> mDatas) {
        this.mDatas = mDatas;
    }

    public void setSelectedPosition(int position) {
        this.mCurrentSelectedPosition = position;
    }

    @Override
    public ContactsGroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_item_contacts_group, parent, false);
        return new ContactsGroupViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ContactsGroupViewHolder holder, final int position) {
        ContactsGroupBean bean = mDatas.get(position);
        //转义字符处理
        holder.tv_class_name.setText(EscapeCharacterUtils.unescape(bean.getGroupName()));
        if (position == mCurrentSelectedPosition) {
            holder.tv_class_name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_1da1f2));
            holder.iv_selected.setVisibility(View.VISIBLE);
        } else {
            holder.tv_class_name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_333333));
            holder.iv_selected.setVisibility(View.INVISIBLE);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemOnClickListener != null) {
                    mCurrentSelectedPosition = position;
                    mItemOnClickListener.onItemClicked(v, holder.getAdapterPosition());
                    notifyDataSetChanged();
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    class ContactsGroupViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_selected;
        TextView tv_class_name;

        public ContactsGroupViewHolder(View itemView) {
            super(itemView);
            tv_class_name = itemView.findViewById(R.id.tv_class_name);
            iv_selected = itemView.findViewById(R.id.iv_selected);

        }
    }

}
