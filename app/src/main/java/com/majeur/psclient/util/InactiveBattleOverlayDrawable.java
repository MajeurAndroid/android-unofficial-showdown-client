package com.majeur.psclient.util;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;

public class InactiveBattleOverlayDrawable extends ColorDrawable {

    private Paint mPaint;
    private Rect mTextBounds;
    private String mText1;
    private String mText2;

    public InactiveBattleOverlayDrawable(Resources resources) {
        super(0x90000000);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14,
                resources.getDisplayMetrics());
        mPaint.setTextSize(textSize);
        mPaint.setColor(Color.WHITE);
        mTextBounds = new Rect();
        mText1 = "No battle running";
    }

    public void setWinner(String user) {
        if (user == null) mText2 = null;
        else mText2 = "(" + user + " won !)";
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mPaint.getTextBounds(mText1, 0, mText1.length(), mTextBounds);
        int cx = getBounds().width() / 2;
        int cy = getBounds().height() / 2;
        canvas.drawText(mText1, cx - (mTextBounds.width() / 2), cy, mPaint);
        if (mText2 == null) return;
        int yOffset = mTextBounds.height();
        mPaint.getTextBounds(mText2, 0, mText2.length(), mTextBounds);
        canvas.drawText(mText2, cx - (mTextBounds.width() / 2), cy + yOffset, mPaint);
    }
}
