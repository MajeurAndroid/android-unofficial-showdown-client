package com.majeur.psclient.util.html;

import android.graphics.Color;
import android.text.TextPaint;
import androidx.annotation.NonNull;

public abstract class ClickableSpan extends android.text.style.ClickableSpan {

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        // Hacky way of allowing ForegroundSpan to override ClickableSpan's text color
        if (ds.getColor() == Color.BLACK) ds.setColor(Color.BLUE);
        ds.setUnderlineText(true);
    }
}
