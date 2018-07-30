package com.yanxiu.im.business.msglist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

/**
 * Created by 朱晓龙 on 2018/5/11 15:44.
 * from
 * https://blog.csdn.net/yulyu/article/details/55261351
 */

public class CornersTransform extends BitmapTransformation {

    private float radius;

    public CornersTransform(Context context) {
        super(context);
        radius = 15;
    }

    public CornersTransform(Context context, float radius) {
        super(context);
        this.radius = radius;
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        return cornersCrop(pool, toTransform);
    }

    private Bitmap cornersCrop(BitmapPool pool, Bitmap source) {
        if (source == null) return null;

        Bitmap result = pool.get(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_4444);
        if (result == null) {
            result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_4444);
        }

        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setShader(new BitmapShader(source, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
        paint.setAntiAlias(true);
        RectF rectF = new RectF(0f, 0f, source.getWidth(), source.getHeight());
        //这里是对原图的bitmap进行圆角操作，固定半径在不同尺寸的图上产生不同的圆角效果
        //改为按比例计算半径
        float bitmapRadius=Math.min(source.getWidth(),source.getHeight())*0.05f;
        // TODO 如果原图尺寸太小 ?
        canvas.drawRoundRect(rectF, bitmapRadius, bitmapRadius, paint);
        return result;
    }

    @Override
    public String getId() {
        return getClass().getName();
    }
}
