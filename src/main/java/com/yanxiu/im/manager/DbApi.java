package com.yanxiu.im.manager;

import android.text.TextUtils;

import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.bean.net_bean.ImMsg_new;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.db.DbMsg;
import com.yanxiu.im.db.DbMyMsg;
import com.yanxiu.im.db.DbTopic;

import org.litepal.crud.ClusterQuery;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * create by 朱晓龙 2018/8/17 上午10:44
 */
public class DbApi {

    /**
     * 更新所有 有更新的 topicitembean
     */
    public static void updateTopics(ArrayList<TopicItemBean> topics) {
        //首先查找到所有 数据库中的 topic
        if (topics == null || topics.size() == 0) {
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (TopicItemBean topic : topics) {
            stringBuilder.append(topic.getTopicId());
            stringBuilder.append(",");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        stringBuilder.append(")");

        ClusterQuery query = DataSupport.where("topicId in " + stringBuilder.toString());
        List<DbTopic> dbTopics = null;
        if (query != null) {
            dbTopics = query.find(DbTopic.class);
        }
        if (dbTopics != null) {
            for (DbTopic dbTopic : dbTopics) {
                for (TopicItemBean update : topics) {
                    if (dbTopic.getTopicId() == update.getTopicId()) {
                        dbTopic.latestMsgId = update.getLatestMsgId();
                        dbTopic.latestMsgTime = update.getLatestMsgTime();
                        dbTopic.setGroup(update.getGroup());
                        dbTopic.setChange(update.getChange());
                        dbTopic.setShowDot(update.isShowDot());
                        dbTopic.setType(update.getType());
                        dbTopic.setFromTopic(update.getFromTopic());
                        dbTopic.setName(update.getName());
                        break;
                    }
                }
            }
        }
        //批量更新
        DataSupport.saveAll(dbTopics);
    }



    /**
     * 批量删除 topic
     */
    public static void deleteTopicList(ArrayList<TopicItemBean> topics) {
        if (topics == null || topics.size() == 0) {
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (TopicItemBean topic : topics) {
            stringBuilder.append(topic.getTopicId());
            stringBuilder.append(",");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        stringBuilder.append(")");

        DataSupport.deleteAll(DbTopic.class, "topicId in " + stringBuilder.toString());
    }



    /**
     * 批量保存 或 更新 member 信息
     * 将获取的 member 信息进行 保存
     * 应存在的进行数据库更新操作
     * 不存在的执行插入操作
     */
    public static ArrayList<DbMember> updateOrSaveMembers(List<ImTopic_new.Member> imMembers) {
        //首先查找到所有 数据库中的 topic
        ArrayList<DbMember> result = new ArrayList<>();
        if (imMembers == null || imMembers.size() == 0) {
            return result;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (ImTopic_new.Member imMember : imMembers) {
            stringBuilder.append(imMember.memberInfo.imId);
            stringBuilder.append(",");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        stringBuilder.append(")");

        ClusterQuery query = DataSupport.where("imId in " + stringBuilder.toString());
        //找到所有已经存在的 member 进行更新操作
        ArrayList<ImTopic_new.Member> newMembers = new ArrayList<>();

        final List<DbMember> dbMembers = query.find(DbMember.class);

        for (ImTopic_new.Member imMember : imMembers) {
            boolean has = false;
            for (DbMember dbMember : dbMembers) {
                if (dbMember.getImId() == imMember.memberInfo.imId) {
                    has = true;
                    dbMember.setName(imMember.memberInfo.memberName);
                    dbMember.setAvatar(imMember.memberInfo.avatar);
                    break;
                }
            }
            if (!has) {
                newMembers.add(imMember);
            }
        }
        final ArrayList<DbMember> dbNewMember = saveMembers(newMembers);
        if (dbNewMember.size() != 0) {
            result.addAll(dbNewMember);
        }
        //将已有的数据添加到结果集合中
        result.addAll(dbMembers);

        return result;
    }
    /**
     * 批量添加 member 到数据库中并返回 dbmember 集合
     */
    private static ArrayList<DbMember> saveMembers(List<ImTopic_new.Member> imMembers) {
        //生成 dbmember 列表
        ArrayList<DbMember> dbMembers = new ArrayList<>();
        for (ImTopic_new.Member imMember : imMembers) {
            DbMember dbMember = new DbMember();
            dbMember.setAvatar(imMember.memberInfo.avatar);
            dbMember.setImId(imMember.memberInfo.imId);
            dbMember.setName(imMember.memberInfo.memberName);
            dbMembers.add(dbMember);
        }
        DataSupport.saveAll(dbMembers);
        return dbMembers;
    }
    /**
     * 批量保存 或更新 msg
     */
    public static boolean updateOrSaveMsgs(ArrayList<ImMsg_new> imMsgList) {
        if (imMsgList == null || imMsgList.size() == 0) {
            return false;
        }
        //首先查找到所有 数据库中的 topic msg 分两个表 db msg 与 dbmy msg

        //首先分两个列表  sender id 为我自己的
        ArrayList<ImMsg_new> myImMsg = new ArrayList<>();
        ArrayList<ImMsg_new> otherImMsg = new ArrayList<>();
        for (ImMsg_new msgNew : imMsgList) {
            if (msgNew.senderId == Constants.imId) {
                myImMsg.add(msgNew);
            } else {
                otherImMsg.add(msgNew);
            }
        }
        /*分别在 不同的表中进行查找*/
        StringBuilder sql = new StringBuilder();
        sql.append("(");
        for (ImMsg_new msgNew : imMsgList) {
            //sqlite 语句中 字符串 需要用' 包围  否则 exception unrecognized token: "xxxx"
            sql.append("'");
            sql.append(msgNew.reqId);
            sql.append("'");
            sql.append(",");
        }
        sql.setLength(sql.length() - 1);
        sql.append(")");
        /*在 msg 表中查找*/
        ClusterQuery msgQuery = DataSupport.where("reqId in " + sql.toString());
        //找到所有已经存在的 member 进行更新操作
        final List<DbMsg> dbMsgs = msgQuery.find(DbMsg.class);
        /*在 mymsg 中查找*/
        ClusterQuery myMsgQuery = DataSupport.where("reqId in " + sql.toString());
        final List<DbMyMsg> dbMyMsgs = myMsgQuery.find(DbMyMsg.class);

        /*去重*/
        ArrayList<DbMsg> toSaveMsg=new ArrayList<>();
        ArrayList<DbMsg> toSaveMyMsg=new ArrayList<>();
        for (ImMsg_new msgNew : otherImMsg) {
            boolean has=false;
            for (DbMsg dbMsg : dbMsgs) {
                if (TextUtils.equals(dbMsg.getReqId(),msgNew.reqId)) {
                    has=true;
                    break;
                }
            }
            if (!has) {
                DbMsg dbMsg=new DbMsg();
                dbMsg.setReqId(msgNew.reqId);
                dbMsg.setMsgId(msgNew.msgId);
                dbMsg.setTopicId(msgNew.topicId);
                dbMsg.setSenderId(msgNew.senderId);
                dbMsg.setSendTime(msgNew.sendTime);
                dbMsg.setContentType(msgNew.contentType);
                dbMsg.setMsg(msgNew.contentData.msg);
                dbMsg.setThumbnail(msgNew.contentData.thumbnail);
                dbMsg.setViewUrl(msgNew.contentData.viewUrl);
                dbMsg.setWidth(msgNew.contentData.width);
                dbMsg.setHeight(msgNew.contentData.height);
                toSaveMsg.add(dbMsg);
            }
        }

        for (ImMsg_new msgNew : myImMsg) {
            boolean has=false;
            for (DbMyMsg dbMyMsg : dbMyMsgs) {
                if (TextUtils.equals(dbMyMsg.getReqId(),msgNew.reqId)) {
                    has=true;
                    /*更新？ 貌似不需要*/
                    break;
                }
            }
            if (!has) {
                DbMyMsg myMsg=new DbMyMsg();
                myMsg.setReqId(msgNew.reqId);
                myMsg.setMsgId(msgNew.msgId);
                myMsg.setTopicId(msgNew.topicId);
                myMsg.setSenderId(msgNew.senderId);

                myMsg.setContentType(msgNew.contentType);
                myMsg.setMsg(msgNew.contentData.msg);
                myMsg.setThumbnail(msgNew.contentData.thumbnail);
                myMsg.setViewUrl(msgNew.contentData.viewUrl);
                myMsg.setWidth(msgNew.contentData.width);
                myMsg.setHeight(msgNew.contentData.height);
                //我的消息特有
                myMsg.setState(DbMyMsg.State.Success.ordinal());
                myMsg.setSendTime(msgNew.sendTime);
                toSaveMyMsg.add(myMsg);
            }
        }

        DataSupport.saveAll(toSaveMsg);
        DataSupport.saveAll(toSaveMyMsg);
        return true;
    }

}
