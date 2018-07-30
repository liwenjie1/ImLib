package com.yanxiu.im.bean;

import java.util.List;

/**
 * Created by Hu Chao on 18/5/17.
 */
public class ContactsGroupBean {

    private Long groupId;
    private String groupName;
    private List<ContactsMemberBean> contacts;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<ContactsMemberBean> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactsMemberBean> contacts) {
        this.contacts = contacts;
    }

}
