package com.yanxiu.im.net;

import com.yanxiu.lib.yx_basic_library.base.bean.YXBaseBean;

public class GetContactMemberInfoResponse extends ImResponseBase_new {
    public PersonalDetailsData data;

    public class PersonalDetailsData extends YXBaseBean {
        public int id;
        public int userId;
        public String realName;
        public String mobilePhone;
        public String email;
        public int stage;
        public int subject;
        public int userStatus;
        public String ucnterId;
        public int sex;
        public String school;
        public String avatar;
        public String stageName;
        public String imTokenInfo;
        public String subjectName;
        public String sexName;
        public Aui aui;

        public class Aui extends YXBaseBean {
            public int userId;
            public String idCard;
            public String province;
            public String city;
            public String country;
            public String area;
            public String schoolType;
            public String nation;
            public String title;
            public String recordeducation;
            public String graduation;
            public String professional;
            public String childprojectId;
            public String childprojectName;
            public String organizer;
            public String job;
            public String telephone;
            public String remarks;
        }


    }
}
