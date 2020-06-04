package com.majeur.psclient.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

public class TextTagSpan extends ReplacementSpan {

    private int mPadding;
    private int mBackgroundColor;
    private int mTextColor;
    private RectF mRectF;

    public TextTagSpan(int backgroundColor, int textColor) {
        super();
        mBackgroundColor = backgroundColor;
        mTextColor = textColor;
        mPadding = Utils.dpToPx(2);
        mRectF = new RectF();
    }


    public TextTagSpan(int textColor) {
        this(Utils.getTagColor(textColor), textColor);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return (int) (2*mPadding + paint.measureText(text.subSequence(start, end).toString()));
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        float textWidth = paint.measureText(text.subSequence(start, end).toString());
        mRectF.set(x, top + mPadding/2, x + textWidth + 2*mPadding, bottom - mPadding/2);
        paint.setColor(mBackgroundColor);
        canvas.drawRoundRect(mRectF, mPadding, mPadding, paint);
        paint.setColor(mTextColor);
        canvas.drawText(text, start, end, x + mPadding, y, paint);
    }
}

