package com.yanxiu.im.bean;

import com.yanxiu.im.bean.net_bean.ImMember_new;

/**
 * Created by Hu Chao on 18/5/17.
 */
public class ContactsMemberBean {

    private Object id;
    private Long bizSource;
    private Long memberId;
    private Long contactId;
    private Long contactType;
    private ImMember_new memberInfo;

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Long getBizSource() {
        return bizSource;
    }

    public void setBizSource(Long bizSource) {
        this.bizSource = bizSource;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Long getContactType() {
        return contactType;
    }

    public void setContactType(Long contactType) {
        this.contactType = contactType;
    }

    public ImMember_new getMemberInfo() {
        return memberInfo;
    }

    public void setMemberInfo(ImMember_new memberInfo) {
        this.memberInfo = memberInfo;
    }
}
