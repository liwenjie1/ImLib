package com.yanxiu.im.business.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yanxiu.im.R;


/**
 * Created by 朱晓龙 on 2018/5/7 10:25.
 */

public class ImTitleLayout extends RelativeLayout implements View.OnClickListener {
    private final String TAG=getClass().getSimpleName();
    /**
     * 左右 4个 可点击的控件
     */
    private ImageView title_left_img;
    private ImageView title_right_img;
    private TextView title_left_txt;
    private TextView title_right_txt;
    private TextView title_title_txt;
    private View title_bottom_line;

    /**
     * attr 参数
     */
    private int mLeftImgReferenceId;
    private int mRightImgReferenceId;
    private String mLeftTxtStr;
    private String mRightTxtStr;
    /**
     * 底部分割线
     * */
    private int mBottomLineColor;
    private float mBottomLineHeight=1;

    private int mLeftMergin = 0;
    private int mRightMergin = 0;


    private TitlebarActionClickListener mTitlebarActionClickListener;

    public ImTitleLayout(Context context) {
        this(context, null);
    }

    public ImTitleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImTitleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.im_title_layout, this, true);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ImTitleLayout);
        mLeftImgReferenceId = typedArray.getResourceId(R.styleable.ImTitleLayout_im_title_left_img, -1);
        mRightImgReferenceId = typedArray.getResourceId(R.styleable.ImTitleLayout_im_title_right_img, -1);
        mLeftMergin = typedArray.getDimensionPixelSize(R.styleable.ImTitleLayout_im_left_margin, 0);
        mRightMergin = typedArray.getDimensionPixelSize(R.styleable.ImTitleLayout_im_right_margin, 0);
        mBottomLineColor=typedArray.getColor(R.styleable.ImTitleLayout_im_title_bottom_line_color, Color.LTGRAY);
        mBottomLineHeight=typedArray.getDimensionPixelSize(R.styleable.ImTitleLayout_im_title_bottom_line_height,1);
        typedArray.recycle();
        //实例化 控件
        viewInit();
        //数据初始化
        dateInit();
        listenerInit();
    }







    private void viewInit() {
        Log.i(TAG, "viewInit: ");
        title_left_img = findViewById(R.id.title_left_img);
        title_left_txt = findViewById(R.id.title_left_txt);
        title_right_img = findViewById(R.id.title_right_img);
        title_right_txt = findViewById(R.id.title_right_txt);
        title_title_txt = findViewById(R.id.title_title_txt);
        title_bottom_line=findViewById(R.id.title_bottom_line);

        setRightMergin();
        setLeftMergin();
    }

    private void setLeftMergin() {
        Log.i(TAG, "setLeftMergin: "+mLeftMergin);
        RelativeLayout.LayoutParams leftMerginParams = (RelativeLayout.LayoutParams) title_left_img.getLayoutParams();
        leftMerginParams.leftMargin = mLeftMergin;
        title_left_img.setLayoutParams(leftMerginParams);
        leftMerginParams = (RelativeLayout.LayoutParams) title_left_txt.getLayoutParams();
        leftMerginParams.leftMargin = mLeftMergin;
        title_left_txt.setLayoutParams(leftMerginParams);

    }

    private void setRightMergin() {
        Log.i(TAG, "setRightMergin: "+mRightMergin);
        RelativeLayout.LayoutParams rightMerginParams = (RelativeLayout.LayoutParams) title_right_img.getLayoutParams();
        rightMerginParams.rightMargin = mRightMergin;
        title_right_img.setLayoutParams(rightMerginParams);
        rightMerginParams = (RelativeLayout.LayoutParams) title_right_txt.getLayoutParams();
        rightMerginParams.rightMargin = mRightMergin;
        title_right_txt.setLayoutParams(rightMerginParams);

    }


    public void enableRightBtn(boolean enable){
        title_right_img.setEnabled(enable);
        title_right_txt.setEnabled(enable);
    }

    /**
     * 初始化显示  xml中设置的数据
     */
    private void dateInit() {
        Log.i(TAG, "dateInit: ");
        //两边imageview 的判定 如果没设置资源 隐藏
        setLeftImageRes(mLeftImgReferenceId);
        setRightImageRes(mRightImgReferenceId);
        //两边 textview
//        setTitleLeftText(mLeftTxtStr);
//        setTitleRightText(mRightTxtStr);
//        bottomLineInit();
    }
    private void bottomLineInit(){
        Log.i(TAG, "bottomLineInit: ");
        //bottomline
        if (mBottomLineHeight>0) {
            title_bottom_line.setBackgroundColor(mBottomLineColor);
            ViewGroup.LayoutParams params=title_bottom_line.getLayoutParams();
            params.height= (int) mBottomLineHeight;
            title_bottom_line.setLayoutParams(params);
            title_bottom_line.setVisibility(VISIBLE);
        }else {
            title_bottom_line.setVisibility(GONE);
        }

    }


    private void listenerInit() {
        title_left_img.setOnClickListener(this);
        title_left_txt.setOnClickListener(this);
        title_right_txt.setOnClickListener(this);
        title_right_img.setOnClickListener(this);
    }

    /**
     * 设置title
     *
     * @param title title 字符串
     */
    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            title_title_txt.setVisibility(VISIBLE);
            title_title_txt.setText(title);
        }else {
            title_title_txt.setVisibility(GONE);
        }
    }

    public void setTitleLeftText(String text) {
        if (!TextUtils.isEmpty(text)) {
            title_left_txt.setText(text);
            title_left_txt.setVisibility(VISIBLE);
            title_left_img.setVisibility(GONE);
        }else {
            title_left_txt.setVisibility(GONE);
        }
    }
    public void setLeftImageRes(int resId) {
        if (resId > 0) {
            title_left_img.setImageResource(resId);
            title_left_img.setVisibility(VISIBLE);
            title_left_txt.setVisibility(GONE);
        }else {
            title_left_img.setVisibility(GONE);
        }

    }
    public void setTitleRightText(String text) {
        if (!TextUtils.isEmpty(text)) {
            title_right_txt.setText(text);
            title_right_txt.setVisibility(VISIBLE);
        }else {
            title_right_img.setVisibility(GONE);
        }
    }



    public void setRightImageRes(int resId) {
        if (resId > 0) {
            title_right_img.setImageResource(resId);
            title_right_img.setVisibility(VISIBLE);
            title_right_txt.setVisibility(GONE);
        } else {
            title_right_txt.setVisibility(GONE);
        }
    }


    public void setmTitlebarActionClickListener(TitlebarActionClickListener mTitlebarActionClickListener) {
        this.mTitlebarActionClickListener = mTitlebarActionClickListener;
    }

    /**
     * title 点击监听
     */
    @Override
    public void onClick(View v) {
        if (mTitlebarActionClickListener == null) {
            return;
        }
        int i = v.getId();
        if (i == R.id.title_left_img || i == R.id.title_left_txt) {
            mTitlebarActionClickListener.onLeftComponentClicked();

        } else if (i == R.id.title_right_img || i == R.id.title_right_txt) {
            mTitlebarActionClickListener.onRightComponpentClicked();

        } else {
        }
    }

    /**
     * titlelayout 左右两侧Component 的点击监听
     */
    public interface TitlebarActionClickListener {
        void onLeftComponentClicked();

        void onRightComponpentClicked();
    }

}
