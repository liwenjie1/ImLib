package com.yanxiu.im.db;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

/**
 * group表
 * Created by 戴延枫 on 2018/5/7.
 */

public class DbGroup extends DataSupport {
    @Column(unique = true, defaultValue = "unknown", nullable = false)
    private long groupId;
    private String groupName;

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
