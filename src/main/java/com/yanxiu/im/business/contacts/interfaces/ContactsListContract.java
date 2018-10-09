package com.yanxiu.im.business.contacts.interfaces;

import java.util.ArrayList;

public interface ContactsListContract {
    interface IView<E, T> {
        void onGetClazsList(ArrayList<E> datalist);

        void onGetClazsMemberList(ArrayList<T> memberList);

        void onException(String msg);
    }

    interface IPresenter {
        void doGetClazsList();

        void doGetClazsMemberList(String clazsId);

    }
}
