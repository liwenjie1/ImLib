package com.yanxiu.im.business.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

import com.yanxiu.im.R;

/**
 * 滑动删除view
 * create by 戴延枫 2018年8月31日
 */
public class SlidingButtonView extends HorizontalScrollView {

    private View sliding_layout;

    private int mScrollWidth;

    private IonSlidingButtonListener mIonSlidingButtonListener;

    private Boolean isOpen = false;
    private Boolean once = false;
    private boolean protect = false;


    public SlidingButtonView(Context context) {
        this(context, null);
    }

    public SlidingButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.setOverScrollMode(OVER_SCROLL_NEVER);
        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!once) {
//            sliding_layout = findViewById(R.id.sliding_layout);
            once = true;
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            this.scrollTo(0, 0);
            //获取水平滚动条可以滑动的范围，即右侧按钮的宽度
            mScrollWidth = sliding_layout.getWidth();
            Log.i("asd", "mScrollWidth:" + mScrollWidth);
        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (protect) {
            return true;
        }
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (mIonSlidingButtonListener != null)
                    mIonSlidingButtonListener.onMove(this);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                changeScrollx();
                return true;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        sliding_layout.setTranslationX(l - mScrollWidth);
    }

    public void setProtect(boolean p) {
        protect = p;
    }

    /**
     * 按滚动条被拖动距离判断关闭或打开菜单
     */
    public void changeScrollx() {
        if (getScrollX() >= (mScrollWidth / 2)) {
            this.smoothScrollTo(mScrollWidth, 0);
            isOpen = true;
            if (mIonSlidingButtonListener != null)
                mIonSlidingButtonListener.onMenuIsOpen(this);
        } else {
            this.smoothScrollTo(0, 0);
            isOpen = false;
        }
    }

    Handler scrollHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                SlidingButtonView.this.smoothScrollTo(500, 0);
            }
        }
    };

    public void openView() {
        if (isOpen) {
            return;
        }
        Message msg = new Message();
        msg.what = 1;
        scrollHandler.sendMessage(msg);
        isOpen = true;
        if (mIonSlidingButtonListener != null)
        mIonSlidingButtonListener.onMenuIsOpen(this);
    }

    /**
     * 打开菜单
     */
    public void openMenu() {
        if (isOpen) {
            return;
        }
        this.smoothScrollTo(mScrollWidth, 0);
        isOpen = true;
        if (mIonSlidingButtonListener != null)
        mIonSlidingButtonListener.onMenuIsOpen(this);
    }

    /**
     * 关闭菜单
     */
    public void closeMenu() {
        if (!isOpen) {
            return;
        }
        this.smoothScrollTo(0, 0);
        isOpen = false;
    }


    public void setSlidingButtonListener(IonSlidingButtonListener listener) {
        mIonSlidingButtonListener = listener;
    }

    public interface IonSlidingButtonListener {
        void onMenuIsOpen(View view);

        void onMove(SlidingButtonView slidingButtonView);

        void onDown(int position);
    }
}

