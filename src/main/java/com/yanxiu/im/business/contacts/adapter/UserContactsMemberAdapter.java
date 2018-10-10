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

    public void addStudentList(List<GetContactMembersResponse_new.AdressBookPeople> studentList) {
        if (mDataList == null) {
            mDataList = new ArrayList<>();
        }
//        for (GetContactMembersResponse_new.AdressBookPeople master : mMasterList) {
//            for (GetContactMembersResponse_new.AdressBookPeople student : studentList) {
//                if (student.userId == master.userId) {
//                    studentList.remove(student);
//                    break;
//                }
//            }
//        }

        mDataList.addAll(studentList);
    }

    public void setDataList(List<GetContactMembersResponse_new.AdressBookPeople> masterList, List<GetContactMembersResponse_new.AdressBookPeople> studentList) {
        this.mMasterList = masterList;
        this.mStudentList = studentList;
        mDataList = new ArrayList<>();

//        for (GetContactMembersResponse_new.AdressBookPeople master : mMasterList) {
//            for (GetContactMembersResponse_new.AdressBookPeople student : mStudentList) {
//                if (student.userId == master.userId) {
//                    mStudentList.remove(student);
//                    break;
//                }
//            }
//        }
        GetContactMembersResponse_new.AdressBookPeople masterHead=new GetContactMembersResponse_new.AdressBookPeople(1,"管理员");
        masterHead.userId=-1;
        GetContactMembersResponse_new.AdressBookPeople studentHead=new GetContactMembersResponse_new.AdressBookPeople(2,"学员");
        studentHead.userId=-1;
        mDataList.add(masterHead);
        mDataList.addAll(masterList);
        mDataList.add(studentHead);
        mDataList.addAll(studentList);
    }


    public ArrayList<GetContactMembersResponse_new.AdressBookPeople> getDataList() {
        return mDataList;
    }

    @Override
    public int getItemViewType(int position) {
        if (mDataList.get(position).userId == -1) {
            return HEAD_ITEM;
        } else {
            return STUDENT_ITEM;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflate = null;
        RecyclerView.ViewHolder holder = null;
        if (viewType == STUDENT_ITEM) {
            inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_adress_item, parent, false);
            holder = new UserContactPeopleViewHolder(parent.getContext(), inflate);
        } else {
            inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_adress_head, parent, false);
            holder = new HeadViewholder(parent.getContext(), inflate);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (getItemViewType(position) == STUDENT_ITEM) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mRecyclerViewItemOnClickListener != null) {
                        mRecyclerViewItemOnClickListener.onItemClicked(v, position);
                    }
                }
            });
            ((UserContactPeopleViewHolder) holder).setData(position, mDataList.get(position));
        } else {
            ((HeadViewholder) holder).setData(position, position == 0 ? "管理员" : "学员");
        }
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
            tv_title = itemView.findViewById(R.id.tv_title);
        }

        private TextView tv_title;

        @Override
        public void setData(int position, String data) {
            tv_title.setText(data);
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
