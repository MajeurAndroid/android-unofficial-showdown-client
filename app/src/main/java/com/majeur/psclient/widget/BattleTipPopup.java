package com.majeur.psclient.widget;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.majeur.psclient.R;
import com.majeur.psclient.util.Utils;

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
    private int[] mTempArr = new int[2];
    private Rect mTempRect = new Rect();

    private OnBindPopupViewListener mOnBindPopupViewListener;

    private TextView mTitleView;
    private TextView mDescView;
    private ImageView mPlaceHolderTop;
    private ImageView mPlaceHolderBottom;

    public BattleTipPopup(Context context) {
        super(context);
        setBackgroundDrawable(null);
        setAnimationStyle(R.style.battle_popup_animation);

        View contentView = LayoutInflater.from(context).inflate(R.layout.popup_battle_tips, null);
        mTitleView = contentView.findViewById(R.id.title_text_view);
        mDescView = contentView.findViewById(R.id.descr_text_view);
        mPlaceHolderTop = contentView.findViewById(R.id.type_view);
        mPlaceHolderBottom = contentView.findViewById(R.id.category_view);
        setContentView(contentView);

        mThumbOffset = Utils.dpToPx(8);
    }

    public void addTippedView(View view) {
        view.setOnTouchListener(this);
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
        int y = Math.max(windowInsetTop, windowInsetTop + mTempArr[1] + mDownY - mTempRect.height() - mThumbOffset);
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

}
