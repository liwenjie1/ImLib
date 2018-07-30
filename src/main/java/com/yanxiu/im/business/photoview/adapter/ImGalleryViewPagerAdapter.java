package com.yanxiu.im.business.photoview.adapter;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yanxiu.im.R;
import com.yanxiu.im.business.view.ZoomImageView;

import java.util.ArrayList;

/**
 * Created by 朱晓龙 on 2018/5/8 17:11.
 */

public class ImGalleryViewPagerAdapter extends PagerAdapter {
    private ArrayList<String> datalist;


    public ImGalleryViewPagerAdapter(ArrayList<String> datalist) {
        this.datalist = datalist;
    }

    @Override
    public int getCount() {
        return datalist==null?0:datalist.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view==object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ZoomImageView zoomImageView=new ZoomImageView(container.getContext());
        zoomImageView.setBackgroundColor(Color.BLACK);
        ViewGroup.LayoutParams layoutParams=new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        zoomImageView.setLayoutParams(layoutParams);
        // TODO: 2018/5/16  是否有必要加载原图  过大原图导致磁盘缓存加载过慢 采用屏幕尺寸进行缓存 增加原图查看按钮 用户选择加载原图时才加载原图？
        Glide.with(zoomImageView.getContext())
                .load(datalist.get(position))
                .fitCenter()
                .dontTransform()
                .dontAnimate()
                .priority(Priority.HIGH)
                .placeholder(R.drawable.im_pic_holder_view_bg)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .error(R.drawable.im_pic_holder_view_bg)
                .into(zoomImageView);
        container.addView(zoomImageView);
        if (itemOnClickListener != null) {
            zoomImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemOnClickListener.onClicked();
                }
            });
        }
        return zoomImageView;
    }

    /**
     * 优化功能，计算屏幕与原图尺寸，显示符合屏幕大小的resize图片并缓存
     * 降低过大原图造成的加载延迟
     * */
    private void calculateImgShowSize(){

    }


    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    private ItemOnClickListener itemOnClickListener;

    public void setItemOnClickListener(ItemOnClickListener itemOnClickListener) {
        this.itemOnClickListener = itemOnClickListener;
    }

    public interface ItemOnClickListener{
        void onClicked();
    }
}
