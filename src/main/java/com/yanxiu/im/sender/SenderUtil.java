package com.yanxiu.im.sender;

import android.graphics.BitmapFactory;
import android.os.Environment;

import com.qiniu.android.storage.Configuration;

import java.io.File;

/**
 * Im发送工具类
 * Created by 杨小明 on 2018/5/7.
 */

public class SenderUtil {
    private static final String COMPRESS_PATH = "/faceShow/";

    //七牛上传配置
    public static Configuration config = new Configuration.Builder()
//            // 分片上传时，每片的大小。 默认256K
            .chunkSize(1 * 1024)
//            // 启用分片上传阀值。默认512K
            .putThreshhold(2 * 1024)
//            .chunkSize(256)
            // 启用分片上传阀值。默认512K
//            .putThreshhold(512)
            // 链接超时。默认10秒
            .connectTimeout(10)
            // 服务器响应超时。默认60秒
            .responseTimeout(60)
            .build();

    /**
     * 获取压缩图片路径
     *
     * @return
     */
    public static String getCompressPath() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + COMPRESS_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 计算图片的宽高
     *
     * @param imgPath 图片路径
     * @return Integer【】 第一个参数表示width 第二个参数表示height
     */
    public static Integer[] getImgWithAndHeight(String imgPath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, options);
        return new Integer[]{options.outWidth, options.outHeight};
    }
}
