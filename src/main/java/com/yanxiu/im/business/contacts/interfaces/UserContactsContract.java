package com.yanxiu.im.business.contacts.interfaces;

import java.util.List;

public interface UserContactsContract {
    interface IView<E,T> {

         void showLoading();

         void hideLoading();

         void showNetError();

         void showNoDataError();

         void showOtherError(String error);

         void hideError() ;

         void showCurrentContactsGroupName(String groupName) ;

         void showContactsGroupsList(int selectedPosition, List<E> groupBeans);

         void showContactsMembersList(T memberBeans);
         void addLoadMember(T memberBeans);

    }

    interface IPresenter {
        void doLoadClazsList();
        void doLoadMembersList(String clazsId,int offset,int limit);
        void doGetMembersList(String clazsId);
    }
}
