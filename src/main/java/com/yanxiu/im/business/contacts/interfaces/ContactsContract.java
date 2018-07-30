package com.yanxiu.im.business.contacts.interfaces;


import com.yanxiu.im.bean.ContactsGroupBean;
import com.yanxiu.im.bean.ContactsMemberBean;

import java.util.List;

/**
 * 联系人ui层和数据层交互接口
 * Created by Hu Chao on 18/5/17.
 */
public interface ContactsContract {

    interface IView {
        /**
         * 显示loading状态
         */
        void showLoading();

        /**
         * 隐藏loading状态
         */
        void hideLoading();

        /**
         * 显示网络错误界面
         */
        void showNetError();

        /**
         * 显示无数据界面
         */
        void showNoDataError();

        /**
         * 显示其他错误界面
         *
         * @param error 错误描述
         */
        void showOtherError(String error);

        /**
         * 隐藏错误界面
         */
        void hideError();

        /**
         * 显示当前班级的名称
         *
         * @param groupName 当前班级名称
         */
        void showCurrentContactsGroupName(String groupName);

        /**
         * 显示当前班级列表
         *
         * @param selectedPosition 当前选中班级在列表中的位置
         * @param memberBeans      班级数据
         */
        void showContactsGroupsList(int selectedPosition, List<ContactsGroupBean> memberBeans);

        /**
         * 显示成员列表
         *
         * @param memberBeans 成员数据
         */
        void showContactsMembersList(List<ContactsMemberBean> memberBeans);

    }

    interface IPresenter {


        /**
         * 加载通信录信息
         */
        void loadContacts();

        /**
         * 加载班级列表
         */
        void loadGroupsList();

        /**
         * 通过班级在列表中的下标位置加载成员列表
         */
        void loadMembersByPosition(int position);

    }

}
