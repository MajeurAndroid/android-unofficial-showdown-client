package com.majeur.psclient.util;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

public class ShakeTranslateAnimation extends Animation {

    private static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private int mAmplitude;
    private float mRevolutions;

    public ShakeTranslateAnimation(int amplitude, float shakeCount) {
        mAmplitude = amplitude;
        mRevolutions = shakeCount;
        setInterpolator(INTERPOLATOR);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float m = interpolatedTime > 0.5f ? 1f - interpolatedTime : interpolatedTime;
        float amplitude = mAmplitude * Math.min(1f, m * 10f);
        float a = (float) (interpolatedTime * 2. * Math.PI * mRevolutions);
        float x = (float) (amplitude * Math.cos(a));
        t.getMatrix().setTranslate(x, 0f);
    }
}
