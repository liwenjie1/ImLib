package com.yanxiu.im.net;

import java.util.List;

public class GetContactMembersResponse_new extends ImResponseBase_new {


    /**
     * data : {"imEvent":233,"reqId":"1","topicChange":null,"topic":null,"topicMsg":null,"chatroom":null,"members":null,"contacts":{"groups":[{"groupId":10,"groupName":"","contacts":[{"id":null,"bizSource":1,"memberId":5,"contactId":1,"contactType":2,"memberInfo":{"id":1,"bizSource":1,"memberType":1,"userId":23248703,"memberName":"许褚","avatar":"http://s2.jsyxw.cn/yanxiu/h2.jpg","state":1}},{"id":null,"bizSource":1,"memberId":5,"contactId":5,"contactType":2,"memberInfo":{"id":5,"bizSource":1,"memberType":1,"userId":23248706,"memberName":"典韦","avatar":"http://s2.jsyxw.cn/yanxiu/h2.jpg","state":1}}]}]}}
     * currentUser :
     * currentTime : 1520563310394
     * error : null
     */

    private DataBean data;

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * imEvent : 233
         * reqId : 1
         * topicChange : null
         * topic : null
         * topicMsg : null
         * chatroom : null
         * members : null
         * contacts : {"groups":[{"groupId":10,"groupName":"","contacts":[{"id":null,"bizSource":1,"memberId":5,"contactId":1,"contactType":2,"memberInfo":{"id":1,"bizSource":1,"memberType":1,"userId":23248703,"memberName":"许褚","avatar":"http://s2.jsyxw.cn/yanxiu/h2.jpg","state":1}},{"id":null,"bizSource":1,"memberId":5,"contactId":5,"contactType":2,"memberInfo":{"id":5,"bizSource":1,"memberType":1,"userId":23248706,"memberName":"典韦","avatar":"http://s2.jsyxw.cn/yanxiu/h2.jpg","state":1}}]}]}
         */

        private int imEvent;
        private String reqId;
        private Object topicChange;
        private Object topic;
        private Object topicMsg;
        private Object chatroom;
        private Object members;
        private ContactsBeanX contacts;

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

        public ContactsBeanX getContacts() {
            return contacts;
        }

        public void setContacts(ContactsBeanX contacts) {
            this.contacts = contacts;
        }

        public static class ContactsBeanX {
            private List<GroupsBean> groups;

            public List<GroupsBean> getGroups() {
                return groups;
            }

            public void setGroups(List<GroupsBean> groups) {
                this.groups = groups;
            }

            public static class GroupsBean {
                /**
                 * groupId : 10
                 * groupName :
                 * contacts : [{"id":null,"bizSource":1,"memberId":5,"contactId":1,"contactType":2,"memberInfo":{"id":1,"bizSource":1,"memberType":1,"userId":23248703,"memberName":"许褚","avatar":"http://s2.jsyxw.cn/yanxiu/h2.jpg","state":1}},{"id":null,"bizSource":1,"memberId":5,"contactId":5,"contactType":2,"memberInfo":{"id":5,"bizSource":1,"memberType":1,"userId":23248706,"memberName":"典韦","avatar":"http://s2.jsyxw.cn/yanxiu/h2.jpg","state":1}}]
                 */

                private int groupId;
                private String groupName;
                private List<ContactsBean> contacts;

                public int getGroupId() {
                    return groupId;
                }

                public void setGroupId(int groupId) {
                    this.groupId = groupId;
                }

                public String getGroupName() {
                    return groupName;
                }

                public void setGroupName(String groupName) {
                    this.groupName = groupName;
                }

                public List<ContactsBean> getContacts() {
                    return contacts;
                }

                public void setContacts(List<ContactsBean> contacts) {
                    this.contacts = contacts;
                }

                public static class ContactsBean {
                    /**
                     * id : null
                     * bizSource : 1
                     * memberId : 5
                     * contactId : 1
                     * contactType : 2
                     * memberInfo : {"id":1,"bizSource":1,"memberType":1,"userId":23248703,"memberName":"许褚","avatar":"http://s2.jsyxw.cn/yanxiu/h2.jpg","state":1}
                     */

                    private Object id;
                    private int bizSource;
                    private int memberId;
                    private int contactId;
                    private int contactType;
                    private MemberInfoBean memberInfo;

                    public Object getId() {
                        return id;
                    }

                    public void setId(Object id) {
                        this.id = id;
                    }

                    public int getBizSource() {
                        return bizSource;
                    }

                    public void setBizSource(int bizSource) {
                        this.bizSource = bizSource;
                    }

                    public int getMemberId() {
                        return memberId;
                    }

                    public void setMemberId(int memberId) {
                        this.memberId = memberId;
                    }

                    public int getContactId() {
                        return contactId;
                    }

                    public void setContactId(int contactId) {
                        this.contactId = contactId;
                    }

                    public int getContactType() {
                        return contactType;
                    }

                    public void setContactType(int contactType) {
                        this.contactType = contactType;
                    }

                    public MemberInfoBean getMemberInfo() {
                        return memberInfo;
                    }

                    public void setMemberInfo(MemberInfoBean memberInfo) {
                        this.memberInfo = memberInfo;
                    }

                    public static class MemberInfoBean {
                        /**
                         * id : 1
                         * bizSource : 1
                         * memberType : 1
                         * userId : 23248703
                         * memberName : 许褚
                         * avatar : http://s2.jsyxw.cn/yanxiu/h2.jpg
                         * state : 1
                         */

                        private int id;
                        private int bizSource;
                        private int memberType;
                        private int userId;
                        private String memberName;
                        private String avatar;
                        private int state;

                        public int getId() {
                            return id;
                        }

                        public void setId(int id) {
                            this.id = id;
                        }

                        public int getBizSource() {
                            return bizSource;
                        }

                        public void setBizSource(int bizSource) {
                            this.bizSource = bizSource;
                        }

                        public int getMemberType() {
                            return memberType;
                        }

                        public void setMemberType(int memberType) {
                            this.memberType = memberType;
                        }

                        public int getUserId() {
                            return userId;
                        }

                        public void setUserId(int userId) {
                            this.userId = userId;
                        }

                        public String getMemberName() {
                            return memberName;
                        }

                        public void setMemberName(String memberName) {
                            this.memberName = memberName;
                        }

                        public String getAvatar() {
                            return avatar;
                        }

                        public void setAvatar(String avatar) {
                            this.avatar = avatar;
                        }

                        public int getState() {
                            return state;
                        }

                        public void setState(int state) {
                            this.state = state;
                        }
                    }
                }
            }
        }
    }
}
