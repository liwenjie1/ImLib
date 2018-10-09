package com.yanxiu.im.business.contacts.interfaces;

import java.util.ArrayList;

public interface TopicMemberListContract {
    interface IView<E> {
        void onGetMemberList(ArrayList<E> memberList);

        void onLoadMemberList(ArrayList<E> memberList);

        void onException(String msg);
    }

    interface IPresenter {

        void doGetMemberList(String topicId);

        void doLoadMoreMemberList(String topicId, int offset, int limit);
    }
}
