package com.yanxiu.im.business.contacts.areas;


import com.yanxiu.lib.yx_basic_library.base.bean.YXBaseBean;

import java.util.ArrayList;

/**
 * 地区的数据类
 * Created by 戴延枫 on 2018年6月24日.
 */

public class AreaBean extends YXBaseBean {

    private String id;
    private String name;
    private ArrayList<AreaBean> sub = new ArrayList();

    public boolean isSelect = false;//是否被选中

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<AreaBean> getSub() {
        return sub;
    }

    public void setSub(ArrayList<AreaBean> sub) {
        this.sub = sub;
    }
}
