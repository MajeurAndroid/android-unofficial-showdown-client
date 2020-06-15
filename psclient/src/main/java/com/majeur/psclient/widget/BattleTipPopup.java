package com.majeur.psclient.widget;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.majeur.psclient.R;
import com.majeur.psclient.util.Utils;

import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static java.lang.Math.max;

public class BattleTipPopup extends PopupWindow implements View.OnTouchListener {

    public interface OnBindPopupViewListener {
        public void onBindPopupView(View anchorView, TextView titleView, TextView descView,
                                    ImageView placeHolderTop, ImageView placeHolderBottom);
    }

    private int mThumbOffset;
    private boolean mIsUserTouching;
    private boolean mLongPressPerformed;
    private int mDownY;
    private View mAnchorView;
    private final int[] mTempArr = new int[2];
    private final Rect mTempRect = new Rect();

    private OnBindPopupViewListener mOnBindPopupViewListener;

    private final TextView mTitleView;
    private final TextView mDescView;
    private final ImageView mPlaceHolderTop;
    private final ImageView mPlaceHolderBottom;

    public BattleTipPopup(Context context) {
        super(context);
        setBackgroundDrawable(null);
        setAnimationStyle(R.style.Animation_PSClient_TipPopup);

        View contentView = LayoutInflater.from(context).inflate(R.layout.popup_battle_tips, null);
        mTitleView = contentView.findViewById(R.id.popup_title);
        mDescView = contentView.findViewById(R.id.popup_content);
        mPlaceHolderTop = contentView.findViewById(R.id.popup_im1);
        mPlaceHolderBottom = contentView.findViewById(R.id.popup_im2);
        setContentView(contentView);

        mThumbOffset = Utils.dpToPx(8);
    }

    public void addTippedView(View view) {
        view.setOnTouchListener(this);
    }

    public void removeTippedView(View view) {
        view.setOnTouchListener(null);
    }

    public void setOnBindPopupViewListener(OnBindPopupViewListener onBindPopupViewListener) {
        mOnBindPopupViewListener = onBindPopupViewListener;
    }

    private final Runnable mLongPressCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsUserTouching)
                return;
            mLongPressPerformed = true;

            if (mOnBindPopupViewListener != null)
                mOnBindPopupViewListener.onBindPopupView(mAnchorView, mTitleView, mDescView, mPlaceHolderTop, mPlaceHolderBottom);

            showPopup();
        }
    };

    // This one is a bit tricky. PopupWindow does not provide any support for showing a popup above a view.
    // We have to measure our content view ourselves (assuming our content view is the Popup's view itself, which
    // is the case when Popup's background is null). Then the Window created for the Popup is fullscreen with the
    // content view container using setFitsSystemWindows, so we have to offset our y coordinate to avoid issue
    // in measurement due to window inset.
    private void showPopup() {
        measureContentView(mTempRect);
        mAnchorView.getLocationInWindow(mTempArr);
        int x = mTempArr[0] + mAnchorView.getWidth() / 2 - mTempRect.width() / 2;
        int windowInsetTop = getTopWindowInset();
        int y = max(windowInsetTop, windowInsetTop + mTempArr[1] + mDownY - mTempRect.height() - mThumbOffset);
        showAtLocation(mAnchorView, Gravity.NO_GRAVITY, x, y);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        final int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mAnchorView = view;
                mDownY = (int) motionEvent.getY();
                mIsUserTouching = true;
                if (view.isClickable())
                    view.postDelayed(mLongPressCheckRunnable, ViewConfiguration.getLongPressTimeout());
                else
                    mLongPressCheckRunnable.run();
                return true;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsUserTouching = false;
                mAnchorView = null;
                if (mLongPressPerformed) {
                    mLongPressPerformed = false;
                    dismiss();
                } else {
                    view.performClick();
                }
                return true;

            default:
                return false;
        }
    }

    private void measureContentView(Rect out) {
        Rect displayFrame = out;
        mAnchorView.getWindowVisibleDisplayFrame(displayFrame);
        int wSpec = View.MeasureSpec.makeMeasureSpec(displayFrame.width(), View.MeasureSpec.AT_MOST);
        int hSpec = View.MeasureSpec.makeMeasureSpec(displayFrame.height(), View.MeasureSpec.AT_MOST);
        getContentView().measure(wSpec, hSpec);
        out.set(0, 0, getContentView().getMeasuredWidth(), getContentView().getMeasuredHeight());
    }

    private int getTopWindowInset() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
            return mAnchorView.getRootWindowInsets().getStableInsetTop();
        return 0;
    }

    // Really faster than its only equivalent using ConstraintLayout
    public static final class Layout extends ViewGroup {

        private final int mImageSpacing;

        private View mTitle;
        private View mContent;
        private View mImage1;
        private View mImage2;

        public Layout(@NonNull Context context, @Nullable AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public Layout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            mImageSpacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f,
                    getResources().getDisplayMetrics());
        }

        @Override
        public void onViewAdded(View child) {
            super.onViewAdded(child);
            switch (child.getId()) {
                case R.id.popup_title:
                    mTitle = child;
                    break;
                case R.id.popup_content:
                    mContent = child;
                    break;
                case R.id.popup_im1:
                    mImage1 = child;
                    break;
                case R.id.popup_im2:
                    mImage2 = child;
                    break;
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = getSize(widthMeasureSpec);
            int height = getSize(heightMeasureSpec);

            LayoutParams lp = mImage1.getLayoutParams();
            if (lp.width < 0 || lp.height < 0) throw new UnsupportedOperationException("popup_im1 must have explicit width and height");
            mImage1.measure(makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                    makeMeasureSpec(lp.height, MeasureSpec.EXACTLY));
            lp = mImage2.getLayoutParams();
            if (lp.width < 0 || lp.height < 0) throw new UnsupportedOperationException("popup_im2 must have explicit width and height");
            mImage2.measure(makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                    makeMeasureSpec(lp.height, MeasureSpec.EXACTLY));

            int imagesWidth = max(mImage1.getMeasuredWidth(), mImage2.getMeasuredWidth());

            mTitle.measure(makeMeasureSpec(width - imagesWidth, MeasureSpec.AT_MOST),
                    makeMeasureSpec(height, MeasureSpec.AT_MOST));
            mContent.measure(makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    makeMeasureSpec(height - mTitle.getMeasuredHeight(), MeasureSpec.AT_MOST));

            int measuredWidth = max(mTitle.getMeasuredWidth() + imagesWidth, mContent.getMeasuredWidth());
            int measuredHeight = mTitle.getMeasuredHeight() + mContent.getMeasuredHeight();
            setMeasuredDimension(measuredWidth, measuredHeight);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final int parentLeft = getPaddingLeft();
            final int parentRight = right - left - getPaddingRight();
            final int parentTop = getPaddingTop();
            final int parentBottom = bottom - top - getPaddingBottom();

            mTitle.layout(parentLeft,
                    parentTop,
                    parentLeft + mTitle.getMeasuredWidth(),
                    parentTop + mTitle.getMeasuredHeight());

            mContent.layout(parentLeft,
                    mTitle.getBottom(),
                    parentLeft + mContent.getMeasuredWidth(),
                    mTitle.getBottom() + mContent.getMeasuredHeight());

            mImage1.layout(parentRight - mImage1.getMeasuredWidth(),
                    parentTop,
                    parentRight,
                    parentTop + mImage1.getMeasuredHeight());

            mImage2.layout(parentRight - mImage2.getMeasuredWidth(),
                    mImage1.getBottom() + mImageSpacing,
                    parentRight,
                    mImage1.getBottom() + mImageSpacing + mImage2.getMeasuredHeight());
        }
    }
}
