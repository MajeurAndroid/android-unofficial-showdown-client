package com.majeur.psclient.util;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

public class BackgroundBitmapDrawable extends BitmapDrawable {

    private final Paint mPaint;
    private final Matrix mMatrix;
    private int moldHeight;
    boolean simpleMapping = false;
    boolean dimBitmap = false;

    public BackgroundBitmapDrawable(Resources res, int resId) {
        super(res, BitmapFactory.decodeResource(res, resId));
        int nightModeFlags = res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) dimBitmap = true;
        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        mMatrix = new Matrix();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (bounds.height() > moldHeight) {
            moldHeight = bounds.height();
            Bitmap b = getBitmap();
            RectF src = new RectF(0, 0, b.getWidth(), b.getHeight());
            RectF dst;

            if (simpleMapping) {
                dst = new RectF(bounds);
                mMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
            } else {
                // Full Screen Image -> Always scale and center-crop in order to fill the screen
                float dwidth = src.width();
                float dheight = src.height();

                float vwidth = bounds.width();
                float vheight = bounds.height();

                float scale;
                float dx = 0, dy = 0;

                if (dwidth * vheight > vwidth * dheight) {
                    scale = (float) vheight / (float) dheight;
                    dx = (vwidth - dwidth * scale) * 0.5f;
                } else {
                    scale = (float) vwidth / (float) dwidth;
                    dy = (vheight - dheight * scale) * 0.5f;
                }

                mMatrix.setScale(scale, scale);
                mMatrix.postTranslate(dx, dy);

            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(getBitmap(), mMatrix, mPaint);
        if (dimBitmap) canvas.drawColor(0xC6000000);
    }
}
