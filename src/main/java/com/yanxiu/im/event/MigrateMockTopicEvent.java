package com.yanxiu.im.event;

import com.yanxiu.lib.yx_basic_library.base.bean.YXBaseEvent;

/**
 * http拉取topic后，有mockTopic同realTopic合并完成
 * 发送者：{@link com.yanxiu.im.manager.DatabaseManager}
 * 接收者：{@link com.yanxiu.im.business.topiclist.fragment.ImTopicListFragment}
 * Created by 戴延枫 on 2018/5/23.
 */

public class MigrateMockTopicEvent extends YXBaseEvent {

    private MigrateMockTopicEvent() {

    }

    public MigrateMockTopicEvent(long topicId) {
        this.topicId = topicId;
    }

    public long topicId;
}
