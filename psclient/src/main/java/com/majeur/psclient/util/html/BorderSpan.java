package com.majeur.psclient.util.html;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;
import androidx.annotation.NonNull;

public class BorderSpan implements LeadingMarginSpan {

    private int mColor;
    private float mBorderWidth;

    public BorderSpan(int color, float borderWidth) {
        super();
        mColor = color;
        mBorderWidth = borderWidth;
    }

    @Override
    public int getLeadingMargin(boolean b) {
        return 0;
    }

    @Override
    public void drawLeadingMargin(@NonNull Canvas c, @NonNull Paint p, int x, int dir, int top, int baseline, int bottom,
                                  @NonNull CharSequence text, int start, int end, boolean first, @NonNull Layout layout) {
        Paint.Style style = p.getStyle();
        int color = p.getColor();
        float strokeW = p.getStrokeWidth();

        p.setStyle(Paint.Style.STROKE);
        p.setColor(mColor);
        p.setStrokeWidth(mBorderWidth);

        float xStart = x + (mBorderWidth / 2);
        float xEnd = x + layout.getWidth() - (mBorderWidth / 2);
        c.drawLine(xStart, bottom, xStart, top, p);
        c.drawLine(xEnd, bottom, xEnd, top, p);

        int st = ((Spanned) text).getSpanStart(this);
        int currentLine = layout.getLineForOffset(end);
        int en = ((Spanned) text).getSpanEnd(this);
        int endLine = layout.getLineForOffset(en);
        if (start == st) {
            float t = top + mBorderWidth / 2;
            c.drawLine(xStart, t, xEnd, t, p);
        } else if (currentLine == endLine) {
            float b = bottom - mBorderWidth / 2;
            c.drawLine(xStart, b, xEnd, b, p);
        }

        p.setStyle(style);
        p.setColor(color);
        p.setStrokeWidth(strokeW);
    }
}
