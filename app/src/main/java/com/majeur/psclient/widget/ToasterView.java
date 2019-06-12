package com.majeur.psclient.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import com.majeur.psclient.util.Utils;

import java.util.LinkedList;
import java.util.Queue;

public class ToasterView extends FrameLayout {

    private Queue<ToastView> mToastViewCache;

    public ToasterView(Context context) {
        super(context);
        mToastViewCache = new LinkedList<>();
    }

    public void makeToast(String text, int color) {
        final ToastView toastView = obtainToastView();
        addView(toastView);
        toastView.setText(text);
        toastView.setColor(color);
        post(new Runnable() {
            @Override
            public void run() {
                startToastAnimation(toastView);
            }
        });
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        mToastViewCache.add((ToastView) child);
    }

    private void startToastAnimation(final ToastView toastView) {
        toastView.setTranslationY(0);
        toastView.setAlpha(1f);
        toastView.animate()
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .translationY(-toastView.getHeight())
                .alpha(0f)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        removeView(toastView);
                    }
                })
                .start();
    }

    private ToastView obtainToastView() {
        if (!mToastViewCache.isEmpty())
            return mToastViewCache.poll();

        ToastView toastView = new ToastView(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        toastView.setLayoutParams(layoutParams);
        return toastView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int childWidthSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST);
        int maxWidth = 0;
        int maxHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.measure(childWidthSpec, childHeightSpec);
            maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
            maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
        }

        setMeasuredDimension(maxWidth, maxHeight * 2);
    }

    private static class ToastView extends View {

        private Paint mPaint;
        private Rect mRect;
        private int mCornerRadius;
        private int mShadowRadius;

        private String mText;
        private int mColor;

        public ToastView(Context context) {
            super(context);
            mPaint = new Paint();
            mRect = new Rect();
            setLayerType(LAYER_TYPE_HARDWARE, null);
            mCornerRadius = Utils.dpToPx(5);
            mShadowRadius = Utils.dpToPx(4);
            mPaint.setTextSize(Utils.dpToPx(14));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            mPaint.getTextBounds(mText, 0, mText.length(), mRect);
            setMeasuredDimension(mRect.width() + 2 * mCornerRadius + 2 * mShadowRadius,
                    mRect.height() + 2 * mCornerRadius + mShadowRadius);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            mPaint.setColor(mColor);
            mPaint.setShadowLayer(mShadowRadius, 0, mShadowRadius / 3, Color.BLACK);
            canvas.drawRoundRect(mShadowRadius, 0, getWidth() - mShadowRadius, getHeight() - mShadowRadius,
                    mCornerRadius, mCornerRadius, mPaint);
            mPaint.setColor(Color.WHITE);
            mPaint.clearShadowLayer();
            canvas.drawText(mText, mShadowRadius + mCornerRadius, mCornerRadius + mRect.height(), mPaint);
        }

        public void setText(String text) {
            mText = text;
        }

        public void setColor(int color) {
            mColor = color;
        }
    }
}
