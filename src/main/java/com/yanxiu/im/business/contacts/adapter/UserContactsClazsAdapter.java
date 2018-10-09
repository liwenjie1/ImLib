package com.yanxiu.im.business.contacts.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.test.yanxiu.common_base.ui.recycler_view.BaseAdapter;
import com.test.yanxiu.common_base.ui.recycler_view.BaseViewHolder;
import com.yanxiu.im.R;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;
import com.yanxiu.im.net.GetClazsListResponse;

import java.util.List;

public class UserContactsClazsAdapter extends BaseAdapter<UserContactsClazsAdapter.ContactsGroupViewHolder> {


    private RecyclerViewItemOnClickListener mItemOnClickListener;

    private List<GetClazsListResponse.ClazsInfosBean> mDatas;
    public int mCurrentSelectedPosition;



    public GetClazsListResponse.ClazsInfosBean getClazsInfoByPos(int position){
        return mDatas.get(position);
    }




    public UserContactsClazsAdapter(Context context) {
        super(context);
    }

    public void setItemOnClickListener(RecyclerViewItemOnClickListener onItemClickListener) {
        this.mItemOnClickListener = onItemClickListener;
    }

    public void setDatas(List<GetClazsListResponse.ClazsInfosBean> mDatas) {
        this.mDatas = mDatas;
    }

    public void setSelectedPosition(int position) {
        this.mCurrentSelectedPosition = position;
    }

    @Override
    public UserContactsClazsAdapter.ContactsGroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_item_contacts_group, parent, false);
        return new UserContactsClazsAdapter.ContactsGroupViewHolder(parent.getContext(), itemView);
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder,final int position) {
        GetClazsListResponse.ClazsInfosBean clazsInfosBean = mDatas.get(position);
        //转义字符处理
        ((ContactsGroupViewHolder) holder).tv_class_name.setText(clazsInfosBean.getClazsName());
        if (position == mCurrentSelectedPosition) {
            ((ContactsGroupViewHolder) holder).tv_class_name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_1da1f2));
            ((ContactsGroupViewHolder) holder).iv_selected.setVisibility(View.VISIBLE);
        } else {
            ((ContactsGroupViewHolder) holder).tv_class_name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_333333));
            ((ContactsGroupViewHolder) holder).iv_selected.setVisibility(View.INVISIBLE);
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

    public  class ContactsGroupViewHolder extends BaseViewHolder<GetClazsListResponse.ClazsInfosBean> {
        ImageView iv_selected;
        TextView tv_class_name;

        public ContactsGroupViewHolder(Context context, View itemView) {
            super(context, itemView);
            iv_selected = itemView.findViewById(R.id.iv_selected);
            tv_class_name = itemView.findViewById(R.id.tv_class_name);
        }


        @Override
        public void setData(int position, GetClazsListResponse.ClazsInfosBean data) {
            tv_class_name.setText(data.getClazsName());
            iv_selected.setVisibility(position==mCurrentSelectedPosition?View.VISIBLE:View.INVISIBLE);
        }
    }


}
