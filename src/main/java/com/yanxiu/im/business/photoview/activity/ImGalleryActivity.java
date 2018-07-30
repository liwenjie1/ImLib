package com.yanxiu.im.business.photoview.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.yanxiu.im.R;
import com.yanxiu.im.business.photoview.adapter.ImGalleryViewPagerAdapter;

import java.util.ArrayList;

public class ImGalleryActivity extends AppCompatActivity {
    public static void invoke(Activity activity, ArrayList<String> imgUrls, int currentPosition,int requestCode){
        Intent intent=new Intent(activity,ImGalleryActivity.class);
        intent.putExtra("imgurls",imgUrls);
        intent.putExtra("position",currentPosition);
        activity.startActivityForResult(intent,requestCode);
    }



    private ViewPager viewPager;
    private ArrayList<String> urls;
    private int initPosition=0;
    private ImGalleryViewPagerAdapter pagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.im_gallery_activity);
        viewInit();
        dataInit();
        listenerInit();
    }

    private void viewInit(){
        viewPager=findViewById(R.id.im_gallery_viewpager);
    }
    private void dataInit(){
        urls=getIntent().getStringArrayListExtra("imgurls");
        initPosition=getIntent().getIntExtra("position",0);
        pagerAdapter=new ImGalleryViewPagerAdapter(urls);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(initPosition);
    }

    private void listenerInit(){
        pagerAdapter.setItemOnClickListener(new ImGalleryViewPagerAdapter.ItemOnClickListener() {
            @Override
            public void onClicked() {
                ImGalleryActivity.this.finish();
            }
        });
    }
}
