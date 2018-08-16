package com.yanxiu.im.business.topiclist.interfaces;

import com.yanxiu.im.Constants;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.bean.net_bean.ImTopic_new;
import com.yanxiu.im.business.topiclist.serverApi.TopicApiImpl;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.db.DbTopic;
import com.yanxiu.im.manager.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * create by 朱晓龙 2018/8/16 下午1:30
 * 主要的四项功能  增删改查
 * 针对3个 对象  topic  member msg
 * 仓库不需要输出 对象 外部界面 直接持有 memorycache
 */
public class TopicDataReponsitory {
    /*单例模式*/
    private static TopicDataReponsitory INSTANCE;

    public static void reponsitoryInit(String imid) {
        DatabaseManager.useDbForUser(Long.toString(Constants.imId) + "_db");//todo:应该放在config里面去
    }

    public static TopicDataReponsitory getInstance() {
        if (INSTANCE == null) INSTANCE = new TopicDataReponsitory();
        return INSTANCE;
    }

    /*数据缓存*/
    private ArrayList<TopicItemBean> dataInMemory;

    public TopicDataReponsitory() {
        this.dataInMemory = new ArrayList<>();
    }

    public void releaseData() {
        dataInMemory.clear();
        dataInMemory = null;
        INSTANCE = null;
    }

    /*增   添加新数据到本地  内存+数据库*/

    /**
     * 增加新的 topic 到本地
     */
    public void addTopics() {

    }

    /**
     * 增加 members 到本地
     */
    public void addMembers() {

    }

    /**
     * 增加消息 到本地
     */
    public void addMsgs() {

    }



    /*删  删除数据 数据库 内存 根据情况选择是否即时删除*/

    /**
     * 删除一个 topic
     */
    public void deleteTopics(boolean removeFromMemery) {

    }



    /*改  更新 数据  内存+数据库*/

    /**
     * 更新 topic 数据
     * 直接向服务器请求最新的 topiclist
     */
    public void updateTopicsInfo(String token, final ActionCallback callback) {
        TopicApiImpl.requestUserTopicList(token, new TopicApiImpl.TopicListCallback<ImTopic_new>() {
            @Override
            public void onGetTopicList(ArrayList<ImTopic_new> topicList) {
                if (topicList==null) {
                    callback.onFinished(false,"返回空");
                    return;
                }
                //获取了最新的列表信息
                /*首先 处理 分类 1、需要更新 member 的 2、需要更新 msg 的 3 已经被移除的 4、新添加的*/
                ArrayList<TopicItemBean> deleted = new ArrayList<>();
                ArrayList<ImTopic_new> added = new ArrayList<>();
                ArrayList<TopicItemBean> updated = new ArrayList<>();
                /*后续请求的数据集合*/
                ArrayList<TopicItemBean> needUpdateMember = new ArrayList<>();
                ArrayList<TopicItemBean> needMsgUpdate = new ArrayList<>();
                /*只处理删除*/
                for (TopicItemBean local : dataInMemory) {
                    boolean remain=false;
                    for (ImTopic_new server : topicList) {
                        if (local.getTopicId()==server.topicId) {
                            remain=true;
                            break;
                        }
                    }
                    if (!remain) {
                        deleted.add(local);
                    }
                }
                /*处理 member msg 更新 分类*/
                for (ImTopic_new imTopic : topicList) {
                    boolean hasThis=false;
                    for (TopicItemBean local : dataInMemory) {
                        if (local.getTopicId()==imTopic.topicId) {
                            hasThis=true;
                            int localChange = Integer.valueOf(local.getChange());
                            int serverChange = Integer.valueOf(imTopic.topicChange);
                            long localLatestId = Long.valueOf(local.getLatestMsgId());
                            long serverLatestId = Long.valueOf(imTopic.latestMsgId);
                            local.setTopicId(imTopic.topicId);
                            local.setName(imTopic.topicName);
                            local.setType(imTopic.topicType);
                            local.setChange(imTopic.topicChange);
                            local.setGroup(imTopic.topicGroup);
                            local.setShowDot(serverLatestId > localLatestId);
                            //加入到后续操作的集合中  member 更新的后续 会执行 msg 更新
                            if (serverChange > localChange) {
                                needUpdateMember.add(local);
                            } else {
                                if (serverLatestId > localLatestId) {
                                    needMsgUpdate.add(local);
                                }
                            }
                            updated.add(local);
                        }
                    }
                    if (!hasThis) {
                        added.add(imTopic);
                    }
                }

                //删除 已经退出的 topic
                DatabaseManager.deleteTopicList(deleted);
                dataInMemory.removeAll(deleted);
                //更新所有 健在的topics 主要是更新红点 以及 change msgid 等 member 在member 请求后才更新
                DatabaseManager.updateTopics(updated);
                //新加入的 topic 进行数据库保存操作
                final ArrayList<TopicItemBean> newBeans = DatabaseManager.saveTopics(added);
                dataInMemory.addAll(newBeans);
                //列表更新完成 
                if (callback != null) {
                    callback.onUpdate(dataInMemory);
                    callback.onFinished(true,"完成列表更新");
                }
                //执行下一步 网络请求 对需要更新 member的 topic 进行 member 更新请求
                // TODO: 2018/8/16  需要进行 队列处理
                for (TopicItemBean topicItemBean : needUpdateMember) {
                    updateTopicMembersInfo(topicItemBean);
                }
            }
        });
    }

    /**
     * 更新 member 数据
     * 更新数据库中对应的 memerb 数据
     * 以及 内存中对应 topic 中的 member 信息
     */
    public void updateTopicMembersInfo(final TopicItemBean itemBean) {
        TopicApiImpl.requestTopicInfo(Constants.imToken, String.valueOf(itemBean.getTopicId()), new TopicApiImpl.TopicListCallback<ImTopic_new>() {
            @Override
            public void onGetTopicList(ArrayList<ImTopic_new> topicList) {
                if (topicList==null) {
                    return;
                }
                //成功获取了 topic 的 member 信息
                /*对 member 的处理与 topic 类似但有区别 topic 在 app 中唯一  member 可以为多个 topic 共有 所以 member的存在性判断只能在查找数据库之后*/
                //获取 member 列表
                final List<ImTopic_new.Member> imMembers = topicList.get(0).members;
                final ArrayList<DbMember> members = DatabaseManager.updateOrSaveMembers(imMembers);
                itemBean.setMembers(members);
            }
        });
    }



    /**
     * 更新 topic 下的 msg 信息
     * 可能会用到 比如 更新 mymsg 的数据
     */
    public void loadTopicMsgPage(TopicItemBean itemBean) {

    }


    /*查 从数据库或网络获取数据并缓存在内存中*/
    public void getTopicList(String token, GetTopicListCallback callback) {

        final List<TopicItemBean> dbTopics = DatabaseManager.topicsFromDb();
        if (dbTopics != null) {
            dataInMemory.addAll(dbTopics);
        }
        //唯一一次外部获取 引用 此后 数据层只通知 事件状态 不在返回数据
        callback.onGetTopicList(dataInMemory);
    }

    public interface GetTopicListCallback {
        void onGetTopicList(ArrayList<TopicItemBean> topicItemBeans);
    }

    public void getTopicMembers(String topicId) {

    }

    public void getTopicMsgs(int limit, int offset) {

    }

    /**
     * 获取指定 topic 的完整信息
     * 首先由内存缓存获取
     * 然后数据库读取
     * 最后网络请求获取
     */
    public void getTopicInfo(final String topicId, final ActionCallback callback) {
        long id = -1;
        try {
            id = Long.valueOf(topicId);
        } catch (NumberFormatException e) {
            id = -1;
        }
        if (id == -1) {
            //异常
            return;
        }
        final ArrayList<TopicItemBean> result = new ArrayList<>();
        for (TopicItemBean topicItemBean : dataInMemory) {
            if (topicItemBean.getTopicId() == id) {
                //内存中找到
                result.add(topicItemBean);
                callback.onFinished(true, "result");
                break;
            }
        }
        //内存中没有需要的数据 进行数据库读取
        if (result.size() == 0) {
            final DbTopic dbTopic = DatabaseManager.getTopicById(id);
            if (dbTopic != null) {
                final TopicItemBean topicItemBean = DatabaseManager.changeDbTopicToTopicItemBean(dbTopic);
                result.add(topicItemBean);
                //添加到内存中
                dataInMemory.add(topicItemBean);
            }
        }
        //数据库中也没有目标 topic 的信息 向服务器获取
        if (result.size() == 0) {
            TopicApiImpl.requestUserTopicList(Constants.imToken, new TopicApiImpl.TopicListCallback<ImTopic_new>() {
                @Override
                public void onGetTopicList(ArrayList<ImTopic_new> topicList) {
                    if (topicList != null && topicList.size() == 1) {
                        //网络获取成功 首先保存到数据库
                        final TopicItemBean bean = DatabaseManager.updateDbTopicWithImTopic(topicList.get(0));
                        //保存到内存缓存
                        result.add(bean);
                        dataInMemory.add(bean);
                    } else {
                        //返回无效数据
                    }
                }
            });
        }
    }

    public interface ActionCallback {
        void onUpdate(ArrayList<TopicItemBean> datalist);
        void onFinished(boolean success, String msg);
    }

}
