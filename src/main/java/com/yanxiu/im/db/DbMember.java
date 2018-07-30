package com.yanxiu.im.db;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * member信息表
 * Created by 戴延枫 on 2018/5/7.
 */

public class DbMember extends DataSupport implements Serializable{
    @Column(unique = true, defaultValue = "unknown", nullable = false)
    private long imId;
    private String name;
    private String avatar;

    //成员角色 区分
    @Column(ignore = true)
    private int role=0;

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    private List<DbTopic> topics = new ArrayList<>();   // 表明此用户加入了哪些topic

    public long getImId() {
        return imId;
    }

    public void setImId(long imId) {
        this.imId = imId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public List<DbTopic> getTopics() {
        return topics;
    }

    public void setTopics(List<DbTopic> topics) {
        this.topics = topics;
    }
}
