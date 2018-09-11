package com.yanxiu.im.manager;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.bean.net_bean.ImMember_new;
import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.im.db.DbGroup;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.db.DbMsg;
import com.yanxiu.im.db.DbMyMsg;
import com.yanxiu.im.db.DbTopic;
import com.yanxiu.im.event.MigrateMockTopicEvent;

import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;
import org.litepal.LitePalDB;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 提供数据库的一些操作的方法。
 * Created by 戴延枫 on 2018/5/7.
 */
// litepal已知问题列表
// 1, 只能两表关联，且不支持多个外键（会重名）
// 2, ClusterQuery中每种类型的只能有一次，连续.where两次，则覆盖
public class DatabaseManager {
    public final static int pagesize = 20;
    public final static int minMsgId = -100;

    /**
     * 初始化db。
     * 给每个用户创建并使用自己的数据库，db_<userId>作为数据库名
     * 每个用户使用不同的数据库
     */

    public static void useDbForUser(String userId) {
        if (userId == null) {
            Logger.e("error: create db for " + userId);
            return;
        }

        LitePalDB db = new LitePalDB("new_db_" + userId, 2);
        db.addClassName(DbMember.class.getName());
        db.addClassName(DbTopic.class.getName());
        db.addClassName(DbMsg.class.getName());
        db.addClassName(DbMyMsg.class.getName());
        db.addClassName(DbGroup.class.getName());
        LitePal.use(db);
    }

    /**
     * 获取改topic里，从startMsgId开始的，count条数的msglist。
     * 注释：
     * 1.返回的msgList以msgId为基础倒序；
     * <p>
     * 2.msg分为otherMsg和myMsg
     * 2.1：otherMsg以msgId为基础倒序，
     * 2.2：myMsg以相同msgId为一组，按照sendtime倒序
     * <p>
     * 3.返回的msglist数量可能会大于count。
     *
     * @param startMsgId : 最小的msgId. 传-100为从msgid最大的一条msg开始
     * @param count      : 每页的数据个数，默认同server保持一致为20
     * @return 返回merge后的数组，如果数据足够，数组size() >= count，如果数组size()小于count值或者为空，则表明已经取完所有数据
     */
    @Deprecated
    public static ArrayList<MsgItemBean> getTopicMsgs_old(long topicId, long startMsgId, int count) {
        List<DbMsg> otherMsgList = null; //DB里的别人发的消息，倒序
        List<DbMsg> mergeMsgList = new ArrayList<>(); // merge结果,倒序
        ArrayList<MsgItemBean> msgItemBeanArrayList = new ArrayList<>();//返回给ui的数据

        //1.查询出count数量的otherMsg
        if (startMsgId == -100) {
            otherMsgList = DataSupport
                    .where("topicId = ?", Long.toString(topicId))
                    .limit(count)
                    .order("msgid desc")
                    .find(DbMsg.class);   // server按照msgId插入，这里也可以用sendtime

        } else {
            otherMsgList = DataSupport
                    .where("topicId = ? and msgid < ?",
                            Long.toString(topicId),
                            Long.toString(startMsgId))
                    .limit(count)
                    .order("msgid desc")
                    .find(DbMsg.class);   // server按照msgId插入，这里也可以用sendtime
        }

        if (otherMsgList == null || otherMsgList.isEmpty()) {
            if (startMsgId == -100) { //这是说明没有otherMsg，但是可能有mymsg，这时的mymsgid = -1
                //查出是否有mymsgid = -1的mymsg
                checkMyMsgExistAndMerge(msgItemBeanArrayList, topicId, count);
                return msgItemBeanArrayList;
            } else {
                return null;
            }
        }
        //2.通过otherMsgId，查找到相同msgid的mymsg，且合并msg和mymsg
        for (int i = 0; i < otherMsgList.size(); i++) { //遍历otherMsgs
            DbMsg dbOtherMsg = otherMsgList.get(i);
            long otherMsgId = dbOtherMsg.getMsgId();//otherMsgId

            //通过otherMsgId，查找到相同msgid的mymsg
            List<DbMyMsg> myMsgList = DataSupport
                    .where("topicId = ? and msgid = ?",
                            Long.toString(topicId),
                            Long.toString(otherMsgId))
                    .order("sendtime desc")
                    .find(DbMyMsg.class);

            //把otherMsgId下的myMsg和otherMsg，merge后放入resultMsgList
            if (myMsgList != null && !myMsgList.isEmpty()) {
                mergeMsgList.addAll(myMsgList); // 因为是倒序，所以把mymsg先添加进去
            }
            mergeMsgList.add(dbOtherMsg);//添加otherMsg
        }

        //3.保留最接近count数量的msg，多余的丢弃
        for (int j = 0; j < mergeMsgList.size(); j++) {
            DbMsg mergeMsg = mergeMsgList.get(j);
            if (!(mergeMsg instanceof DbMyMsg)) { //是otherMsg
                //只检查otherMsg的index
                if ((j + 1) >= count) { //超出了count，终止循环
                    msgItemBeanArrayList.add(changeDbMsgToMsgItemBean(mergeMsg));
                    break;
                }
            }
            msgItemBeanArrayList.add(changeDbMsgToMsgItemBean(mergeMsg));
        }

        //4.因为有mymsgid = -1的情况，所以，每次都需要检查otherMsg是否还有下一页，如果没有，那么要检查是否存在mymsgid = -1的mymsg
        checkMyMsgExistAndMerge(msgItemBeanArrayList, topicId, count);

        return msgItemBeanArrayList;
    }

    /**
     * 获取改topic里，从startMsgId开始的，count条数的msglist。
     * 注释：
     * 1.返回的msgList以msgId为基础倒序；
     * <p>
     * 2.msg分为otherMsg和myMsg
     * 2.1：otherMsg以msgId为基础倒序，
     * 2.2：myMsg以相同msgId为一组，按照sendtime倒序
     * <p>
     * 3.返回的msglist数量可能会大于count。
     *
     * @param startMsgId : 最小的msgId. 传-100为从msgid最大的一条msg开始
     * @param count      : 每页的数据个数，默认同server保持一致为20
     * @return 返回merge后的数组，如果数据足够，数组size() >= count，如果数组size()小于count值或者为空，则表明已经取完所有数据
     */
    public static ArrayList<MsgItemBean> getTopicMsgs(long topicId, long startMsgId, int count) {
        List<DbMsg> otherMsgList = null; //DB里的别人发的消息，倒序
        List<DbMsg> mergeMsgList = new ArrayList<>(); // merge结果,倒序
        ArrayList<MsgItemBean> msgItemBeanArrayList = new ArrayList<>();//返回给ui的数据

        //1.查询出count数量的otherMsg
        if (startMsgId == -100) {
            otherMsgList = DataSupport
                    .where("topicId = ?", Long.toString(topicId))
                    .limit(count)
                    .order("msgid desc")
                    .find(DbMsg.class);   // server按照msgId插入，这里也可以用sendtime

        } else {
            otherMsgList = DataSupport
                    .where("topicId = ? and msgid < ?",
                            Long.toString(topicId),
                            Long.toString(startMsgId))
                    .limit(count)
                    .order("msgid desc")
                    .find(DbMsg.class);   // server按照msgId插入，这里也可以用sendtime
        }

        if (otherMsgList == null || otherMsgList.isEmpty()) { //这是说明没有otherMsg，但是可能有mymsg
//            if (startMsgId == -100) {
            getMyMsgWhenNoOtherMsgAndMerge(msgItemBeanArrayList, topicId, startMsgId, count);
            return msgItemBeanArrayList;
//            } else {
//                return null;
//            }
        }
        //2.通过otherMsgId，查找到 otherMsgId < myMsgId < 前一个otherMsgId 和otherMsgId == myMsgId的两个list
        for (int i = 0; i < otherMsgList.size(); i++) { //遍历otherMsgs
            DbMsg dbOtherMsg = otherMsgList.get(i);
            long currentOtherMsgId = dbOtherMsg.getMsgId();//otherMsgId

            //获取上一个msgid，如果没有返回空
            List<DbMsg> previousMsgList = DataSupport
                    .where("topicId = ? and msgid > ?",
                            Long.toString(topicId),
                            Long.toString(currentOtherMsgId))
                    .limit(1)
                    .order("msgid desc")
                    .find(DbMsg.class);
            //2.1 通过otherMsgId，查找到 otherMsgId < myMsgId <  previousOtherMsgId 的数据
            if (previousMsgList == null || previousMsgList.isEmpty()) {
                //没有上一个othermsg，那就查找出msgid > currentOtherMsgId 对应的mymsg
                List<DbMyMsg> myMsgList;
                if (startMsgId == -100) {
                    myMsgList = DataSupport
                            .where("topicId = ? and msgid > ?",
                                    Long.toString(topicId),
                                    Long.toString(currentOtherMsgId))
                            .limit(count)
                            .order("sendtime desc")
                            .find(DbMyMsg.class);
                } else {
                    myMsgList = DataSupport
                            .where("topicId = ? and msgid > ? and msgid < ?",
                                    Long.toString(topicId),
                                    Long.toString(currentOtherMsgId),
                                    Long.toString(startMsgId))
                            .limit(count)
                            .order("sendtime desc")
                            .find(DbMyMsg.class);
                }

                if (myMsgList == null || myMsgList.isEmpty()) { //没有数据，啥也不干

                } else { //添加数据
//                    for (DbMyMsg msg : myMsgList) {
                    mergeMsgList.addAll(myMsgList);//添加myMsg
//                    }
                }
            } else { //有上一个otherMsg，查询出otherMsgId < myMsgId < Math.min(previousOtherMsgId(),startMsgId)的数据
                DbMsg previousOtherMsg = previousMsgList.get(0);
                long minId = Math.min(previousOtherMsg.getMsgId(), startMsgId);//previousId和startid，取一个小值
                List<DbMyMsg> myMsgList = DataSupport
                        .where("topicId = ? and msgid > ? and msgid < ?",
                                Long.toString(topicId),
                                Long.toString(currentOtherMsgId),
                                Long.toString(minId))
                        .limit(count)
                        .order("sendtime desc")
                        .find(DbMyMsg.class);
                if (myMsgList == null || myMsgList.isEmpty()) {
                    //没有数据，啥也不干
                } else {
                    mergeMsgList.addAll(myMsgList);//添加myMsg
                }
            }

            //2.2 通过otherMsgId，查找到 otherMsgId == myMsgId 的数据
            if (mergeMsgList.size() >= count) { //数量已经够了，那么不再添加otherMsgId == myMsgId 的数据了
                break;
            } else {
                //通过otherMsgId，查找到相同msgid的mymsg
                List<DbMyMsg> myMsgList = DataSupport
                        .where("topicId = ? and msgid = ?",
                                Long.toString(topicId),
                                Long.toString(currentOtherMsgId))
                        .order("sendtime desc")
                        .find(DbMyMsg.class);

                //把otherMsgId下的myMsg和otherMsg，merge后放入resultMsgList
                if (myMsgList != null && !myMsgList.isEmpty()) {
                    mergeMsgList.addAll(myMsgList); // 因为是倒序，所以把mymsg先添加进去
                }
                mergeMsgList.add(dbOtherMsg);//添加otherMsg
            }
            if (mergeMsgList.size() >= count)
                break;
        }

        //3.转化数据
        for (int j = 0; j < mergeMsgList.size(); j++) {
            DbMsg mergeMsg = mergeMsgList.get(j);
            msgItemBeanArrayList.add(changeDbMsgToMsgItemBean(mergeMsg));
        }

        //4.因为有otherMsg取完了，但是还有msgid  < otherMsgId 的mymsg的情况，
        // 所以，每次都需要检查otherMsg是否还有下一页，如果没有，那么要检查是否存在msgid  < otherMsgId 的mymsg
        checkMyMsgExistAndMerge(msgItemBeanArrayList, topicId, count);

        return msgItemBeanArrayList;
    }

    /**
     * 没有otherMsg时，查找mymsg
     * 当查找的最后一个msg恰好是id=-1时，一次性把所有-1的msg都返回
     *
     * @param msgItemBeanArrayList
     * @param topicId
     */
    private static void getMyMsgWhenNoOtherMsgAndMerge(ArrayList<MsgItemBean> msgItemBeanArrayList, long topicId, long startMsgId, int count) {
        if (msgItemBeanArrayList == null)
            return;
        //1.查找mymsg
        List<DbMyMsg> myMsgList;
        if (startMsgId == -100) {
            myMsgList = DataSupport
                    .where("topicId = ? ", Long.toString(topicId))
                    .limit(count)
                    .order("sendtime desc")
                    .find(DbMyMsg.class);
        } else {
            myMsgList = DataSupport
                    .where("topicId = ? and msgid < ?",
                            Long.toString(topicId),
                            Long.toString(startMsgId))
                    .limit(count)
                    .order("sendtime desc")
                    .find(DbMyMsg.class);   // server按照msgId插入，这里也可以用sendtime
        }
        if (myMsgList == null || myMsgList.isEmpty()) {
            return;
        }

        //2.添加数据
        for (DbMyMsg dbMyMsg : myMsgList) {
            msgItemBeanArrayList.add(changeDbMsgToMsgItemBean(dbMyMsg));
        }

        //3.拿到最后一个mymsg的id
        DbMyMsg lastMsg = myMsgList.get(myMsgList.size() - 1);//因为按照msgid倒序
        long lastMsgId = lastMsg.getMsgId();
        if (lastMsgId == -1) {
            //查找msgid = -1的mymsg
            List<DbMyMsg> negativeMsgList = DataSupport
                    .where("topicId = ? and msgid = ? and sendtime < ?",
                            Long.toString(topicId),
                            Long.toString(-1),
                            Long.toString(lastMsg.getSendTime()))
                    .order("sendtime desc")
                    .find(DbMyMsg.class);
            if (negativeMsgList != null && !negativeMsgList.isEmpty()) {
                for (DbMyMsg dbMyMsg : negativeMsgList) {
                    msgItemBeanArrayList.add(changeDbMsgToMsgItemBean(dbMyMsg));
                }
            }
        }
    }

    /**
     * 因为有otherMsg取完了，但是还有msgid  < otherMsgId 的mymsg的情况，
     * 所以，每次都需要检查otherMsg是否还有下一页，如果没有，那么要检查是否存在msgid  < otherMsgId 的mymsg
     * ，有就合并进数据里
     *
     * @param msgItemBeanArrayList
     * @param topicId
     */
    private static void checkMyMsgExistAndMerge(ArrayList<MsgItemBean> msgItemBeanArrayList, long topicId, int count) {
        if (msgItemBeanArrayList == null)
            return;
        long minMsgId = -100;
        boolean hasNextPage = true;
        if (msgItemBeanArrayList.isEmpty()) {
            //没有数据，视为没有下一页
            hasNextPage = false;
        } else {
            minMsgId = msgItemBeanArrayList.get(msgItemBeanArrayList.size() - 1).getMsgId();//因为按照msgid倒序，所以最后一个id就是最小的id
            //检测是否还有下一页otherMsg
            List<DbMsg> nextPageMsgList = DataSupport
                    .where("topicId = ? and msgid < ?",
                            Long.toString(topicId),
                            Long.toString(minMsgId))
                    .limit(1)
                    .order("msgid desc")
                    .find(DbMsg.class);
            if (nextPageMsgList == null || nextPageMsgList.isEmpty()) { //没有下一页othermsg
                hasNextPage = false;
            }
        }

        if (!hasNextPage && minMsgId >= 0) { //没有下一页othermsg ，需要检查是否有msgid = -1的mymsg
//            //查找msgid = -1的mymsg
//            List<DbMyMsg> msgList = DataSupport
//                    .where("topicId = ? and msgid = ?",
//                            Long.toString(topicId),
//                            Long.toString(-1))
//                    .order("sendtime desc")
//                    .find(DbMyMsg.class);
//            if (msgList != null && !msgList.isEmpty()) {
//                for (DbMyMsg dbMyMsg : msgList) {
//                    msgItemBeanArrayList.add(changeDbMsgToMsgItemBean(dbMyMsg));
//                }
//            }
            getMyMsgWhenNoOtherMsgAndMerge(msgItemBeanArrayList, topicId, minMsgId, count);

        }
    }

    //topic相关 start

    /**
     * 从数据库重建Topic List，每条topic带最新一页pagesize条msgs，且topic list按照topic里最新的msg排序
     */
    public static List<TopicItemBean> topicsFromDb() {
        List<DbTopic> topics = DataSupport.findAll(DbTopic.class, true);//查询数据库
        if (topics == null || topics.isEmpty()) {
            return null;
        }

        ArrayList<TopicItemBean> topicItemBeans = new ArrayList<>();//返回给UI层的数据

        for (DbTopic topic : topics) {
            if (topic == null)
                continue;
            ArrayList<MsgItemBean> msgs = getTopicMsgs(topic.getTopicId(), -100, 1);

            topic.latestMsgId = -1;// 理论上讲应该每个topic的msgs里至少有一条消息,默认-1
            if ((msgs != null) && (msgs.size() > 0)) {
                topic.latestMsgId = msgs.get(0).getMsgId();
                topic.latestMsgTime = msgs.get(0).getSendTime();
            }
            topic.mergedMsgs = msgs;

            topicItemBeans.add(changeDbTopicToTopicItemBean(topic));
        }

        Collections.sort(topicItemBeans, topicComparator);

        return topicItemBeans;
    }

    /**
     * 更新topic数据库（不更新msglist）
     */
    public static boolean updateTopicWithTopicItemBean(TopicItemBean topicItemBean) {
        List<DbTopic> topics = DataSupport
                .where("topicid = ?", Long.toString(topicItemBean.getTopicId()))
                .find(DbTopic.class, true);

        DbTopic dbTopic = new DbTopic();
        if (topics.size() > 0) {
            dbTopic = topics.get(0);
        }
        dbTopic.setTopicId(topicItemBean.getTopicId());
        dbTopic.setName(topicItemBean.getName());
        dbTopic.setType(topicItemBean.getType());
        dbTopic.setChange(topicItemBean.getChange());
        dbTopic.setGroup(topicItemBean.getGroup());
        dbTopic.setFromTopic(topicItemBean.getFromTopic());
        dbTopic.setFromGroup(topicItemBean.getFromGroup());
        dbTopic.setShowDot(topicItemBean.isShowDot());
        dbTopic.setMembers(topicItemBean.getMembers());
        dbTopic.setLatestMsgId(topicItemBean.getLatestMsgId());
        dbTopic.setLatestMsgTime(topicItemBean.getLatestMsgTime());
        //清空历史部分的标志 字段
        dbTopic.setAlreadyDeletedLocalTopic(topicItemBean.isAlreadyDeletedLocalTopic());
        dbTopic.setLatestMsgIdWhenDeletedLocalTopic(topicItemBean.getLatestMsgIdWhenDeletedLocalTopic());
//        dbTopic.setMergedMsgs(topicItemBean.getLatestPageMsgList());//消息不能直接覆盖，否则mymsg排序就不对了

        return dbTopic.save();
    }

    /**
     * 把DbTopic转化为TopicItemBean
     *
     * @param dbTopic_
     * @return
     */
    public static TopicItemBean changeDbTopicToTopicItemBean(DbTopic dbTopic_) {
        if (dbTopic_ == null)
            return null;
        TopicItemBean topicItemBean = new TopicItemBean();
        topicItemBean.setTopicId(dbTopic_.getTopicId());
        topicItemBean.setName(dbTopic_.getName());
        topicItemBean.setType(dbTopic_.getType());
        topicItemBean.setChange(dbTopic_.getChange());
        topicItemBean.setGroup(dbTopic_.getGroup());
        topicItemBean.setFromTopic(dbTopic_.getFromTopic());
        topicItemBean.setFromGroup(dbTopic_.getFromGroup());
        topicItemBean.setShowDot(dbTopic_.isShowDot());
        topicItemBean.setMembers(dbTopic_.getMembers());
        topicItemBean.setLatestMsgId(dbTopic_.latestMsgId);
        topicItemBean.setLatestMsgTime(dbTopic_.latestMsgTime);
        topicItemBean.setMsgList(dbTopic_.getMergedMsgs());
        topicItemBean.setManagers(dbTopic_.getManagers());

        if (TextUtils.equals("1", topicItemBean.getType())) {
            topicItemBean.setSilence(false);//私聊没有禁言
        } else {
            topicItemBean.setSilence(dbTopic_.speak == 0);// 0开启禁言 1非禁言
        }
        if (dbTopic_.getPersonalConfig() != null) {
            topicItemBean.setBlockNotice(dbTopic_.getPersonalConfig().getQuite() == 1);//1 开启免打扰 0 关闭免打扰
        } else {
            topicItemBean.setBlockNotice(false);
        }

        topicItemBean.setAlreadyDeletedLocalTopic(dbTopic_.isAlreadyDeletedLocalTopic());
        topicItemBean.setLatestMsgIdWhenDeletedLocalTopic(dbTopic_.getLatestMsgIdWhenDeletedLocalTopic());

        return topicItemBean;
    }

    /**
     * 把DbMsg转化为MsgItemBean
     *
     * @param dbMsg
     * @return
     */
    public static MsgItemBean changeDbMsgToMsgItemBean(DbMsg dbMsg) {
        if (dbMsg == null)
            return null;
        MsgItemBean msgItemBean;
        if (dbMsg instanceof DbMyMsg) {
            msgItemBean = new MsgItemBean(MsgItemBean.MSG_TYPE_MYSELF, dbMsg.getContentType());
//            msgItemBean.setType(MsgItemBean.MSG_TYPE_MYSELF);
            msgItemBean.setState(((DbMyMsg) dbMsg).getState());
            msgItemBean.setRealMsgId(((DbMyMsg) dbMsg).getRealMsgId());
        } else {
            msgItemBean = new MsgItemBean(MsgItemBean.MSG_TYPE_OTHER_PEOPLE, dbMsg.getContentType());
            msgItemBean.setRealMsgId(dbMsg.getMsgId());
        }
//        msgItemBean.setType(MsgItemBean.MSG_TYPE_OTHER_PEOPLE);
        msgItemBean.setReqId(dbMsg.getReqId());
        msgItemBean.setMsgId(dbMsg.getMsgId());
        msgItemBean.setTopicId(dbMsg.getTopicId());
        msgItemBean.setSenderId(dbMsg.getSenderId());
        msgItemBean.setSendTime(dbMsg.getSendTime());
//        msgItemBean.setContentType(dbMsg.getContentType());
        msgItemBean.setMsg(dbMsg.getMsg());
        msgItemBean.setThumbnail(dbMsg.getThumbnail());
        msgItemBean.setViewUrl(dbMsg.getViewUrl());
        msgItemBean.setWidth(dbMsg.getWidth());
        msgItemBean.setHeight(dbMsg.getHeight());
        msgItemBean.setLocalViewUrl(dbMsg.getLocalViewUrl());
        return msgItemBean;
    }

    public static Comparator<TopicItemBean> topicComparator = new Comparator<TopicItemBean>() {
        @Override
        public int compare(TopicItemBean t1, TopicItemBean t2) {
            //由于加入了离线消息的时间判断 这里要对离线消息进行比较
            long t1Time = t1.getLatestMsgTime();
            long t2Time = t2.getLatestMsgTime();
            if (t1Time < t2Time) {
                return 1;
            }
            if (t1Time > t2Time) {
                return -1;
            }
            return 0;
        }
    };


    /**
     * 创建一个新的DbMyMsg
     *
     * @param msgItemBean
     * @return
     */
    public static DbMyMsg createDbMyMsgByMsgItemBean(MsgItemBean msgItemBean) {
        if (msgItemBean == null)
            return null;
        DbMyMsg dbMsgResult = new DbMyMsg();
        dbMsgResult.setMsgId(msgItemBean.getMsgId());
        dbMsgResult.setReqId(msgItemBean.getReqId());
        dbMsgResult.setTopicId(msgItemBean.getTopicId());
        dbMsgResult.setState(msgItemBean.getState());
        dbMsgResult.setSenderId(msgItemBean.getSenderId());
        dbMsgResult.setSendTime(msgItemBean.getSendTime());
        dbMsgResult.setContentType(msgItemBean.getContentType());
        dbMsgResult.setMsg(msgItemBean.getMsg());
        dbMsgResult.setThumbnail(msgItemBean.getThumbnail());
        dbMsgResult.setViewUrl(msgItemBean.getViewUrl());
        dbMsgResult.setWidth(msgItemBean.getWidth());
        dbMsgResult.setHeight(msgItemBean.getHeight());
        dbMsgResult.setLocalViewUrl(msgItemBean.getLocalViewUrl());
        return dbMsgResult;
    }

    //topic相关 end

    /**
     * 更新http来的topic：
     * 1.membes
     *
     * @param topic
     * @return
     */
    public static TopicItemBean updateDbTopicWithImTopic(ImTopic_new topic) {
        List<DbTopic> topics = DataSupport
                .where("topicid = ?", Long.toString(topic.topicId))
                .find(DbTopic.class, true);

        DbTopic dbTopic = new DbTopic();
        if (topics.size() > 0) {
            dbTopic = topics.get(0);
        } else { //新创建的topic，需要给一个时间latestTime，用于没有msg列表的topic排序
            dbTopic.setLatestMsgTime(System.currentTimeMillis());
        }
        dbTopic.setLatestMsgId(topic.latestMsgId);
        dbTopic.setTopicId(topic.topicId);
        dbTopic.setName(topic.topicName);
        dbTopic.setType(topic.topicType);
        dbTopic.setChange(topic.topicChange);
        dbTopic.setGroup(topic.topicGroup);
        dbTopic.setSpeak(topic.getSpeak());
        dbTopic.setPersonalConfig(topic.getPersonalConfigInfo());

//        dbTopic.getMembers().clear();
//
//        for (ImTopic_new.Member member : topic.members) {
//            ImMember_new imMember = member.memberInfo;
//            DbMember dbMember = updateDbMemberWithImMember(imMember);
//            dbTopic.getMembers().add(dbMember);
//            dbMember.getTopics().add(dbTopic);
//            dbMember.save();
//        }
        //更新 member 信息
        long start = System.currentTimeMillis();
        if (topic.members != null) {
            //保存管理员信息
            //获取管理员信息
            ArrayList<Long> managerIds = new ArrayList<>();
            for (ImTopic_new.Member member : topic.members) {
                if (member.memberRole == 2) {
                    managerIds.add(member.memberInfo.imId);
                }
            }
            dbTopic.setManagers(managerIds);
        }
        updateMembers(dbTopic, topic.members);
//        Log.i("dbupdate", "更新 member 花费  "+(System.currentTimeMillis()-start));
        updateMembersThatNeedUpdate(dbTopic, topic);

        dbTopic.save();
        return changeDbTopicToTopicItemBean(dbTopic);
    }


    public static void updateTopicMembersWithImTopic(TopicItemBean localTopic, List<ImTopic_new.Member> imMembers) {

    }

    /**
     * 获取全部member
     * 给通讯录使用
     *
     * @return
     */
    public static List<DbMember> getAllDbMembers() {
        List<DbMember> members = DataSupport.findAll(DbMember.class);
        return members;
    }


    /**
     * 获取member
     *
     * @param memberId
     * @return
     */
    public static DbMember getMemberById(long memberId) {
        DbMember member = null;
        List<DbMember> members = DataSupport
                .where("imid = ?", Long.toString(memberId))
                .find(DbMember.class, true);

        if (members.size() > 0) {
            member = members.get(0);
        }
        return member;
    }


    private static void updateMembers(DbTopic dbTopic, List<ImTopic_new.Member> members) {
        if (members == null || members.size() == 0) {
            //清空 member 信息
            dbTopic.setMembers(new ArrayList<DbMember>());
            return;
        }
        StringBuilder url = new StringBuilder();
        url.append("imid in (");
        for (ImTopic_new.Member member : members) {
            url.append(member.memberInfo.imId);
            url.append(",");
        }
        url.setLength(url.length() - 1);
        url.append(")");
        //找到所有数据库中有的 members
        final List<DbMember> memberAlreadyInDb = DataSupport.where(url.toString()).find(DbMember.class);

        List<DbMember> insertMembers = new ArrayList<>();
        for (ImTopic_new.Member imMember : members) {
            boolean has = false;
            //去除数据库中有的
            for (DbMember dbMember : memberAlreadyInDb) {
                if (imMember.memberInfo.imId == dbMember.getImId()) {
                    //更新 更新数据库中已有的
                    dbMember.setRole(imMember.memberInfo.memberType);
                    dbMember.setAvatar(imMember.memberInfo.avatar);
                    dbMember.setName(imMember.memberInfo.memberName);
                    dbMember.getTopics().add(dbTopic);
                    has = true;
                    break;
                }
            }
            //数据库中没有的 进行insert操作 加入到待保存列表
            if (!has) {
                DbMember dbMember = new DbMember();
                dbMember.setImId(imMember.memberInfo.imId);
                dbMember.setRole(imMember.memberInfo.memberType);
                dbMember.setAvatar(imMember.memberInfo.avatar);
                dbMember.setName(imMember.memberInfo.memberName);
                dbMember.getTopics().add(dbTopic);
                insertMembers.add(dbMember);
            }
        }
        // 合并 update 与 insert
        memberAlreadyInDb.addAll(insertMembers);
        DataSupport.saveAll(memberAlreadyInDb);
        //内存更新
        dbTopic.getMembers().clear();
        dbTopic.getMembers().addAll(memberAlreadyInDb);
    }

    /**
     * 将topic新增的member，和有信息改变的member，存DB
     */
    private static void updateMembersThatNeedUpdate(DbTopic dbTopic, ImTopic_new imTopic) {
//        List<DbMember> ret = new ArrayList<>();
        if (imTopic.members == null) {
            return;
        }
        for (ImTopic_new.Member member : imTopic.members) {
            ImMember_new imMember = member.memberInfo;
            DbMember dbMember = null;
            for (DbMember m : dbTopic.getMembers()) {
                if (m.getImId() == imMember.imId) { //本地已经含有该member
                    dbMember = m;
                    break;
                }
            }

            if (dbMember == null) {
                // 此member为新增人员
                dbMember = updateDbMemberWithImMember(imMember);
                dbTopic.getMembers().add(dbMember);
                dbMember.getTopics().add(dbTopic);
                dbMember.save();
            } else {
                // 此member为已有人员
                if (hasMemberUpdate(dbMember, imMember)) {
                    // 且此member有更新信息
                    dbMember.setName(imMember.memberName);
                    dbMember.setAvatar(imMember.avatar);
                    dbMember.save();
                }
            }
        }
    }

    /**
     * 判断相比于DB中的dbMember,HTTP返回的imMember是否有改动
     */
    private static boolean hasMemberUpdate(DbMember dbMember, ImMember_new imMember) {
        boolean ret = false;
        if (!dbMember.getName().equals(imMember.memberName)) {
            ret = true;
        }

        if (!dbMember.getAvatar().equals(imMember.avatar)) {
            ret = true;
        }

        return ret;
    }

    /**
     * 新增member，保存member
     *
     * @param member
     * @return
     */
    public static DbMember updateDbMemberWithImMember(ImMember_new member) {
        List<DbMember> members = DataSupport
                .where("imid = ?", Long.toString(member.imId))
                .find(DbMember.class, true);

        DbMember dbMember = new DbMember();
        if (members.size() > 0) {
            dbMember = members.get(0);
        }
        dbMember.setImId(member.imId);
        dbMember.setName(member.memberName);
        dbMember.setAvatar(member.avatar);
        dbMember.save();
        return dbMember;
    }

    /**
     * 创建一个 member 来组成 mocktopic
     */
    public static DbMember createMockMemberForMockTopic(long memberId, String memberName) {
        DbMember dbMember = new DbMember();
        dbMember.setImId(memberId);
        dbMember.setName(memberName);
        dbMember.setAvatar("");
        dbMember.save();
        return dbMember;
    }


    /**
     * 更新http、mqtt来的消息并保存
     *
     * @param curUserImId : 当前app的登录用户的imId
     * @return 返回该消息对应的MsgItemBean
     */
    public static MsgItemBean updateDbMsgWithImMsg(ImMsg_new msg, long curUserImId) {
        DbMsg theMsg;
        if (msg.senderId == curUserImId) { //我的消息
            // 我发的消息不入库，以后有删除后，重拉消息列表时，应该入DbMyMsg库
            DbMyMsg dbMyMsg = DataSupport
                    .where("reqid = ?", msg.reqId)
                    .findFirst(DbMyMsg.class);
            if (dbMyMsg == null) { //本地没有的我的消息，完全按照server的来
                dbMyMsg = new DbMyMsg();
                dbMyMsg.setMsgId(msg.msgId);
                dbMyMsg.setState(DbMyMsg.State.Success.ordinal());
                dbMyMsg.setSendTime(msg.sendTime);
            } else { //本地已有的mymsg，msgid不能能变

            }

            dbMyMsg.setState(DbMyMsg.State.Success.ordinal());    // http来的消息都是以完成的消息
            theMsg = dbMyMsg;

            theMsg.setReqId(msg.reqId);
            theMsg.setTopicId(msg.topicId);
            theMsg.setSenderId(msg.senderId);
            theMsg.setContentType(msg.contentType);
            theMsg.setMsg(msg.contentData.msg);
            theMsg.setThumbnail(msg.contentData.thumbnail);
            theMsg.setViewUrl(msg.contentData.viewUrl);
            theMsg.setWidth(msg.contentData.width);
            theMsg.setHeight(msg.contentData.height);
            dbMyMsg.setRealMsgId(msg.msgId);
        } else { //别人的消息
            DbMsg dbMsg = DataSupport
                    .where("reqid = ?", msg.reqId)
                    .findFirst(DbMsg.class);
            if (dbMsg == null) {
                dbMsg = new DbMsg();
            }

            theMsg = dbMsg;

            theMsg.setReqId(msg.reqId);
            theMsg.setMsgId(msg.msgId);
            theMsg.setTopicId(msg.topicId);
            theMsg.setSenderId(msg.senderId);
            theMsg.setSendTime(msg.sendTime);
            theMsg.setContentType(msg.contentType);
            theMsg.setMsg(msg.contentData.msg);
            theMsg.setThumbnail(msg.contentData.thumbnail);
            theMsg.setViewUrl(msg.contentData.viewUrl);
            theMsg.setWidth(msg.contentData.width);
            theMsg.setHeight(msg.contentData.height);
        }

        theMsg.save();
        return changeDbMsgToMsgItemBean(theMsg);
    }

    /**
     * httpl来的mymsg，更改状态为成功
     * 或许不需要，暂时加上吧
     *
     * @param msg
     * @return
     */
    public static MsgItemBean updateMyMsgDBToSuccess(ImMsg_new msg) {
        if (msg.senderId == Constants.imId) {
            // 我发的消息
            DbMyMsg dbMyMsg = DataSupport
                    .where("reqid = ?", msg.reqId)
                    .findFirst(DbMyMsg.class);
            if (dbMyMsg == null) {
                return null;
            }
            dbMyMsg.setState(DbMyMsg.State.Success.ordinal());    // http来的消息都是以完成的消息
            dbMyMsg.save();
            return changeDbMsgToMsgItemBean(dbMyMsg);
        }
        return null;
    }

    /**
     * 更新msg,给Sender更新数据使用
     *
     * @param msgItemBean
     * @return true:成功
     */
    public static boolean updateDbMsgWithMsgItemBean(MsgItemBean msgItemBean) {
        DbMyMsg theMsg = DataSupport
                .where("reqid = ?", msgItemBean.getReqId())
                .findFirst(DbMyMsg.class);
        if (theMsg == null) { //数据库中没有这条消息，无法修改状态
            return false;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put("msgId", msgItemBean.getMsgId());
        contentValues.put("sendTime", msgItemBean.getSendTime());
        contentValues.put("state", msgItemBean.getState());
        contentValues.put("topicId", msgItemBean.getTopicId());
        contentValues.put("thumbnail", msgItemBean.getThumbnail());
        contentValues.put("viewUrl", msgItemBean.getViewUrl());
        contentValues.put("width", msgItemBean.getWidth());
        contentValues.put("height", msgItemBean.getHeight());
        contentValues.put("localViewUrl", msgItemBean.getLocalViewUrl());
        contentValues.put("realMsgId", msgItemBean.getRealMsgId());

        int updateCount = DataSupport.updateAll(DbMyMsg.class, contentValues, "reqid = ?", msgItemBean.getReqId());
//        theMsg = DataSupport
//                .where("reqid = ?", dbMsg.getReqId())
//                .findFirst(DbMyMsg.class);
        return updateCount > 0 ? true : false;
    }


    /**
     * 根据给定的topic id 删除数据库信息
     */
    public static void deleteTopicById(long topicId) {
        Log.i("mockTopic", "deleteTopicById: " + topicId);
        DataSupport.deleteAll(DbTopic.class,
                "topicId = ? ", String.valueOf(topicId));

    }

    /**
     * 清空topic的本地聊天记录
     * 根据给定的topic id 清空msg信息
     */
    public static void deleteLocalMsgByTopicId(TopicItemBean topicItemBean) {
        DataSupport.deleteAll(DbMsg.class,
                "topicId = ? ", String.valueOf(topicItemBean.getTopicId()));
        DataSupport.deleteAll(DbMyMsg.class,
                "topicId = ? ", String.valueOf(topicItemBean.getTopicId()));

        DbTopic dbTopic = getTopicById(topicItemBean.getTopicId());
        if (dbTopic != null) {
            dbTopic.setAlreadyDeletedLocalTopic(true);
            dbTopic.setLatestMsgIdWhenDeletedLocalTopic(topicItemBean.getLatestMsgId());
            dbTopic.save();
        }
    }


    //mockTpoic start

    /**
     * 创建mockTopic
     * 注释：1.mockTopicId 从负max开始++
     *
     * @param otherMemberId 私聊的另一个人的id
     * @param fromTopicId
     * @return
     */
    public static TopicItemBean createMockTopic(long otherMemberId, long fromTopicId) {
        DbTopic lastMockTopic = DataSupport
                .where("topicid < ?", "0")
                .order("topicid asc")
                .findLast(DbTopic.class);

        DbTopic mockTopic = new DbTopic();
        if (lastMockTopic == null) { //没有负数的mockTopic
            mockTopic.setTopicId(Long.MIN_VALUE);//第一个mockTopic，id从-max开始
        } else {
            mockTopic.setTopicId(lastMockTopic.getTopicId() + 1);//有mocktopic，id++
        }

        mockTopic.setName("mock name");
        mockTopic.setType("1");
        mockTopic.setChange("mock change");
        mockTopic.setGroup("mock group");
        //用于排序
        mockTopic.setLatestMsgTime(System.currentTimeMillis());

        DbMember myself = getMemberById(Constants.imId);
        DbMember member = getMemberById(otherMemberId);
        mockTopic.getMembers().add(myself);
        mockTopic.getMembers().add(member);
        if (member != null) {
            mockTopic.setName(member.getName());
        }
        if (fromTopicId > 0) { // 来自群聊点击的私聊
            mockTopic.setFromTopic(Long.toString(fromTopicId));
        }
        boolean isSuccess = mockTopic.save();
        if (isSuccess)
            return changeDbTopicToTopicItemBean(mockTopic);

        return null;
    }

    /**
     * 获取数据库里所有mockTopic
     *
     * @return
     */
    public static List<DbTopic> getMockTopic() {
        List<DbTopic> mockTopicList = DataSupport
                .where("topicid < ?", "0")
                .order("topicid asc")
                .find(DbTopic.class, true);

        if (mockTopicList == null || mockTopicList.isEmpty()) { //没有负数的mockTopic
            return null;
        }
        return mockTopicList;
    }

    /**
     * 当http获取topic更新后，检查mockTopic。如有对应的realTopic，合并
     * 注释：如果私聊topic中，俩人相同，那么就是同一个私聊topic。
     * 例s子：topic1中，member有A、B， topic2中，member有A、B，
     * 因为A、B俩成员相同，那么就是同一个私聊
     *
     * @return
     */
    public static void checkAndMigrateMockTopic(List<TopicItemBean> topicList) {
        List<DbTopic> mockTopicList = getMockTopic();

        if (mockTopicList != null && !mockTopicList.isEmpty()) {
            //获取全部私聊的realTopic
            List<DbTopic> allPrivateRealTopicList = DataSupport
                    .where("topicid >= ? and type = ?", "0", "1")
                    .order("topicid asc")
                    .find(DbTopic.class, true);

            if (allPrivateRealTopicList == null || allPrivateRealTopicList.isEmpty()) {
                return;
            }
            for (int i = 0; i < mockTopicList.size(); i++) { //获取每一个mockTopic

                DbTopic mockTopic = mockTopicList.get(i);
                if (mockTopic.getMembers() == null || mockTopic.getMembers().size() < 2)
                    continue;
                //因为是私聊，所以只能有两个member
                DbMember mockMember1 = mockTopic.getMembers().get(0);
                DbMember mockMember2 = mockTopic.getMembers().get(1);

                long mockMemberId1 = mockMember1.getImId();
                long mockMemberId2 = mockMember2.getImId();


                for (int j = 0; j < allPrivateRealTopicList.size(); j++) { //遍历私聊的realTopic

                    DbTopic privateRealTopic = allPrivateRealTopicList.get(j);
                    if (privateRealTopic.getMembers() == null || privateRealTopic.getMembers().size() < 2)
                        continue;
                    //因为是私聊，所以只能有两个member
                    DbMember realMember1 = privateRealTopic.getMembers().get(0);
                    DbMember realMember2 = privateRealTopic.getMembers().get(1);
                    long realMemberId1 = realMember1.getImId();
                    long realMemberId2 = realMember2.getImId();
                    if ((mockMemberId1 == realMemberId1 && mockMemberId2 == realMemberId2) || (mockMemberId1 == realMemberId2 && mockMemberId2 == realMemberId1)) {
                        //只要私聊人员相同，那么就是同一个私聊topic
                        migrateMockTopicToRealTopic(mockTopic, privateRealTopic, topicList);
                        break;
                    }

                }
            }

        }

    }

    /**
     * 把mockTopic和对应的realTopic合并
     *
     * @param mockTopic
     * @param realTopic
     */
    private static void migrateMockTopicToRealTopic(@NonNull DbTopic mockTopic, @NonNull DbTopic realTopic, List<TopicItemBean> topicList) {
        //1.保留mockTopicId,在修改msg时使用
        long mockTopicId = mockTopic.getTopicId();

        //2.通过mockTopicId，在map中找到内存中的topic，
//        List<TopicItemBean> topicList = SharedSingleton.getInstance().get(SharedSingleton.KEY_TOPIC_LIST);
        TopicItemBean mockTopicItemBean = null;//该对象就是在UI里显示的bean对象， migrate的本质目的就是修改该对象里的数据，保证对象不变。
        for (int i = 0; i < topicList.size(); i++) {
            if (mockTopicId == topicList.get(i).getTopicId()) { //找到对应的topic
                mockTopicItemBean = topicList.get(i);
                break;
            }
        }
        if (mockTopicItemBean == null) {
            return;
        }

        //3.修改内存中mockTopicBean
        mockTopicItemBean.setTopicId(realTopic.getTopicId());
        mockTopicItemBean.setName(realTopic.getName());
        mockTopicItemBean.setGroup(realTopic.getGroup());
        mockTopicItemBean.setChange(realTopic.getChange());
        mockTopicItemBean.setFromGroup(realTopic.getFromGroup());
        mockTopicItemBean.setFromTopic(realTopic.getFromTopic());

        //4.修改mockTopicBean中的msg，以及msg数据库并保存
        migrateMsgsForMockTopic(mockTopicItemBean, mockTopicId);//这步骤结束之后，mockTopicItemBean其实就已经是realTopicBean了，且保证了内存中对象不变
        //5.把realTopic中的msg合并到mockTopicItemBean中。
        //注释：mockTopic里的msg的msgid应该全部都是-1。因为msg为倒序，且realTopic里的msgid都大于0，
        // 所以需要把realTopic里的msg添加到内存的mockTopic里msglist的头部
        List<MsgItemBean> realMsgs = getTopicMsgs(realTopic.getTopicId(), -100, pagesize);
        if (realMsgs != null && !realMsgs.isEmpty()) {
            mockTopicItemBean.getMsgList().clear();
            mockTopicItemBean.getMsgList().addAll(realMsgs);
        }
        //6.删除mockTopic
        deleteTopicById(mockTopicId);

        //7.发送eventbus，通知UI
        EventBus.getDefault().post(new MigrateMockTopicEvent(realTopic.getTopicId()));
    }

    /**
     * 判断是否是mockTopic
     * 负数为临时topic
     */

    public static boolean isMockTopic(TopicItemBean topic) {
        if (topic == null) {
            return true;
        }

        return topic.getTopicId() < 0 ? true : false;
    }

    /**
     * 更新 重发消息的数据库状态
     * 由于直接采用save()方法 会出现 失败的异常
     * 采用数据库更新对已经存在数据库中的 未发送成功的数据进行状态参数的更新
     */
    public static boolean createOrUpdateMyMsg(MsgItemBean msgItemBean) {
        DbMyMsg theMsg = DataSupport
                .where("reqid = ?", msgItemBean.getReqId())
                .findFirst(DbMyMsg.class);
        if (theMsg == null) { //新发消息
            //如果 数据库中还没有保存这条信息  创建并保存
            DbMyMsg dbMyMsg = createDbMyMsgByMsgItemBean(msgItemBean);
            return dbMyMsg.save();
        }
        //重发
        ContentValues contentValues = new ContentValues();
//        contentValues.put("msgId", dbMsg.getMsgId());
        contentValues.put("sendTime", msgItemBean.getSendTime());
        contentValues.put("state", msgItemBean.getState());
        contentValues.put("topicId", msgItemBean.getTopicId());
        contentValues.put("thumbnail", msgItemBean.getThumbnail());
        contentValues.put("viewUrl", msgItemBean.getViewUrl());
        contentValues.put("width", msgItemBean.getWidth());
        contentValues.put("height", msgItemBean.getHeight());
        contentValues.put("localViewUrl", msgItemBean.getLocalViewUrl());

        int updateCount = DataSupport.updateAll(DbMyMsg.class, contentValues, "reqid = ?", msgItemBean.getReqId());
//        theMsg = DataSupport
//                .where("reqid = ?", dbMsg.getReqId())
//                .findFirst(DbMyMsg.class);
        return updateCount > 0 ? true : false;
    }

    /**
     * 把mockTopic替换为realTopic
     *
     * @param mockTopicBean mockTopic的bean对象
     * @param realImTopic   server返回的realtopic的ImTopic数据
     */
    public static void migrateMockTopicToRealTopic(@NonNull TopicItemBean mockTopicBean, @NonNull ImTopic_new realImTopic) {
        //保留mockTopicId,在修改msg时使用
        long mockTopicId = mockTopicBean.getTopicId();
        //1.找到mockTopic
        DbTopic dbTopic = getTopicById(mockTopicBean.getTopicId());
        if (dbTopic == null) { //TODO 没有该mockTopic，是否应该视为失败？
            return;
        }
        //2.修改内存中mockTopicBean
        mockTopicBean.setTopicId(realImTopic.topicId);
        mockTopicBean.setName(realImTopic.topicName);
        mockTopicBean.setGroup(realImTopic.topicGroup);
        mockTopicBean.setChange(realImTopic.topicChange);
        //3.修改数据库
        dbTopic.setTopicId(realImTopic.topicId);
        dbTopic.setName(realImTopic.topicName);
        dbTopic.setGroup(realImTopic.topicGroup);
        dbTopic.setChange(realImTopic.topicChange);
        dbTopic.save();

        //替换相关msg
        migrateMsgsForMockTopic(mockTopicBean, mockTopicId);

        //发送eventbus，通知UI
        EventBus.getDefault().post(new MigrateMockTopicEvent(mockTopicBean.getTopicId()));
    }

    /**
     * 查找并获取topic
     *
     * @param topicId
     * @return
     */
    public static DbTopic getTopicById(long topicId) {
        List<DbTopic> topics = DataSupport
                .where("topicid = ?", Long.toString(topicId))
                .find(DbTopic.class, true);

        DbTopic dbTopic = new DbTopic();
        if (topics.size() > 0) {
            dbTopic = topics.get(0);
            return dbTopic;
        }
        return null;
    }

    /**
     * 把mockTopic的msg，关联到realtopic上
     * 做两件事：1.msg更新DB。2.更新UI的Msg
     *
     * @param realTopicBean 把msglist替换
     * @param mockTopicId
     */
    public static void migrateMsgsForMockTopic(TopicItemBean realTopicBean, long mockTopicId) {
        // 查询出 topicid = mockTopicId 的所有msg
        List<DbMyMsg> myMsgList = DataSupport
                .where("topicid = ?", Long.toString(mockTopicId))
                .find(DbMyMsg.class);

        if (myMsgList == null || myMsgList.isEmpty()) { //没有msg
//            realTopicBean.getMsgList().clear();
            return;
        }
        //替换msg的topicId为realTopicId
        for (DbMyMsg myMsg : myMsgList) {
            myMsg.setTopicId(realTopicBean.getTopicId());
            myMsg.save();
        }

        //更新内存里的msg的topicId
        for (MsgItemBean msgItemBean : realTopicBean.getMsgList()) {
            msgItemBean.setTopicId(realTopicBean.getTopicId());
        }
    }

    // 将mock topicz中的msg都设置为发送失败
    public static void topicCreateFailed(TopicItemBean mockTopicBean) {
        // 对于DB处理
        List<DbMyMsg> myMsgs = DataSupport
                .where("topicid = ?", Long.toString(mockTopicBean.getTopicId()))
                .find(DbMyMsg.class);

        for (DbMyMsg myMsg : myMsgs) {
            myMsg.setState(DbMyMsg.State.Failed.ordinal());
            myMsg.save();
        }

        // 对于UI处理
        for (MsgItemBean msg : mockTopicBean.getMsgList()) {
            if (msg.getType() == MsgItemBean.MSG_TYPE_MYSELF) {
                msg.setState(DbMyMsg.State.Failed.ordinal());
            }
        }
    }

    //mockTpoic end


}
