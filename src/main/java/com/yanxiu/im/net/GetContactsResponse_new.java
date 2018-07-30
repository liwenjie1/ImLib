package com.yanxiu.im.net;

import com.yanxiu.im.bean.ContactsGroupBean;
import com.yanxiu.im.bean.ContactsMemberBean;

import java.util.List;

/**
 * 3.1 当前用户通讯录
 * Created by frc on 2018/3/20.
 * wiki:http://wiki.yanxiu.com/pages/viewpage.action?pageId=12326677
 */

public class GetContactsResponse_new extends ImResponseBase_new {


    private DataBean data;
    private long currentTime;

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public static class DataBean {

        private int imEvent;
        private String reqId;
        private Object topicChange;
        private Object topic;
        private Object topicMsg;
        private Object chatroom;
        private Object members;
        private ContactsWrapper contacts;

        public int getImEvent() {
            return imEvent;
        }

        public void setImEvent(int imEvent) {
            this.imEvent = imEvent;
        }

        public String getReqId() {
            return reqId;
        }

        public void setReqId(String reqId) {
            this.reqId = reqId;
        }

        public Object getTopicChange() {
            return topicChange;
        }

        public void setTopicChange(Object topicChange) {
            this.topicChange = topicChange;
        }

        public Object getTopic() {
            return topic;
        }

        public void setTopic(Object topic) {
            this.topic = topic;
        }

        public Object getTopicMsg() {
            return topicMsg;
        }

        public void setTopicMsg(Object topicMsg) {
            this.topicMsg = topicMsg;
        }

        public Object getChatroom() {
            return chatroom;
        }

        public void setChatroom(Object chatroom) {
            this.chatroom = chatroom;
        }

        public Object getMembers() {
            return members;
        }

        public void setMembers(Object members) {
            this.members = members;
        }

        public ContactsWrapper getContacts() {
            return contacts;
        }

        public void setContacts(ContactsWrapper contacts) {
            this.contacts = contacts;
        }

        public static class ContactsWrapper {
            private List<ContactsGroupBean> groups;
            private List<ContactsMemberBean> personals;

            public List<ContactsGroupBean> getGroups() {
                return groups;
            }

            public void setGroups(List<ContactsGroupBean> groups) {
                this.groups = groups;
            }

            public List<ContactsMemberBean> getPersonals() {
                return personals;
            }

            public void setPersonals(List<ContactsMemberBean> personals) {
                this.personals = personals;
            }
        }
    }
}

