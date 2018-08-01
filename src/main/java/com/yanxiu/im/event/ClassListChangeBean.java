package com.yanxiu.im.event;

import com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment;
import com.yanxiu.lib.yx_basic_library.base.bean.YXBaseEvent;

/**
 * Created by 朱晓龙 on 2018/5/7 12:17.
 * 发送者 {@link ImTopicListFragment} 接收到MQTT 通知后 发送事件
 * 接收者 {主项目中 ClassManageActivity} 收到事件后刷新
 * 班级列表 并给出提示
 */

public class ClassListChangeBean extends YXBaseEvent{
    public String type;

    public ClassListChangeBean(String type) {
        this.type = type;
    }
}
