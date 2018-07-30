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
import com.test.yanxiu.common_base.utils.EscapeCharacterUtils;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.ContactsMemberBean;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;

import java.util.ArrayList;
import java.util.List;


/**
 * 成员列表adapter，实现了Filterable接口，可进行过滤
 * Created by Hu Chao on 18/5/17.
 */

public class ContactsMemberAdapter extends RecyclerView.Adapter<ContactsMemberAdapter.ContactsMemberViewHolder> implements Filterable {


    private RecyclerViewItemOnClickListener mItemOnClickListener;

    //源数据
    private List<ContactsMemberBean> mSourceDatas;
    //过滤后的数据
    private List<ContactsMemberBean> mFilterDatas;

    private MemberFilter mFilter;

    public void setItemOnClickListener(RecyclerViewItemOnClickListener onItemClickListener) {
        this.mItemOnClickListener = onItemClickListener;
    }

    public void setDatas(List<ContactsMemberBean> mDatas) {
        this.mSourceDatas = mDatas;
        this.mFilterDatas = mDatas;
    }

    public List<ContactsMemberBean> getDatas() {
        return mFilterDatas;
    }

    @Override
    public ContactsMemberViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_item_contacts_member, parent, false);
        return new ContactsMemberViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ContactsMemberViewHolder holder, int position) {
        ContactsMemberBean bean = mFilterDatas.get(position);
        //转义字符处理
        holder.tv_name.setText(EscapeCharacterUtils.unescape(bean.getMemberInfo().memberName));
        Glide.with(holder.itemView.getContext()).load(bean.getMemberInfo().avatar)
                .placeholder(R.drawable.im_chat_default)
                .into(holder.iv_avatar);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemOnClickListener != null) {
                    mItemOnClickListener.onItemClicked(v, holder.getAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFilterDatas == null ? 0 : mFilterDatas.size();
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new MemberFilter();
        }
        return mFilter;
    }

    class ContactsMemberViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_avatar;
        TextView tv_name;
        TextView tv_phone_number;

        ContactsMemberViewHolder(View itemView) {
            super(itemView);
            iv_avatar = itemView.findViewById(R.id.iv_avatar);
            tv_name = itemView.findViewById(R.id.tv_name);
            tv_phone_number = itemView.findViewById(R.id.tv_phone_number);

        }
    }

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
                filterResults.values = mSourceDatas;
            } else {
                String charString = prefix.toString().toLowerCase();
                List<ContactsMemberBean> filterDatas = new ArrayList<>();
                for (ContactsMemberBean bean : mSourceDatas) {
                    //根据需求，添加匹配规则
                    if (bean.getMemberInfo().memberName.toLowerCase().contains(charString)) {
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
            mFilterDatas = (List<ContactsMemberBean>) results.values;
            notifyDataSetChanged();
        }
    }

}
