package com.majeur.psclient.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class SwitchLayout extends FrameLayout {

    public SwitchLayout(Context context) {
        super(context);
    }

    public SwitchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).layout((i * width), 0, (i + 1) * width, height);
        }
    }

    public void switchTo(int position) {
        setScrollX(position * getWidth());
    }

    public void smoothSwitchTo(final int position) {
        animate()
                .alpha(0f)
                .setDuration(50)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        switchTo(position);
                        animate()
                                .alpha(1f)
                                .setDuration(150)
                                .withEndAction(null)
                                .start();
                    }
                })
                .start();
    }

    public int getPosition() {
        return getScrollX() / getWidth();
    }
}
