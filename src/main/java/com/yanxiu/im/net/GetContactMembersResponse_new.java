package com.yanxiu.im.net;

import java.io.Serializable;
import java.util.List;

public class GetContactMembersResponse_new extends ImResponseBase_new {


    public AdressData data;

    public class AdressData {
        public List<AdressBookPeople> masters;
        public AdressStudents students;

        public class AdressStudents {
            public List<AdressBookPeople> elements;
            public int offset;
            public int pageSize;
            public int totalElements;
        }
    }


    public class AdressBookPeople implements Serializable {

        /**
         * 0默认 1标题教师  2标题学生
         */
        public int type;
        public String text;
        /**
         * 这个人是否是教师
         */
        public boolean isTeacher = false;

        public String avatar;
        public int id;
        public String mobilePhone;
        public String realName;
        public String school;
        public int stage;
        public int subject;
        public int sex;
        public int userId;
        public int userStatus;

        public AdressBookPeople(int type, String text) {
            this.type = type;
            this.text = text;
        }
    }
}
