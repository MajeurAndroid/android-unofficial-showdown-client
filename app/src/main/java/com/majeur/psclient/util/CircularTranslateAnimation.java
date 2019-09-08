package com.majeur.psclient.util;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

public class CircularTranslateAnimation extends Animation {

    private static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private int mRadius;
    private float mRevolutions;

    public CircularTranslateAnimation(int radius, float revolutions) {
        mRadius = radius;
        mRevolutions = revolutions;
        setInterpolator(INTERPOLATOR);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float m = interpolatedTime > 0.5f ? 1f - interpolatedTime : interpolatedTime;
        float radius = mRadius * Math.min(1f, m * 10f);
        float a = (float) (interpolatedTime * 2. * Math.PI * mRevolutions);
        float x = (float) (radius * Math.cos(a));
        float y = (float) (radius * Math.sin(a));
        t.getMatrix().setTranslate(x, y);
    }
}
