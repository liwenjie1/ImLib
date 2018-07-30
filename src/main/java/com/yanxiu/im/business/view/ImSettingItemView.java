package com.yanxiu.im.business.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.yanxiu.im.R;

/**
 * Created by 朱晓龙 on 2018/6/5 12:09.
 * im设置页面 的单条设置选项 封装成类 方便控制
 */

public class ImSettingItemView extends FrameLayout {
    public ImSettingItemView(Context context) {
        this(context,null);
    }

    public ImSettingItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ImSettingItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
        initData();
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ImSettingItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context,attrs);
        initData();
    }



    private String settingItemTitle;
    private String settingItemSummery;
    private ImSwitchButton mImSwitchButton;

    private TextView itemTitleTv;
    private TextView itemSummeryTv;

    private void initView(Context context,AttributeSet attrs){
        LayoutInflater layoutInflater=LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.im_seting_switchbtn_layout,this);
        TypedArray typedArray =context.obtainStyledAttributes(attrs, R.styleable.ImSettingItemView);
        settingItemTitle=typedArray.getString(R.styleable.ImSettingItemView_setting_name);
        settingItemSummery=typedArray.getString(R.styleable.ImSettingItemView_setting_summery);
        typedArray.recycle();

        itemTitleTv=findViewById(R.id.im_setting_title_tv);
        itemSummeryTv=findViewById(R.id.im_setting_summery_tv);
        mImSwitchButton=findViewById(R.id.im_setting_switchbtn);
    }


    private void initData(){
        itemTitleTv.setText(String.valueOf(settingItemTitle));
        itemSummeryTv.setText(String.valueOf(settingItemSummery));
        mImSwitchButton.setChecked(false);
    }

    private void initListener(){
        //switchbuttn listener
    }

    public void setTitle(String titleStr){
        itemTitleTv.setText(String.valueOf(titleStr));
    }

    public void setSummery(String summeryStr){
        itemSummeryTv.setText(String.valueOf(summeryStr));
    }

    public void setSwitchBtnChecked(boolean checked){
        mImSwitchButton.setChecked(checked);
    }

    public void setOnSwitchCheckedChangedListener(ImSwitchButton.OnCheckedChangeListener listener){
        mImSwitchButton.setOnCheckedChangeListener(listener);
    }


}
