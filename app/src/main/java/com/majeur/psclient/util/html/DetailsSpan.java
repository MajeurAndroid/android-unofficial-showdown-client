package com.majeur.psclient.util.html;

import android.text.Editable;
import android.text.TextPaint;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class DetailsSpan extends ClickableSpan {

    private CharSequence mContent;
    private boolean mExpanded;

    public void setContent(CharSequence summary) {
        mContent = summary;
    }

    @Override
    public void onClick(@NonNull View view) {
        if (!(view instanceof TextView)) return;
        if (!(((TextView) view).getText() instanceof Editable)) return;
        Editable text = ((TextView) view).getEditableText();

        int end = text.getSpanEnd(this);
        if (mExpanded) {
            text.delete(end, end + mContent.length() + 2);
        } else {
            text.insert(end, "\n");
            text.insert(end + 1, mContent);
            text.insert(end + mContent.length() + 1, "\n");
        }
        mExpanded = !mExpanded;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setFakeBoldText(true);
    }
}
