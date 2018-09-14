package com.yanxiu.im.business.contacts.interfaces.impls;

import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.ContactsGroupBean;
import com.yanxiu.im.bean.ContactsMemberBean;
import com.yanxiu.im.bean.net_bean.ImMember_new;
import com.yanxiu.im.business.contacts.interfaces.ContactsContract;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.manager.DatabaseManager;
import com.yanxiu.im.net.GetContactsRequest_new;
import com.yanxiu.im.net.GetContactsResponse_new;
import com.yanxiu.lib.yx_basic_library.network.IYXHttpCallback;
import com.yanxiu.lib.yx_basic_library.network.YXRequestBase;

import java.util.List;

import okhttp3.Request;

/**
 * 实现联系人ui层和数据层交互
 * Created by Hu Chao on 18/5/17.
 */
public class ContactsPresenter implements ContactsContract.IPresenter {

    private ContactsContract.IView mView;

    private List<ContactsGroupBean> mGroupsBeans;

    private int mCurrentGroupIndex = 0;

    public int getCurrentGroupIndex() {
        return mCurrentGroupIndex;
    }

    public String getCurrentGroupName() {
       return mGroupsBeans.get(mCurrentGroupIndex).getGroupName();
    }

    public long getCurrentGroupId() {
        return mGroupsBeans.get(mCurrentGroupIndex).getGroupId();
    }

    public List<ContactsGroupBean> getGroupsBeans() {
        return mGroupsBeans;
    }

    public ContactsPresenter(ContactsContract.IView view) {
        this.mView = view;
    }

    @Override
    public void loadContacts() {
        mView.showLoading();
        GetContactsRequest_new getContactsRequest = new GetContactsRequest_new();
        getContactsRequest.imToken = Constants.imToken;
        getContactsRequest.startRequest(GetContactsResponse_new.class, new IYXHttpCallback<GetContactsResponse_new>() {
            @Override
            public void onRequestCreated(Request request) {

            }

            @Override
            public void onSuccess(YXRequestBase request, GetContactsResponse_new ret) {
                mView.hideError();
                mView.hideLoading();
                if (ret == null) {
                    mView.showNoDataError();
                    return;
                }
                if (ret.code == 0) {
                    if (ret.getData() != null && ret.getData().getContacts() != null
                            && ret.getData().getContacts().getGroups() != null
                            && ret.getData().getContacts().getGroups().size() > 0) {
                        mGroupsBeans = ret.getData().getContacts().getGroups();
                        loadMembersByPosition(0);
                        updateDbMembers();
                    } else {
                        mView.showNoDataError();
                    }

                } else {
                    mView.showOtherError(ret.message);
                }

            }

            @Override
            public void onFail(YXRequestBase request, Error error) {
                mView.hideLoading();
                mView.showNetError();
            }
        });
    }

    @Override
    public void loadMembersByPosition(int position) {
        this.mCurrentGroupIndex = position;
        if (mGroupsBeans.get(position) != null
                && mGroupsBeans.get(position).getContacts() != null
                && mGroupsBeans.get(position).getContacts().size() > 0) {
            mView.showCurrentContactsGroupName(mGroupsBeans.get(position).getGroupName());
            mView.showContactsMembersList(mGroupsBeans.get(position).getContacts());
        } else {
            mView.showNoDataError();
        }
    }

    @Override
    public void loadGroupsList() {
        mView.showContactsGroupsList(mCurrentGroupIndex, mGroupsBeans);
    }


    /**
     * 比较通讯录member信息相对db中是否有变化
     * 如果db中不存在member，则不处理;存在且有变化则更新db
     */
    private void updateDbMembers() {
        List<DbMember> dbMembers = DatabaseManager.getAllDbMembers();
        for (ContactsGroupBean groupBean : mGroupsBeans) {
            if (groupBean.getContacts() == null) {
                continue;
            }
            for (ContactsMemberBean memberBean : groupBean.getContacts()) {
                //db中存在且有变化，则更新db
                for (DbMember member : dbMembers) {
                    //member存在
                    if (member.getImId() == memberBean.getMemberInfo().imId) {
                        //有变化，则更新db
                        if (isChange(member, memberBean.getMemberInfo())) {
                            DatabaseManager.updateDbMemberWithImMember(memberBean.getMemberInfo());
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * member信息是否有变化
     * 主要判断name和avatar是否有变化
     *
     * @param dbMember       数据库member信息
     * @param contactsMember 通讯录member信息
     * @return true 有更新; false 无更新
     */
    private boolean isChange(DbMember dbMember, ImMember_new contactsMember) {
        if (!dbMember.getName().equals(contactsMember.memberName)
                || !dbMember.getAvatar().equals(contactsMember.avatar)) {
            return true;
        }
        return false;
    }

    /**
     * 如果 member 不存在 先进行数据库保存 防止在 私聊部分造成的空指针
     */
    public void checkMemberDbInfo(ContactsGroupBean groupBean, ImMember_new memberInfo) {
        //检查 topic 是否存在
        final Long targetTopicId = groupBean.getGroupId();
        //检查 member 是否存在
        final DbMember memberById = DatabaseManager.getMemberById(memberInfo.imId);
        if (memberById == null) {
            //插入新的 member 信息
            DbMember newMember = new DbMember();
            newMember.setRole(memberInfo.memberType);
            newMember.setName(memberInfo.memberName);
            newMember.setAvatar(memberInfo.avatar);
            newMember.save();
        } else {
            //如果信息有变化
            if (isChange(memberById, memberInfo)) {
                memberById.setName(memberInfo.memberName);
                memberById.setAvatar(memberInfo.avatar);
                memberById.setRole(memberInfo.memberType);
                memberById.save();
            }
        }
    }
}
