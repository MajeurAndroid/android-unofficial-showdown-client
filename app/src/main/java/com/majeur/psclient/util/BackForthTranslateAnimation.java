package com.majeur.psclient.util;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

public class BackForthTranslateAnimation extends Animation {

    private static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private float mTranslateX;
    private float mTranslateY;

    public BackForthTranslateAnimation(float startX, float startY, float endX, float endY) {
        mTranslateX = endX - startX;
        mTranslateY = endY - startY;
        setInterpolator(INTERPOLATOR);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float m = (interpolatedTime > 0.5f ? 1f - interpolatedTime : interpolatedTime) * 2f;
        float x = mTranslateX * m;
        float y = mTranslateY * m;
        t.getMatrix().setTranslate(x, y);
    }
}
