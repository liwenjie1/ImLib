package com.yanxiu.im.business.utils;

import android.text.TextUtils;

import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.db.DbMember;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by 朱晓龙 on 2018/5/22 14:36.
 * <p>
 * 负责对内存中的topiclist 进行操作
 * 如 查找 等
 */

public class TopicInMemoryUtils {

    /**
     * 根据topicid查找topic
     *
     * @param topicId 目标topic的id
     */
    public static TopicItemBean findTopicByTopicId(long topicId, List<TopicItemBean> topics) {
        if (topics == null) {
            return null;
        }
        for (TopicItemBean topic : topics) {
            if (topic.getTopicId() == topicId) {
                return topic;
            }
        }
        return null;
    }


    /**
     * 根据memberId 找对应的私聊topic
     *
     * @param
     */
    public static TopicItemBean findPrivateTopicByMemberId(long memberId, List<TopicItemBean> topics) {
        if (topics == null || memberId < 0) {
            return null;
        }
        for (TopicItemBean topicItemBean : topics) {
            if (TextUtils.equals("1", topicItemBean.getType()) && topicItemBean.getMembers() != null) {
                for (DbMember memberNew : topicItemBean.getMembers()) {
                    if (memberNew.getImId() == memberId) {
                        return topicItemBean;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 检查目标memberId 是否在 目标topic 的memberlist中
     */
    public static boolean checkMemberInTopic(long memberId, TopicItemBean topic) {

        if (topic == null) {
            return false;
        }

        if (topic.getMembers() == null || topic.getMembers().isEmpty()) {
            return false;
        }

        for (DbMember member : topic.getMembers()) {
            if (member.getImId() == memberId) {
                return true;
            }
        }

        return false;
    }


    /**
     * 获取内存中的memberbean
     */
    public static DbMember findMemberById(long memberId, List<TopicItemBean> topics) {
        DbMember targetMember = null;
        for (TopicItemBean topic : topics) {
            if (topic.getMembers() != null) {
                for (DbMember memberNew : topic.getMembers()) {
                    if (memberNew.getImId() == memberId) {
                        targetMember = memberNew;
                        break;
                    }
                }
            }
            //已经找到 跳出topic 循环
            if (targetMember != null) {
                break;
            }
        }
        return targetMember;
    }

    /**
     * 在列表中移除topic
     */
    public static boolean removeTopicFromListById(long topicId, List<TopicItemBean> topics) {
        for (TopicItemBean topic : topics) {
            if (topic.getTopicId() == topicId) {
                topics.remove(topic);
                return true;
            }
        }
        return false;
    }


    /**
     * msgList 列表的去重处理
     */
    public static void duplicateRemoval(List<MsgItemBean> tempMsgs, List<MsgItemBean> currentMsgList) {
        if (tempMsgs == null || tempMsgs.size() == 0) {
            return;
        }
        //从数据库中获取了一页msg
        //1、对msg列表进行去重
        Iterator<MsgItemBean> dbMsgItemIterator = tempMsgs.iterator();
        MsgItemBean dbMsgItem = null;
        for (MsgItemBean uiMsgItemBean : currentMsgList) {
            while (dbMsgItemIterator.hasNext()) {
                dbMsgItem = dbMsgItemIterator.next();
                if (TextUtils.equals(uiMsgItemBean.getReqId(), dbMsgItem.getReqId())) {
                    dbMsgItemIterator.remove();
                    break;
                }
            }
        }
    }


    /**
     * 加载一页msg到topic.msgList中
     *
     * @param uiMsgList   UI显示的数据集
     * @param loadMsgList 新增的数据 将被插入到UI数据集中
     * @return 第一个loadMsgList 被插入的位置
     */
    public static int mergeAPageMsgsToCurrentList(List<MsgItemBean> uiMsgList, List<MsgItemBean> loadMsgList) {
        int resultIndex = -1;
        if (loadMsgList == null || loadMsgList.size() == 0) {
            if (uiMsgList.size() > 0) {
                return uiMsgList.size() - 1;
            }
            return 0;
        }
        //首先 需要去重
        duplicateRemoval(uiMsgList, loadMsgList);
        //取出第一个loadmsg
        MsgItemBean loadMsg = null;
        MsgItemBean uiMsg = null;
        for (int i = 0; i < loadMsgList.size(); i++) {
            loadMsg = loadMsgList.get(i);
            int position = -1;
            //查找位置 j最大值为 size-1  position最大值为size
            for (int j = 0; j < uiMsgList.size(); j++) {
                uiMsg = uiMsgList.get(j);
                //找到最有一个 msgId 大于 插入的loadmsg 的位置后一个
                if (uiMsg.getMsgId() > loadMsg.getMsgId()) {
                    position = j + 1;
                }
            }
            if (position == -1) {
                uiMsgList.add(loadMsg);
            } else {
                uiMsgList.add(position, loadMsg);
            }

        }
        //返回第一个被插入的位置 最为列表更新的依据
        resultIndex = uiMsgList.indexOf(loadMsgList.get(0));
        return resultIndex;
    }

    /**
     * 每次加载更多后以及受到新消息后 需要进行date显示的处理
     * 预处理 date textview的 visiable
     */
    public static void processMsgListDateInfo(List<MsgItemBean> msgList) {
        if (msgList != null && msgList.size() > 0) {
            MsgItemBean preItem;
            MsgItemBean curItem;
            int length = msgList.size();
            for (int i = 0; i < length; i++) {
                curItem = msgList.get(i);
                if (length - 1 > i) {
                    preItem = msgList.get(i + 1);
                    boolean showDate = (curItem.getSendTime() - preItem.getSendTime() > 5 * 60 * 1000);
                    curItem.setShowDate(showDate);
                } else {
                    curItem.setShowDate(true);
                }
            }
        }
    }

    /**
     * 获取当前消息列表内最小的realmsgid
     */
    public static long getMinMsgBeanRealIdInList(List<MsgItemBean> msgList) {
        long result = Long.MAX_VALUE;

        if (msgList == null || msgList.isEmpty()) {
            return result;
        }

        MsgItemBean latestBean = msgList.get(msgList.size() - 1);
        result = latestBean.getRealMsgId();
        return result;
    }

    /**
     * 获取当前消息列表内最小的msgid
     */
    public static long getMinImMsgIdInList(List<MsgItemBean> msgList) {
        long result = Long.MAX_VALUE;

        if (msgList == null || msgList.isEmpty()) {
            return result;
        }

        MsgItemBean latestBean = msgList.get(msgList.size() - 1);
        result = latestBean.getMsgId();
        return result;

    }

    /**
     * 获取当前消息列表内最大的msgid
     */
    public static long getMaxMsgIdInList(List<MsgItemBean> msgList) {
        long result = Long.MAX_VALUE;

        if (msgList == null || msgList.isEmpty()) {
            return result;
        }

        for (MsgItemBean msgItemBean : msgList) {
            if (msgItemBean.getMsgId() >= result) {
                result = msgItemBean.getMsgId();
            }
        }
        return result;
    }

    /**
     * 判断 新获取的私聊topic 是否在内存中已经有一个mocktopic存在
     */
    public static boolean hasTheSameMockPrivateTopic(List<DbMember> members, List<TopicItemBean> topics) {
        //找到所有私聊topic
        for (TopicItemBean topicInMemory : topics) {
            if (isPrivateTopic(topicInMemory)) {
                //如果是私聊 判断member结构是否一样
                if (isSamePrivateMemberList(members, topicInMemory.getMembers())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSamePrivateMemberList(List<DbMember> memberList1, List<DbMember> memberList2) {
        long member1 = -1;
        long member2 = -1;
        //都不为空才能判断
        if (memberList1 == null && memberList2 == null) {
            return false;
        }

        // 列表长度相等
        if (memberList1.size() != memberList2.size()) {
            return false;
        }
        //两个memberlist都不等于2
        if (memberList1.size() != 2 || memberList2.size() != 2) {
            return false;
        }

        //获取list1中的成员id
        for (DbMember member : memberList1) {
            if (member.getImId() != Constants.imId) {
                member1 = member.getImId();
                break;
            }
        }
        //获取 成员的imid
        for (DbMember member : memberList2) {
            if (member.getImId() != Constants.imId) {
                member2 = member.getImId();
                break;
            }
        }

        if (member1 == -1 || member2 == -1) {
            return false;
        }

        return member1 == member2;
    }

    public static boolean isPrivateTopic(TopicItemBean topic) {
        if (topic == null) {
            return false;
        }
        return TextUtils.equals(topic.getType(), "1");
    }


    /**
     * 检查 当前获取的最新一页消息 与本地的topic 消息是否有更新
     * 是否需要显示红点
     * 1、比较本地数据库 msglist 与 网络 topic 更新后的 latestmsgid
     * 2、比较 本地数据库 msgList 与新获取的 msglist
     */
    public static boolean checkTopicShouldShowRedDot(TopicItemBean topicItem, List<MsgItemBean> newMsgList) {

        //首先是参数检查
        if (topicItem == null) {
            return false;
        }

        //topicbean 信息中携带的 服务器记录的最新 msgid
        long serverLatestMsgId = topicItem.getLatestMsgId();
        //本地数据库 数据列表
        List<MsgItemBean> localMsgList = topicItem.getMsgList();
        //本地数据库中 最新的一条 msgid
        long localLatestMsgId = (localMsgList == null || localMsgList.size() == 0) ? 0 : localMsgList.get(0).getRealMsgId();

        //新创建的 topic
        if (serverLatestMsgId == 0) {
            //情况 1 、本身没有历史消息
            //2、服务器还没有获取到历史消息 就收到了 mqtt 通知
            if (newMsgList == null || newMsgList.isEmpty()) {
                return false;
            } else {
                return true;
            }

        }
        if (localLatestMsgId == 0) {
            //代表 本地没有消息记录 可能是清空了数据库 也可能 没有在线接受过消息
            //而 serverLatestMsgid >0
            return true;
        }

        //如果有新消息页
        if (serverLatestMsgId > localLatestMsgId) {
            //判断最有一条是不是自己的消息
            boolean hasOtherMsg = false;
            for (MsgItemBean bean : newMsgList) {
                if (bean.getType() == MsgItemBean.MSG_TYPE_OTHER_PEOPLE) {
                    if (bean.getMsgId() > localLatestMsgId) {
                        hasOtherMsg = true;
                    }
                }
            }
            return hasOtherMsg;

        }
        return false;
    }


    /**
     * 更新内存中所有 topic 中 相同 member 的信息
     */
    public static void updateMemberInfoInAllTopics(DbMember member, List<TopicItemBean> topics) {
        if (topics == null) {
            return;
        }
        for (TopicItemBean targetTopic : topics) {
            updateMemberInfoInTopic(member, targetTopic);
        }
    }

    /**
     * 更新对应topic下的member信息？
     * 更新所有topic下的这个member信息？
     */
    public static void updateMemberInfoInTopic(DbMember member, TopicItemBean targetToipc) {
        if (targetToipc == null||member==null) {
            return;
        }
        if (targetToipc.getMembers() == null) {
            return;
        }
        for (DbMember dbMember : targetToipc.getMembers()) {
            if (dbMember.getImId() == member.getImId()) {
                //找到member 更新信息 （不更换对象）
                dbMember.setAvatar(member.getAvatar());
                dbMember.setName(member.getName());
                //当前 topic 更新完成
                break;
            }
        }
    }

    public static void updateMsgSenderInfo(DbMember member, ArrayList<TopicItemBean> topics){
        if (topics == null||member==null) {
            return;
        }
        for (TopicItemBean topic : topics) {
            if (topic.getMsgList() != null) {
                for (MsgItemBean msgItemBean : topic.getMsgList()) {
                    if (msgItemBean.getSenderId()==member.getImId()) {
                        msgItemBean.setMember(member);
                    }
                }
            }
        }
    }


    public static void cutoffMsgListByMsgId(long msgId,List<MsgItemBean> msgs){
        ArrayList<MsgItemBean> msgToBeRemoved=new ArrayList<>();
        for (MsgItemBean msg : msgs) {
            if (msg.getRealMsgId()<=msgId) {
                msgToBeRemoved.add(msg);
            }
        }
        msgs.removeAll(msgToBeRemoved);
    }


}
