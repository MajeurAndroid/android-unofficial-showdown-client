package com.majeur.psclient.util.html;

import android.graphics.Color;
import android.text.TextPaint;
import android.view.View;
import androidx.annotation.NonNull;

public class ClickableSpan extends android.text.style.ClickableSpan {

    private final OnClickListener mOnClickListener;

    public ClickableSpan() {
        this(null);
    }

    public ClickableSpan(OnClickListener listener) {
        super();
        mOnClickListener = listener;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        // Hacky way of allowing ForegroundSpan to override ClickableSpan's text color
        if (ds.getColor() == Color.BLACK) ds.setColor(Color.BLUE);
        ds.setUnderlineText(true);
    }

    @Override
    public void onClick(@NonNull View widget) {
        if (mOnClickListener != null) mOnClickListener.onClick(widget);
    }

    public interface OnClickListener {
        void onClick(@NonNull View widget);
    }
}
