package com.yanxiu.im.business.msglist.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.test.yanxiu.common_base.imagePicker.GlideImageLoader;
import com.test.yanxiu.common_base.ui.ImBaseActivity;
import com.test.yanxiu.common_base.ui.KeyboardChangeListener;
import com.test.yanxiu.common_base.utils.SrtLogger;
import com.test.yanxiu.common_base.utils.permission.OnPermissionCallback;
import com.test.yanxiu.common_base.utils.talkingdata.EventUpdate;
import com.yanxiu.im.Constants;
import com.yanxiu.im.R;
import com.yanxiu.im.bean.MsgItemBean;
import com.yanxiu.im.bean.TopicItemBean;
import com.yanxiu.im.business.msglist.ImMsgListFoucsLinearLayoutManager;
import com.yanxiu.im.business.msglist.adapter.ImMsgListDecorateAdapter;
import com.yanxiu.im.business.msglist.interfaces.MsgListContract;
import com.yanxiu.im.business.msglist.interfaces.impls.MsgListPresenter;
import com.yanxiu.im.business.photoview.activity.ImGalleryActivity;
import com.yanxiu.im.business.view.ChoosePicsDialog;
import com.yanxiu.im.business.view.ImTitleLayout;
import com.yanxiu.im.db.DbMember;
import com.yanxiu.im.event.MqttConnectedEvent;
import com.yanxiu.im.event.MsgListMigrateMockTopicEvent;
import com.yanxiu.im.event.MsgListNewMsgEvent;
import com.yanxiu.im.event.MsgListTopicChangeEvent;
import com.yanxiu.im.event.MsgListTopicRemovedEvent;
import com.yanxiu.im.event.MsgListTopicUpdateEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class ImMsgListActivity extends ImBaseActivity implements MsgListContract.IView<MsgItemBean>
        , ImTitleLayout.TitlebarActionClickListener, ImMsgListDecorateAdapter.MsgItemViewClickListener {
    private final String TAG = getClass().getSimpleName();


    /*MsgListActivity 三种不同的开启方式*/
    public final static int REQUEST_CODE_PUSH = 0X11;
    public final static int REQUEST_CODE_MEMBERID = 0X12;
    public final static int REQUEST_CODE_TOPICID = 0X13;

    /**
     * 由 app 通讯录 或 IM 通讯录 点击用户头像进入 对话页面
     * {@link com.yanxiu.im.business.contacts.activity.ContactsActivity}
     * 可能存在点击的 member 在数据库中不存在 所以传入名字 用于创建临时对话名称显示 以及 mocktopic 所需要的 dbmember 信息
     */
    public static void invoke(Activity activity, long memberId, String memberName, long fromTopicId, int requestCode) {
        Intent intent = new Intent(activity, ImMsgListActivity.class);
        intent.putExtra("memberId", memberId);
        intent.putExtra("memberName", memberName);
        intent.putExtra("fromTopicId", fromTopicId);
        intent.putExtra("requestCode", requestCode);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 这个方法一般为 由通讯录、群聊进入私聊界面
     * <p>
     * 传入点击的memberId
     * 查找对应的用户，并获取对应的私聊topic 如果没有私聊 需要创建本地临时topic 并由底层进行http创建后进行
     * 内容更新
     *
     * @param activity
     * @param memberId
     * @param requestCode
     */
    public static void invoke(Activity activity, long memberId, long fromTopicId, String groupName, int requestCode) {
        Intent intent = new Intent(activity, ImMsgListActivity.class);
        intent.putExtra("memberId", memberId);
        intent.putExtra("fromTopicId", fromTopicId);
        intent.putExtra("groupName", groupName);
        intent.putExtra("requestCode", requestCode);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void invoke(Activity activity, long memberId, String memberName, long fromTopicId, String groupName, int requestCode) {
        Intent intent = new Intent(activity, ImMsgListActivity.class);
        intent.putExtra("memberId", memberId);
        intent.putExtra("memberName", memberName);
        intent.putExtra("fromTopicId", fromTopicId);
        intent.putExtra("groupName", groupName);
        intent.putExtra("requestCode", requestCode);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 这个方法为 在topic列表界面进入聊天界面 目标一定是已经存在的topic所以直接传topicid 进行查找
     *
     * @param fragment
     * @param topicId
     * @param requestCode
     */
    public static void invoke(Fragment fragment, long topicId, int requestCode) {
        Intent intent = new Intent(fragment.getContext(), ImMsgListActivity.class);
        intent.putExtra("topicId", topicId + "");
        intent.putExtra("requestCode", requestCode);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 由推送进入 聊天界面
     */
    public static void invokeByPush(Context context, long topicId, int requestCode) {
        Intent intent = new Intent(context, ImMsgListActivity.class);
        intent.putExtra("topicId", topicId + "");
        intent.putExtra("requestCode", requestCode);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    private RecyclerView im_msglist_recyclerview;
    private ImMsgListDecorateAdapter<MsgItemBean> msgRecyclerAdapter;
    private ImageView cameraView;
    private ImTitleLayout imTitleLayout;
    private MsgListPresenter msgListPresenter;
    private ImagePicker imagePicker;
    private EditText mMsgEditText;
    private TopicItemBean currentTopic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.im_msglist_activity);
        initView();
        initData();
        initListener();
        initImagePicker();
        EventBus.getDefault().register(this);

    }


    private void showSlientNotice(boolean show) {
        findViewById(R.id.im_msglist_topic_silence).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //页面统计
        if (currentTopic == null || TextUtils.equals(currentTopic.getType(), "1")) {
            EventUpdate.onPrivatePageStart(this);
        } else {
            EventUpdate.onGroupPageStart(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //页面统计
        if (currentTopic == null || TextUtils.equals(currentTopic.getType(), "1")) {
            EventUpdate.onPrivatePageEnd(this);
        } else {
            EventUpdate.onGroupPageEnd(this);
        }
    }

    private void initView() {
        im_msglist_recyclerview = findViewById(R.id.im_msglist_recyclerview);
        ImMsgListFoucsLinearLayoutManager layoutManager =
                new ImMsgListFoucsLinearLayoutManager(ImMsgListActivity.this, LinearLayoutManager.VERTICAL, true);
        layoutManager.setStackFromEnd(true);
        im_msglist_recyclerview.setLayoutManager(layoutManager);
        im_msglist_recyclerview.setItemAnimator(null);
        mMsgEditText = findViewById(R.id.msg_edittext);
        cameraView = findViewById(R.id.takepic_imageview);
        imTitleLayout = findViewById(R.id.im_title_layout);
    }

    private void initData() {
        msgListPresenter = new MsgListPresenter(this, ImMsgListActivity.this);
        //获取 需要展示的topic 如果没有 则currentTopic 为空
        getTargetTopic();
        // 获取当前topic 的最新一页数据 可能为空
        List<MsgItemBean> datalist = (currentTopic == null) ? null : currentTopic.getMsgList();
        msgRecyclerAdapter = new ImMsgListDecorateAdapter(ImMsgListActivity.this, (ArrayList) datalist);

        im_msglist_recyclerview.setAdapter(msgRecyclerAdapter);
        im_msglist_recyclerview.scrollToPosition(0);
        //更新member信息
        msgListPresenter.updateTopicInfo(currentTopic);
        if (Constants.showTopicSetting) {
            imTitleLayout.setTitleRightText("设置");
        }
    }


    /**
     * 根据intent携带的参数 获取相应的topic
     * <p>
     * 由通讯录 或 群聊点击用户头像进入此页时 intent包含memberId信息 topic不一定存在(CurrentTopic 可能为空)
     * <p>
     * <p>
     * 由topic列表进入时 intent包含topicId信息 并且topic一定存在 （包括mocktopic情况）
     */
    private void getTargetTopic() {
        final int requestCode = getIntent().getIntExtra("requestCode", REQUEST_CODE_TOPICID);
        switch (requestCode) {
            case REQUEST_CODE_MEMBERID: {
                //通过点击 member 头像 开启的 msglist 界面 需要判断是否存在私聊 不存在需要创建一个临时的 topicbean
                long memberId = getIntent().getLongExtra("memberId", -1);
                long fromTopicId = getIntent().getLongExtra("fromTopicId", -1);
                String memberName = getIntent().getStringExtra("memberName");
                msgListPresenter.openPrivateTopicByMember(memberId, memberName, fromTopicId);
            }
            break;
            case REQUEST_CODE_PUSH: {
                //通过推送 开启的 msglist 界面 可能是群聊也可能是私聊  但是在服务器上一定存在 所以本地先查找 ，没有直接在服务器上获取
                //如果 本地没有 在 异步执行 服务器获取的之间，创建本地 tempTopicBean 来容纳消息列表
                String topicId = getIntent().getStringExtra("topicId");
                msgListPresenter.openPushTopic(Long.valueOf(topicId));
            }
            break;
            case REQUEST_CODE_TOPICID: {
                //通过 topiclist 界面点击 topic 开启 msglist 界面 topic 一定存在 只需要在内存查找 获取
                String topicId = getIntent().getStringExtra("topicId");
                msgListPresenter.openTopicByTopicId(Long.valueOf(topicId));
            }
            break;
        }
    }


    private void initImagePicker() {
        GlideImageLoader glideImageLoader = new GlideImageLoader();
        imagePicker = ImagePicker.getInstance();
        imagePicker.setImageLoader(glideImageLoader);
        //显示拍照按钮
        imagePicker.setShowCamera(true);
        //允许裁剪（单选才有效）
        imagePicker.setCrop(false);
        //选中数量限制
        imagePicker.setSelectLimit(9);
        //裁剪框的形状
    }


    /**
     * 手势监听  保证只有在 下拉recyclerview时才检测是否执行 loadmore
     */
    private GestureDetector gestureDetector;
    private boolean scrollDown = false;
    private GestureDetector.SimpleOnGestureListener gestruelistener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            scrollDown = (distanceY < 0);
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    };

    private void initListener() {
        //下拉加载更多
        initRecyclerLoadmoreListener();
        //msgList 消息点击监听
        msgRecyclerAdapter.setMsgItemViewClickListener(this);
        //titlelayout 点击监听
        imTitleLayout.setmTitlebarActionClickListener(this);

        cameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //事件统计 点击相机按钮
                EventUpdate.onClickMsgCameraEvent(ImMsgListActivity.this);
                showChoosePicsDialog();
            }
        });
        //键盘操作
        mMsgEditText.setImeOptions(EditorInfo.IME_ACTION_NONE);
        mMsgEditText.setRawInputType(InputType.TYPE_CLASS_TEXT);


        // 弹出键盘后处理
        KeyboardChangeListener keyboardListener = new KeyboardChangeListener(this);
        keyboardListener.setKeyBoardListener(new KeyboardChangeListener.KeyBoardListener() {
            @Override
            public void onKeyboardChange(boolean isShow, int keyboardHeight) {
                if ((isSoftShowing())) {
                    im_msglist_recyclerview.scrollToPosition(0);
                }
            }
        });
        //新增的 发送按钮 发送逻辑与 按键发送一样
        final TextView sendTv = findViewById(R.id.tv_sure);
        sendTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //动作统计  发送
                EventUpdate.onClickMsgSendEvent(ImMsgListActivity.this);

                SrtLogger.log("imui", "TBD: 发送");
                String msg = mMsgEditText.getText().toString();
                mMsgEditText.setText("");
                String trimMsg = msg.trim();
                if (trimMsg.length() == 0) {
                    return;
                }
                // 由于存在currentTopic为空的情况 需要先检查  如果currentTopic=null 创建mocktopic
                checkTopicExsist();
                //current ！=null currentTopic.msglist不为空 adapter 的datalist = null
                msgListPresenter.doSendTextMsg(trimMsg, currentTopic);

            }
        });
        //添加监听 当有文字输入时 展示发送按钮可点击
        mMsgEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
                if (charSequence != null) {
                    if (charSequence.length() > 0) {
                        //设置 enable 可点击状态
                        sendTv.setEnabled(true);
                        sendTv.setBackgroundResource(R.drawable.im_sendbtn_default);
                        return;
                    }

                }
                //设置颜色为 disable 与 按下颜色一样
                sendTv.setEnabled(false);
                sendTv.setBackgroundResource(R.drawable.im_sendbtn_pressed);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable == null) {
                    return;
                }

                if (editable.length() > 2000) {
                    editable.delete(2000, editable.length());
                }

            }
        });
        //初始设置为 不可点击 当用户有输入 才进行使能设置
        sendTv.setEnabled(false);
        sendTv.setBackgroundResource(R.drawable.im_sendbtn_pressed);
    }

    /**
     * 检查当前topic 是否能完成发送消息的操作
     * currentTopic = null 时 需要创建mocktopic
     */
    private void checkTopicExsist() {
        if (currentTopic == null) {
            long memberId = getIntent().getLongExtra("memberId", -1);
            long fromTopicId = getIntent().getLongExtra("fromTopicId", -1);
            String memberName = getIntent().getStringExtra("memberName");
            currentTopic = msgListPresenter.createMockTopicForMsg(memberId, fromTopicId, memberName);
            msgRecyclerAdapter.setDataList(currentTopic.getMsgList());
        }
    }

    @Override
    public void onMockTopicCreated(TopicItemBean mockTopic) {
        //mocktopic 创建成功后
        currentTopic = mockTopic;

    }

    /**
     * 形成loadmore功能
     * 手势监听负责判断手势方向
     * scrolllistener 负责判断recyclerview滚动状态以及是否显示footview 是否调用loadmore
     */
    private void initRecyclerLoadmoreListener() {
        gestureDetector = new GestureDetector(ImMsgListActivity.this, gestruelistener);
        im_msglist_recyclerview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //用户滑动recyclerView时 收起键盘
//                mMsgEditText.clearFocus();
//                hideSoftInput(mMsgEditText);
                return gestureDetector.onTouchEvent(event);
            }
        });
        im_msglist_recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //记录 recyclerview 的状态 主要是 用户是否还在拖拽 方向判断？
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && scrollDown) {
                    msgRecyclerAdapter.addFooterView();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    //非滚动状态 判断 loading 是否显示
                    int position = ((LinearLayoutManager) im_msglist_recyclerview.getLayoutManager()).findLastVisibleItemPosition();
                    if (position > -1) {
                        if (msgRecyclerAdapter.getItemViewType(position) == ImMsgListDecorateAdapter.ITEM_TYPE_FOOTER) {
                            msgListPresenter.doLoadMore(currentTopic);
                        }
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {

                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }

        });
    }

    /**
     * 收起软键盘
     */
    private void hideSoftInput(EditText editText) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }


    //region 图片选取与预览
    /**
     * 选取照片或拍照
     */
    private ChoosePicsDialog mClassCircleDialog;

    private void showChoosePicsDialog() {
        if (mClassCircleDialog == null) {
            mClassCircleDialog = new ChoosePicsDialog(ImMsgListActivity.this);
            mClassCircleDialog.setClickListener(new ChoosePicsDialog.OnViewClickListener() {
                @Override
                public void onAlbumClick() {
                    ImMsgListActivity.requestWriteAndReadPermission(new OnPermissionCallback() {
                        @Override
                        public void onPermissionsGranted(@Nullable List<String> deniedPermissions) {
                            Intent intent = new Intent(ImMsgListActivity.this, ImageGridActivity.class);
                            startActivityForResult(intent, IMAGE_PICKER);
                        }

                        @Override
                        public void onPermissionsDenied(@Nullable List<String> deniedPermissions) {
                            Toast.makeText(ImMsgListActivity.this, R.string.no_storage_permissions, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onCameraClick() {
                    ImMsgListActivity.requestCameraPermission(new OnPermissionCallback() {
                        @Override
                        public void onPermissionsGranted(@Nullable List<String> deniedPermissions) {

                            Intent intent = new Intent(ImMsgListActivity.this, ImageGridActivity.class);
                            // 是否是直接打开相机
                            intent.putExtra(ImageGridActivity.EXTRAS_TAKE_PICKERS, true);
                            startActivityForResult(intent, REQUEST_CODE_SELECT);
                        }

                        @Override
                        public void onPermissionsDenied(@Nullable List<String> deniedPermissions) {
                            Toast.makeText(ImMsgListActivity.this, R.string.no_storage_permissions, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
        mClassCircleDialog.show();
    }

    private static final int IMAGE_PICKER = 0x03;
    private static final int REQUEST_CODE_SELECT = 0x04;
    private static final int REQUEST_CODE_LOAD_BIG_IMG = 0x05;
    private boolean shouldScrollToBottom = true;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult: ");
        switch (requestCode) {
            case REQUEST_CODE_MEMBERID:
                msgRecyclerAdapter.notifyDataSetChanged();
            case REQUEST_CODE_LOAD_BIG_IMG:
                shouldScrollToBottom = true;
                break;
            case IMAGE_PICKER:
            case REQUEST_CODE_SELECT:
                ArrayList<ImageItem> imageItems = createSelectedImagesList(data);
                if (imageItems == null || imageItems.size() == 0) {
                    //用户没选图片 直接返回
                    return;
                }
                for (ImageItem imageItem : imageItems) {
                    // 检查是否有topic 没有 创建mocktopic
                    checkTopicExsist();
                    //模拟发送 相同数量的图片
                    msgListPresenter.doSendImgMsg(imageItem.path, currentTopic);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 构造需要的图片数据
     *
     * @param data
     */
    private ArrayList<ImageItem> createSelectedImagesList(Intent data) {
        ArrayList<ImageItem> images = null;
        try {
            images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
        } catch (Exception e) {
        }
        if (images == null) {
            return null;
        }
        return images;
    }


    //endregion

    //region EventBus

    /**
     * 收到TopicList发送的新消息收取通知
     * 检查是否是当前所在topic收到新消息
     * 如果是当前topic 更新msglist列表
     */
    @Subscribe
    public void receiveNewMsg(MsgListNewMsgEvent event) {
        //判断是否是当前正在显示的topic
        if (currentTopic != null && event.topicId != currentTopic.getTopicId()) {
            return;
        }
        //处理msgitem的显示
        msgListPresenter.resetTopicRedDot(event.topicId, currentTopic);
        if (!shouldScrollToBottom) {
            return;
        }
        //滚动到最新一条消息
        msgRecyclerAdapter.notifyItemInserted(0);
        msgRecyclerAdapter.notifyItemRangeChanged(0, msgRecyclerAdapter.getDataList().size());
        im_msglist_recyclerview.scrollToPosition(0);
    }

    @Subscribe
    public void topicChanged(MsgListTopicChangeEvent event) {
        Log.i(TAG, "topicChanged: ");
        //判断是否是当前正在显示的topic
        if (currentTopic != null && event.topicId != currentTopic.getTopicId()) {
            return;
        }
    }

    @Subscribe
    public void topicUpdated(MsgListTopicUpdateEvent event) {
        Log.i(TAG, "topicUpdated: ");
        if (currentTopic == null) {
            long memberId = getIntent().getLongExtra("memberId", -1);
            if (msgListPresenter.checkNullTopicCanbeMerged(event.topicId, memberId)) {
                Log.i(TAG, "topic opened : ");
                onRealTopicOpened(msgListPresenter.getTargetTopic(event.topicId));
                msgListPresenter.resetTopicRedDot(currentTopic.getTopicId(), currentTopic);
                msgRecyclerAdapter.setDataList(currentTopic.getMsgList());
                msgRecyclerAdapter.notifyDataSetChanged();
            }
            return;
        }
        if (currentTopic.getTopicId() != event.topicId) {
            return;
        }
        msgListPresenter.resetTopicRedDot(event.topicId, currentTopic);

        msgRecyclerAdapter.notifyDataSetChanged();
        im_msglist_recyclerview.scrollToPosition(0);
    }

    @Subscribe
    public void topicRemoved(MsgListTopicRemovedEvent event) {
        Log.i(TAG, "topicRemoved: ");
        if (currentTopic != null && event.topicId != currentTopic.getTopicId()) {
            return;
        }
        //关闭当前界面 学员端
        if (Constants.APP_TYPE == Constants.APP_TYPE_STUDENT) {
            finish();
        }
        //管理端 不处理
    }

    /**
     * 收到mocktopic 被更新为 realtopic的通知
     */
    @Subscribe
    public void migrateMockTopic(MsgListMigrateMockTopicEvent event) {
        //检查 当前topic情况
        if (currentTopic != null && event.topicId != currentTopic.getTopicId()) {
            return;
        }
        onRealTopicOpened(currentTopic);
        msgListPresenter.resetTopicRedDot(event.topicId, currentTopic);
    }

    @Subscribe
    public void onMqttConnected(MqttConnectedEvent event) {
        //mqtt 服务器连接通知 通知后 刷新数据列表
        msgListPresenter.updateTopicInfo(currentTopic);
        Toast.makeText(this, "mqtt 连接成功", Toast.LENGTH_SHORT).show();
    }


    //endregion


    //region MVP 回调

    /**
     * 收到新消息
     */
    @Override
    public void onNewMsg() {
        //收到消息 presenter 已经对数据集进行了处理
        msgRecyclerAdapter.notifyItemInserted(0);
        msgRecyclerAdapter.notifyItemRangeChanged(0, msgRecyclerAdapter.getDataList().size());
        im_msglist_recyclerview.scrollToPosition(0);
    }

    /**
     * 当前msglist中含有失败造成的断档
     * 内部执行了merge操作 这时需要刷新的数据位置与直接加载不同
     */
    @Override
    public void onLoadMoreWithMerge(int start, int end) {
        Log.i(TAG, "onLoadMoreWithMerge: ");
        msgRecyclerAdapter.removeFooterView();

        msgRecyclerAdapter.notifyItemRangeInserted(start, end);
        msgRecyclerAdapter.notifyItemRangeChanged(start, end);

    }

    @Override
    public void onTopicInfoUpdate() {
        //更新名字
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showSlientNotice(currentTopic.isSilence());
                setTitlemsg(currentTopic);
                msgRecyclerAdapter.notifyDataSetChanged();
            }
        });

    }

    /**
     * 当网络加载失败并完全由数据库获取数据进行队尾拼接时调用
     */
    @Override
    public void onLoadMoreFromDb(int size) {
        Log.i(TAG, "onLoadMoreFromDb: ");
        //队尾增加了 size 条数据
        //清除 loading item
        msgRecyclerAdapter.removeFooterView();
        if (size == 0) {
            Toast.makeText(this, "没有更多数据", Toast.LENGTH_SHORT).show();
            return;
        }
        int startPosition = Math.max(msgRecyclerAdapter.getItemCount() - size - 1, 0);
        int endPosition = Math.max(msgRecyclerAdapter.getItemCount() - 1, 0);
        msgRecyclerAdapter.notifyItemRangeInserted(startPosition, endPosition);
        msgRecyclerAdapter.notifyItemRangeChanged(startPosition, endPosition);

    }

    /**
     * 当网络成功获取数据进行队尾拼接时调用
     */
    @Override
    public void onLoadMoreFromHttp(int size) {
        Log.i(TAG, "onLoadMoreFromHttp: ");
        //队尾增加了 size 条数据
        //清除 loading item
        msgRecyclerAdapter.removeFooterView();
        if (size == 0) {
            Toast.makeText(this, "没有更多数据", Toast.LENGTH_SHORT).show();
            return;
        }
        int startPosition = Math.max(msgRecyclerAdapter.getItemCount() - size - 1, 0);
        int endPosition = Math.max(msgRecyclerAdapter.getItemCount() - 1, 0);
        msgRecyclerAdapter.notifyItemRangeInserted(startPosition, endPosition);
        msgRecyclerAdapter.notifyItemRangeChanged(startPosition, endPosition);
    }

    /**
     * 重发消息完成后的回调
     * 更新 被重发消息的显示情况
     *
     * @param oldPosition 被点击重发的消息所在的位置 获取位置后 更新位置变化涉及到的所有数据的更新
     */
    @Override
    public void onResendMsg(int oldPosition) {
        if (oldPosition == 0) {
            //重发最新一条 没有位置变动
            msgRecyclerAdapter.notifyItemChanged(0);
        } else {
            msgRecyclerAdapter.notifyItemMoved(oldPosition, 0);
            msgRecyclerAdapter.notifyItemRangeChanged(0, oldPosition + 1);
        }
        im_msglist_recyclerview.scrollToPosition(0);
    }


    /**
     * 开启一个 临时的私聊界面
     */
    @Override
    public void onNewPrivateTopicOpened(final String memberName) {
        //设置 对话名称
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imTitleLayout.setTitle(memberName);
            }
        });

    }

    @Override
    public void onRealTopicOpened(TopicItemBean realBean) {
        //打开一个 本地存在的 realtopic
        currentTopic = realBean;
        setTitlemsg(realBean);
    }

    @Override
    public void onPushTopicOpend(final TopicItemBean tempBean) {
        //打开一个 push 开启的topic
        currentTopic = tempBean;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitlemsg(tempBean);
            }
        });

    }

    private void setTitlemsg(TopicItemBean topic) {
        StringBuilder titleStringBuilder = new StringBuilder();
        if (TextUtils.equals("1", topic.getType())) {
            //私聊
            if (topic.getMembers() != null) {
                for (DbMember memberNew : topic.getMembers()) {
                    if (memberNew.getImId() != Constants.imId) {
                        titleStringBuilder.append(memberNew.getName());
                        break;
                    }
                }

            }
        } else {
            //群聊 班级名称
            titleStringBuilder.append(topic.getGroup());
            if (topic.getMembers() != null) {
                //群聊人数
                titleStringBuilder.append("(" + topic.getMembers().size() + ")");
            }

        }


        //需要对转义字符处理
        imTitleLayout.setTitle(titleStringBuilder.toString());
    }


    @Override
    public void onCreateTopicFail() {
        msgRecyclerAdapter.notifyDataSetChanged();
    }


    //endregion

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        im_msglist_recyclerview.setAdapter(null);
        super.onDestroy();
    }


    //region recyclerview 点击监听


    @Override
    public void onBackPressed() {
        onLeftComponentClicked();
    }

    /**
     * 页面标题栏左侧控件被点击
     */
    @Override
    public void onLeftComponentClicked() {
        //点击返回按钮
        im_msglist_recyclerview.stopScroll();
        msgListPresenter.doCuroffMsgList(currentTopic);
        finish();
    }

    /**
     * 页面标题栏右侧控件被点击
     */
    @Override
    public void onRightComponpentClicked() {
        // TODO mockTopic 与 空 topic 的处理
        if (currentTopic != null) {
            //跳转到设置界面
            ImSettingActivity.invoke(ImMsgListActivity.this, currentTopic.getTopicId());
        }
    }

    /**
     * recyclerview 信息内容被点击
     *
     * @param p           点击的数据index
     * @param contentType 内容类型 10-文字 20-图片
     */
    @Override
    public void onContentClicked(int p, int contentType) {
        if (contentType == 20) {
            //图片类型消息
            MsgItemBean currentBean = msgRecyclerAdapter.getDataList().get(p);
            //获取目前所有图片消息
            ArrayList<MsgItemBean> imgItemBeans =
                    (ArrayList<MsgItemBean>) msgListPresenter.getAllImageMsgs((ArrayList<MsgItemBean>) msgRecyclerAdapter.getDataList());
            //获取urls
            ArrayList<String> urls = (ArrayList<String>) msgListPresenter.getAllImageUrls(imgItemBeans);
            //获取当前msg的对应位置
            int currentPosition = imgItemBeans.indexOf(currentBean);
            shouldScrollToBottom = false;
            ImGalleryActivity.invoke(ImMsgListActivity.this, urls, currentPosition, REQUEST_CODE_LOAD_BIG_IMG);
        } else if (contentType == 10) {
            //文字类型消息
        } else {
            //其他类型消息 如语音   需要一个全局的独立播放器来进行音频播放
        }
    }

    /**
     * recyclerview 消息发送者头像被点击
     *
     * @param p 点击数据在数据集的index
     */
    @Override
    public void onIconClicked(int p) {
        //检查 点击的用户是否可以打开私聊
        MsgItemBean msgItemBean = msgRecyclerAdapter.getDataList().get(p);
        //检查自己是否被删除
        if (!msgListPresenter.checkUserExsist(Constants.imId, currentTopic)) {
            Toast.makeText(ImMsgListActivity.this, "【您已被移除此班级】", Toast.LENGTH_SHORT).show();
            return;
        }
        //检查用户存在性
        if (msgListPresenter.checkUserExsist(msgItemBean.getSenderId(), currentTopic)) {
            if (msgListPresenter.doCheckMemberChat(msgItemBean.getSenderId(), currentTopic)) {
                //事件统计 群聊点击 用户头像
                EventUpdate.onClickGroupAvatarEvent(ImMsgListActivity.this);
                shouldScrollToBottom = false;
                ImMsgListActivity.invoke(ImMsgListActivity.this,
                        msgItemBean.getSenderId(), msgItemBean.getMember().getName(), currentTopic.getTopicId(), REQUEST_CODE_MEMBERID);
            }
        } else {
            //用户不存在 给出提示
            Toast.makeText(ImMsgListActivity.this, "【用户已被删除】", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * recyclerview 消息发送失败 标志被点击
     * presenter 对 msgitemlist进行处理后 回调{@link ImMsgListActivity#onResendMsg(int)} 通知UI 更新
     *
     * @param p 消息所在数据集位置
     */
    @Override
    public void onFailFlagClicked(int p) {
        msgListPresenter.doResendMsg(p, currentTopic);
    }

//endregion

    /**
     * 底部虚拟按键栏的高度
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int getSoftButtonsBarHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        //这个方法获取可能不是真实屏幕的高度
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        //获取当前屏幕的真实高度
        this.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }
    }


    private boolean isSoftShowing() {
        //获取当前屏幕内容的高度
        int screenHeight = getWindow().getDecorView().getHeight();
        //获取View可见区域的bottom
        Rect rect = new Rect();
        //DecorView即为activity的顶级view
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        //考虑到虚拟导航栏的情况（虚拟导航栏情况下：screenHeight = rect.bottom + 虚拟导航栏高度）
        //选取screenHeight*2/3进行判断
//        return screenHeight*2/3 > rect.bottom;
        return screenHeight - getSoftButtonsBarHeight() > rect.bottom;
    }
}
