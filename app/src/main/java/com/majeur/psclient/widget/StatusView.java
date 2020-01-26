package com.majeur.psclient.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Property;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.majeur.psclient.model.BattlingPokemon;
import com.majeur.psclient.model.Colors;
import com.majeur.psclient.model.StatModifiers;
import com.majeur.psclient.util.Utils;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.majeur.psclient.model.Id.toId;

public class StatusView extends View {

    private static final Property<StatusView, Float> HEALTH_PROP =
            new Property<StatusView, Float>(Float.class, "health") {
        @Override
        public void set(StatusView statusView, Float health) {
            statusView.mHealth = health;
            statusView.invalidate();
        }

        @Override
        public Float get(StatusView statusView) {
            return statusView.mHealth;
        }
    };
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

    private Paint mPaint;
    private Rect mTempRect;
    private Rect mTempRect2;
    private Rect mMeasureRect;
    private Canvas mMockCanvas;
    private Point mMeasurePoint;

    private String mLabel = "";
    private float mHealth = 0f;
    private String mStatus;
    private Set<String> mVolatileStatus;
    private ObjectAnimator mHealthAnimator;

    private Typeface mDefaultTypeFace;
    private Typeface mBoldTypeFace;

    private int mLabelsWidthLimit;

    private int mShadowRadius;
    private int mHorizontalMargin;

    private int mVerticalMargin;
    private int mLabelTextSize;

    private int mHealthBarWidth;
    private int mHealthBarHeight;
    private int mHealthBarStrokeWidth;
    private int mRectRadius;
    private int mExtraTextSize;
    private int mExtraMargin;

    private Map<String, Float> mStatsModifiers;

    public StatusView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTempRect = new Rect();
        mTempRect2 = new Rect();
        mMeasureRect = new Rect();
        mMockCanvas = new Canvas();
        mMeasurePoint = new Point();

        mHealthAnimator = new ObjectAnimator();
        mHealthAnimator.setInterpolator(new DecelerateInterpolator());
        mHealthAnimator.setDuration(500);
        mHealthAnimator.setTarget(this);
        mHealthAnimator.setProperty(HEALTH_PROP);

        mShadowRadius = Utils.dpToPx(4);
        mHorizontalMargin = Utils.dpToPx(4);
        mVerticalMargin = Utils.dpToPx(4);
        mExtraMargin = Utils.dpToPx(1.5f);

        mLabelTextSize = Utils.dpToPx(14);

        mHealthBarWidth = Utils.dpToPx(126);
        mHealthBarHeight = Utils.dpToPx(5);
        mHealthBarStrokeWidth = Utils.dpToPx(1);

        mRectRadius = Utils.dpToPx(2);
        mExtraTextSize = Utils.dpToPx(9);

        mLabelsWidthLimit = mHealthBarWidth + mHealthBarStrokeWidth + mShadowRadius;

        mDefaultTypeFace = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        mBoldTypeFace = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        mStatsModifiers = new HashMap<>();
        mStatsModifiers.put("atk", 1f);
        mStatsModifiers.put("def", 1f);
        mStatsModifiers.put("spa", 1f);
        mStatsModifiers.put("spd", 1f);
        mStatsModifiers.put("spe", 1f);
        mStatsModifiers.put("eva", 1f);

        mVolatileStatus = new HashSet<>();

    }

    public void setPokemon(BattlingPokemon pokemon) {
        mLabel = pokemon.name + " " + Objects.toString(pokemon.gender, "") + " l." + pokemon.level;
        mHealth = pokemon.condition.health;
        mStatus = pokemon.condition.status;
        mVolatileStatus.clear();
        updateModifier(pokemon.statModifiers);
    }

    public void setHealth(float health) {
        if (mHealthAnimator.isStarted())
            mHealthAnimator.cancel();
        mHealthAnimator.setFloatValues(health);
        mHealthAnimator.start();
    }

    public void setStatus(String status) {
        mStatus = status;
        requestLayout();
        invalidate();
    }

    public void updateModifier(StatModifiers statModifiers) {
        for (String statKey : StatModifiers.STAT_KEYS)
            mStatsModifiers.put(statKey, statModifiers.modifier(statKey));
        requestLayout();
        invalidate();
    }

    public void addVolatileStatus(String vStatus) {
        if (mVolatileStatus.add(vStatus.trim())) {
            requestLayout();
            invalidate();
        }
    }

    public void removeVolatileStatus(String vStatus) {
        if (mVolatileStatus.remove(vStatus.trim())) {
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMeasurePoint.set(0, 0);
        drawContent(mMockCanvas, mMeasurePoint);
        setMeasuredDimension(mMeasurePoint.x, mMeasurePoint.y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawContent(canvas, null);
    }

    private void drawContent(Canvas canvas, Point measurePoint) {
        Rect measureRect = mMeasureRect;
        measureRect.setEmpty();

        if (TextUtils.isEmpty(mLabel)) return; // Nothing to draw

        drawLabelText(canvas, measureRect);
        updateMeasurePoint(measurePoint, measureRect);

        drawHealthBar(canvas, measureRect);
        updateMeasurePoint(measurePoint, measureRect);

        int y = measureRect.bottom;
        drawStatus(canvas, measureRect);
        updateMeasurePoint(measurePoint, measureRect);

        for (String vStatus : mVolatileStatus) {
            mTempRect2.set(measureRect);
            drawVolatileStatus(mMockCanvas, measureRect, y, vStatus);
            if (measureRect.right > mLabelsWidthLimit) {
                y = measureRect.bottom + measureRect.height() / 2 - mShadowRadius - mExtraMargin;
                measureRect.set(mHorizontalMargin, y, mHorizontalMargin, y);
            } else {
                measureRect.set(mTempRect2);
            }

            drawVolatileStatus(canvas, measureRect, y, vStatus);
            updateMeasurePoint(measurePoint, measureRect);
        }

        for (Map.Entry<String, Float> entry : mStatsModifiers.entrySet()) {
            if (entry.getValue().equals(1f))
                continue;
            mTempRect2.set(measureRect);
            drawStatModifier(mMockCanvas, measureRect, y, entry);
            if (measureRect.right > mLabelsWidthLimit) {
                y = measureRect.bottom + measureRect.height() / 2 - mShadowRadius - mExtraMargin;
                measureRect.set(mHorizontalMargin, y, mHorizontalMargin, y);
            } else {
                measureRect.set(mTempRect2);
            }
            drawStatModifier(canvas, measureRect, y, entry);
            updateMeasurePoint(measurePoint, measureRect);
        }
    }


    private void drawLabelText(Canvas canvas, Rect measureRect) {
        mPaint.setTypeface(mBoldTypeFace);
        mPaint.setColor(Colors.WHITE);
        mPaint.setTextSize(mLabelTextSize);
        mPaint.getTextBounds(mLabel, 0, mLabel.length(), mTempRect);
        mPaint.setShadowLayer(mShadowRadius, 0, mShadowRadius / 3, Colors.BLACK);
        int y = measureRect.bottom;
        canvas.drawText(mLabel, mHorizontalMargin, y + mTempRect.height(), mPaint);
        measureRect.set(mHorizontalMargin,
                y,
                2 * mHorizontalMargin + mTempRect.width(),
                y + mTempRect.height() + mVerticalMargin);

        mPaint.clearShadowLayer();
    }

    private void drawHealthBar(Canvas canvas, Rect measureRect) {
        mPaint.setStrokeWidth(mHealthBarStrokeWidth);
        mPaint.setColor(Colors.WHITE);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setShadowLayer(mShadowRadius, 0, mShadowRadius / 3, Colors.BLACK);
        int y = measureRect.bottom;
        canvas.drawRoundRect(mShadowRadius, y, mShadowRadius + mHealthBarWidth, y + mHealthBarHeight, mRectRadius, mRectRadius, mPaint);
        measureRect.set(mShadowRadius - mHealthBarStrokeWidth / 2, y - mHealthBarStrokeWidth / 2,
                mHealthBarWidth + mHealthBarStrokeWidth / 2 + 2*mShadowRadius,
                y + mHealthBarHeight + mHealthBarStrokeWidth / 2 + mVerticalMargin);
        mPaint.clearShadowLayer();

        mPaint.setColor(Colors.healthColor(mHealth));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(mShadowRadius, y, mShadowRadius + (int) Math.ceil(mHealthBarWidth * mHealth), y + mHealthBarHeight, mRectRadius, mRectRadius, mPaint);
    }



    private void drawStatus(Canvas canvas, Rect measureRect) {
        int y = measureRect.bottom;

        if (mStatus == null) {
            measureRect.set(mHorizontalMargin, y, mHorizontalMargin, y);
            return;
        }

        drawTextWithBackground(canvas, measureRect, mHorizontalMargin + mShadowRadius,
                y + mHealthBarStrokeWidth, mStatus.toUpperCase(), mExtraTextSize,
                Colors.WHITE, Colors.statusColor(toId(mStatus)));
    }

    private void drawVolatileStatus(Canvas canvas, Rect measureRect, int y, String vStatus) {
        int x = measureRect.right;
        drawTextWithBackground(canvas, measureRect, x + mExtraMargin, y + mHealthBarStrokeWidth,
                vStatus, mExtraTextSize, Colors.VOLATILE_STATUS, Colors.WHITE);
    }

    @SuppressWarnings("DefaultLocale")
    private void drawStatModifier(Canvas canvas, Rect measureRect, int y, Map.Entry<String, Float> entry) {
        String text = DECIMAL_FORMAT.format(entry.getValue()) + Utils.firstCharUpperCase(entry.getKey());

        int x = measureRect.right;
        drawTextWithBackground(canvas, measureRect, x + mExtraMargin, y + mHealthBarStrokeWidth,
                text, mExtraTextSize, entry.getValue() < 1f ? Colors.STAT_UNBOOST : Colors.STAT_BOOST, Colors.WHITE);
    }

    private void drawTextWithBackground(Canvas canvas, Rect measureRect, int x, int cY,
                                        String text, int textSize, int color, int bgColor) {
        mPaint.setTypeface(mDefaultTypeFace);
        mPaint.setTextSize(textSize);
        mPaint.getTextBounds(text, 0, text.length(), mTempRect);

        mPaint.setShadowLayer(mShadowRadius, 0, mShadowRadius / 3, Colors.BLACK);
        mPaint.setColor(bgColor);
        int w = mTempRect.width() + 2 * mRectRadius;
        int h = mTempRect.height() + 2 * mRectRadius;
        canvas.drawRoundRect(x, cY - h / 2, x + w, cY + h / 2, mRectRadius, mRectRadius, mPaint);
        measureRect.set(x, cY - h / 2, x + w + mShadowRadius, cY + h / 2 + mShadowRadius);
        mPaint.clearShadowLayer();

        mPaint.setColor(color);
        canvas.drawText(text, x + mRectRadius, cY - mTempRect.height() / 2 - mTempRect.top, mPaint);
    }

    private void updateMeasurePoint(Point measurePoint, Rect measureRect) {
        if (measurePoint == null)
            return;
        measurePoint.set(Math.max(measurePoint.x, measureRect.right),
                Math.max(measurePoint.y, measureRect.bottom));
    }
}
