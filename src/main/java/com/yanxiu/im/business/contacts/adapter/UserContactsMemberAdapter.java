package com.yanxiu.im.business.contacts.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.test.yanxiu.common_base.ui.recycler_view.BaseViewHolder;
import com.yanxiu.im.R;
import com.yanxiu.im.business.interfaces.RecyclerViewItemOnClickListener;
import com.yanxiu.im.net.GetContactMembersResponse_new;

import java.util.ArrayList;
import java.util.List;

public class UserContactsMemberAdapter extends RecyclerView.Adapter {

    private final int HEAD_ITEM = 0X01;
    private final int MANAGER_ITEM = 0X02;
    private final int STUDENT_ITEM = 0X03;


    private List<GetContactMembersResponse_new.AdressBookPeople> mMasterList;
    private List<GetContactMembersResponse_new.AdressBookPeople> mStudentList;
    private ArrayList<GetContactMembersResponse_new.AdressBookPeople> mDataList;

    public List<GetContactMembersResponse_new.AdressBookPeople> getStudentList() {
        return mStudentList;
    }

    public void addStudentList(List<GetContactMembersResponse_new.AdressBookPeople> studentList){
        if (mDataList==null) {
            mDataList=new ArrayList<>();
        }
        mDataList.addAll(studentList);
    }

    public void setDataList(List<GetContactMembersResponse_new.AdressBookPeople> masterList, List<GetContactMembersResponse_new.AdressBookPeople> studentList) {
        this.mMasterList=masterList;
        this.mStudentList=studentList;
        mDataList=new ArrayList<>();
        mDataList.addAll(masterList);
        mDataList.addAll(studentList);
    }

    public ArrayList<GetContactMembersResponse_new.AdressBookPeople> getDataList() {
        return mDataList;
    }

    @Override
    public int getItemViewType(int position) {
        return HEAD_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_adress_item, parent, false);
        UserContactPeopleViewHolder holder = new UserContactPeopleViewHolder(parent.getContext(), inflate);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecyclerViewItemOnClickListener != null) {
                    mRecyclerViewItemOnClickListener.onItemClicked(v, position);
                }
            }
        });
        ((UserContactPeopleViewHolder) holder).setData(position, mDataList.get(position));
    }

    @Override
    public int getItemCount() {
        return mDataList == null ? 0 : mDataList.size();
    }


    private RecyclerViewItemOnClickListener mRecyclerViewItemOnClickListener;

    public void setRecyclerViewItemOnClickListener(RecyclerViewItemOnClickListener recyclerViewItemOnClickListener) {
        mRecyclerViewItemOnClickListener = recyclerViewItemOnClickListener;
    }

    /**
     * 分类标题 item
     */
    public static class HeadViewholder extends BaseViewHolder<String> {

        public HeadViewholder(Context context, View itemView) {
            super(context, itemView);
        }

        @Override
        public void setData(int position, String data) {

        }
    }


    /**
     * 学员
     */
    public static class UserContactPeopleViewHolder extends BaseViewHolder<GetContactMembersResponse_new.AdressBookPeople> {
        private TextView username;
        private TextView phone;
        private ImageView avaral;

        public UserContactPeopleViewHolder(Context context, View itemView) {
            super(context, itemView);
            username = itemView.findViewById(R.id.tv_name);
            phone = itemView.findViewById(R.id.tv_mobile);
            avaral = itemView.findViewById(R.id.iv_head_img);
        }

        @Override
        public void setData(int position, GetContactMembersResponse_new.AdressBookPeople data) {
            username.setText(data.realName);
            phone.setText(data.mobilePhone);
            Glide.with(itemView.getContext()).load(data.avatar).placeholder(R.drawable.im_chat_default).into(avaral);
        }
    }


}
