package com.majeur.psclient.util;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.majeur.psclient.model.Colors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CategoryDrawable extends Drawable {

    static final float PADDING_AMOUNT = 0.16f;
    static final float CORNER_RADIUS_AMOUNT = TypeDrawable.CORNER_RADIUS_AMOUNT;

    private int mCategoryColor;
    private Paint mPaint;
    private Path mPath;

    private int mPadding;
    private int mCornerRadius;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;

    public CategoryDrawable(String cat) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCategoryColor = Colors.categoryColor(cat.trim().toLowerCase());

        /* These will be overridden in onBoundsChange() */
        float density = Resources.getSystem().getDisplayMetrics().density;
        mIntrinsicWidth = (int) (32 * density);
        mIntrinsicHeight = (int) (14 * density);
        mPadding = (int) (2 * density);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect r = getBounds();
        mPaint.setColor(mCategoryColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(r.left, r.top, r.right, r.bottom, mCornerRadius, mCornerRadius, mPaint);

        canvas.save();
        canvas.translate(r.width() / 2, r.height() / 2);
        float ry = r.height() / 2 - mPadding;
        float rx = ry * 1.65f;
        drawInnerIcon(canvas, rx, ry);
        canvas.restore();
    }

    private void drawInnerIcon(Canvas canvas, float rx, float ry) {
        if (mCategoryColor == Colors.CATEGORY_PHYSICAL) {
            if (mPath == null) mPath = new Path();
            else mPath.reset();
            for (int s = -1; s <= 1; s+=2) {
                mPath.moveTo(0, -ry);
                mPath.lineTo(s*rx*0.22f, -ry*0.5f);
                mPath.lineTo(s*rx*0.68f, -ry*0.72f);
                mPath.lineTo(s*rx*0.5f, -ry*0.22f);
                mPath.lineTo(s*rx*1.1f, 0);
                mPath.lineTo(s*rx*0.5f, ry*0.22f);
                mPath.lineTo(s*rx*0.68f, ry*0.72f);
                mPath.lineTo(s*rx*0.22f, ry*0.5f);
                mPath.lineTo(0, ry);
            }
            mPaint.setColor(Colors.CATEGORY_PHY_INNER);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(mPath, mPaint);

        } else if (mCategoryColor == Colors.CATEGORY_SPECIAL) {
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawOval(-rx, -ry, rx, ry, mPaint);
            rx *= 0.6f;
            ry *= 0.6f;
            canvas.drawOval(-rx, -ry, rx, ry, mPaint);
            rx *= 0.48f;
            ry *= 0.48f;
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawOval(-rx, -ry, rx, ry, mPaint);

        } else if (mCategoryColor == Colors.CATEGORY_STATUS) {
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawOval(-rx, -ry, rx, ry, mPaint);
            if (mPath == null) mPath = new Path();
            else mPath.reset();
            float a = (float) (Math.PI * 0.32f);
            float x = (float) (rx * Math.cos(a));
            float y = (float) (ry * Math.sin(a));
            mPath.moveTo(x, -y);
            mPath.quadTo(-rx/2, -ry/2, 0, 0);
            mPath.quadTo(rx/2, ry/2, -x, y);
            mPath.arcTo(-rx, -ry, rx, ry, 90, -180, false);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(mPath, mPaint);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mPadding = (int) (bounds.height() * PADDING_AMOUNT);
        mCornerRadius = (int) (bounds.height() * CORNER_RADIUS_AMOUNT);
    }

    @Override
    public void setAlpha(int a) {
        mPaint.setAlpha(a);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }
}
