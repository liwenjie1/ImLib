package com.yanxiu.im.business.contacts.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.test.yanxiu.common_base.ui.recycler_view.BaseAdapter;
import com.test.yanxiu.common_base.ui.recycler_view.BaseViewHolder;
import com.yanxiu.im.R;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;

public class ContactsClazsAdapter extends BaseAdapter<ContactsClazsAdapter.ContactsGroupViewHolder> {
    public ContactsClazsAdapter(Context context) {
        super(context);
    }

    private RecyclerViewItemOnClickListener mItemOnClickListener;


    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 10;
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
