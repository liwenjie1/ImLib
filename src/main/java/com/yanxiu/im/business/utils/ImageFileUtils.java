package com.yanxiu.im.business.utils;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.text.TextUtils;


import com.yanxiu.lib.yx_basic_library.util.YXScreenUtil;

import java.io.File;

/**
 * Created by 朱晓龙 on 2018/5/17 11:58.
 */

public class ImageFileUtils {

    public static boolean isImgFileExsist(String fileUrl){
        if (TextUtils.isEmpty(fileUrl)) {
            return false;
        }
        File file=new File(fileUrl);
        return file.exists();
    }
    /**
     * 计算图片的宽高
     *
     * @param imgPath 图片路径
     * @return Integer【】 第一个参数表示width 第二个参数表示height
     */
    public static Integer[] getPicWithAndHeight(String imgPath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, options);
        return new Integer[]{options.outWidth, options.outHeight};
    }

    /**
     * 获取图片应该显示的长和宽
     *
     * @param width  真实宽度
     * @param height 真实长度
     * @return
     */
    public static Integer[] getPicShowWH(Context context, int width, int height) {
        float baseSize = YXScreenUtil.dpToPx(context, 140);
        float iResultWidth = baseSize;
        float iResultHeight = baseSize;

        //水平显示
        if (width > height) {
            float scaleSize = baseSize / width;
            iResultWidth = baseSize;
            iResultHeight = height * scaleSize;
        }
        //垂直显示
        if (width < height) {
            float scaleSize = baseSize / height;
            iResultHeight = baseSize;
            iResultWidth = width * scaleSize;
        }

        if (iResultHeight < baseSize / 2) {
            iResultHeight = baseSize / 2;
        }
        if (iResultWidth < baseSize / 2) {
            iResultWidth = baseSize / 2;
        }


        return new Integer[]{(int) iResultWidth, (int) iResultHeight};
    }


}
