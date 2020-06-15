package com.majeur.psclient.util.html;

import android.graphics.Color;
import android.text.TextPaint;
import androidx.annotation.NonNull;

public class URLSpan extends android.text.style.URLSpan {

    public URLSpan(String url) {
        super(url);
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        // Hacky way of allowing ForegroundSpan to override URLSpan's text color
        if (ds.getColor() == Color.BLACK) ds.setColor(Color.BLUE);
        ds.setUnderlineText(true);
    }
}
