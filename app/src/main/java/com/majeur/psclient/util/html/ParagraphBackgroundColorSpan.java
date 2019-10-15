package com.majeur.psclient.util.html;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;

import androidx.annotation.NonNull;

public class ParagraphBackgroundColorSpan implements LeadingMarginSpan {

    private int mColor;

    public ParagraphBackgroundColorSpan(int color) {
        mColor = color;
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

        p.setStyle(Paint.Style.FILL);
        p.setColor(mColor);
        c.drawRect(x, top, x + layout.getWidth(), bottom, p);

        p.setStyle(style);
        p.setColor(color);
    }
}
