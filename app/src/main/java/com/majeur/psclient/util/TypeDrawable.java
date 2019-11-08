package com.majeur.psclient.util;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.majeur.psclient.model.Colors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/*
 * More convenient than a switch statement and 18 drawable resources.
 * Also much faster at runtime.
 */
public class TypeDrawable extends Drawable {

    static final float PADDING_AMOUNT = 0.2875f;
    static final float CORNER_RADIUS_AMOUNT = 0.15f;

    private String mTypeText;
    private int mTypeColor;
    private Paint mPaint;
    private Rect mTextRect;

    private int mPadding;
    private int mCornerRadius;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;

    public TypeDrawable(String type) {
        mTypeText = type.trim().toUpperCase();
        mTypeColor = Colors.typeColor(type.trim().toLowerCase());
        if (mTypeColor == 0) mTypeText = "???";

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        mPaint.setLetterSpacing(lenToSpacing(mTypeText.length()));
        mTextRect = new Rect();

        /* These will be overridden in onBoundsChange() */
        float density = Resources.getSystem().getDisplayMetrics().density;
        mIntrinsicWidth = (int) (32 * density);
        mIntrinsicHeight = (int) (14 * density);
        mPadding = (int) (2 * density);
        mPaint.setTextSize(16);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect r = getBounds();
        mPaint.setColor(mTypeColor);
        canvas.drawRoundRect(r.left, r.top, r.right, r.bottom, mCornerRadius, mCornerRadius, mPaint);
        mPaint.setColor(Color.WHITE);
        mPaint.getTextBounds(mTypeText, 0, mTypeText.length(), mTextRect);
        canvas.save();
        canvas.translate(r.width() / 2 - mTextRect.width() / 2, r.height() / 2 - mTextRect.height() / 2);
        canvas.drawText(mTypeText, r.left - mTextRect.left, r.top - mTextRect.top, mPaint);
        canvas.restore();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (bounds.width() == 0 || bounds.height() == 0) return;

        mPadding = (int) (bounds.height() * PADDING_AMOUNT);
        mCornerRadius = (int) (bounds.height() * CORNER_RADIUS_AMOUNT);

        float textSize = bounds.height() * 0.65f;
        mPaint.setTextSize(textSize);
        mPaint.getTextBounds(mTypeText, 0, mTypeText.length(), mTextRect);
        while ((bounds.height() - 2 * mPadding) < mTextRect.height()) {
            textSize -= textSize * 0.05f;
            mPaint.setTextSize(textSize);
            mPaint.getTextBounds(mTypeText, 0, mTypeText.length(), mTextRect);
        }
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

    private static float lenToSpacing(int l) {
        // Ok maybe I went to far for this one
        return 0.00022f * (float) Math.exp(17.345-3.85*l) - 0.022f * l + 0.14f;

    }
}
