package com.majeur.psclient.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.majeur.psclient.model.Colors;
import com.majeur.psclient.model.Nature;
import com.majeur.psclient.model.Stats;
import com.majeur.psclient.util.Utils;

import androidx.annotation.Nullable;

/*
 * This widget needs improvements, it works well but is a bit hacky.
 */
public class StatsTable extends View {

    private static final String[] COLUMN_NAMES = {"Stat", "Base", "EVs", "IVs", "Total", ""};
    private static final float[] COLUMN_WEIGHTS = {0.20f, 0.14f, 0.14f, 0.14f, 0.28f, 0.10f};

    private static final String[] STAT_NAMES = {"HP", "Attack", "Defense", "Sp. Atk.", "Sp. Def.", "Speed"};

    private static final int BASE = 0;
    private static final int EVS = 1;
    private static final int IVS = 2;
    private static final int TOTAL = 3;
    private static final int MAX_EV_SUM = 510;

    private float mTextSize;
    private int mMargin;
    private int mBarThickness;
    private int mWidth;
    private Rect mRect;
    private Paint mPaint;
    private Point mMeasurePoint;
    private int mTextColor;

    private OnRowClickListener mRowClickListener;

    /*       HP Atk Def Spa Spd Spe
     * Base
     * EVs
     * IVs
     * Total
     */
    private int[][] mStatData = new int[4][6];
    private int mLevel = 100;
    private Nature mNature = Nature.Serious;
    private StringBuilder mStringBuilder = new StringBuilder();

    public StatsTable(Context context) {
        this(context, null);
    }

    public StatsTable(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatsTable(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(true);
        mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        mMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22, getResources().getDisplayMetrics());
        mBarThickness = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextSize(mTextSize);
        mPaint.setPathEffect(new DashPathEffect(new float[] {3, 3}, 0));
        mMeasurePoint = new Point();
        mRect = new Rect();
        mTextColor = Color.BLACK;
    }

    public void setLevel(int level) {
        mLevel = level;
        invalidateData();
    }

    public void setNature(Nature nature) {
        mNature = nature;
        invalidateData();
    }

    public void setBaseStats(Stats baseStats) {
        mStatData[BASE] = baseStats.toArray();
        invalidateData();
    }

    public void setEVs(Stats evs) {
        mStatData[EVS] = evs.toArray();
        invalidateData();
    }

    public void setIVs(Stats ivs) {
        mStatData[IVS] = ivs.toArray();
        invalidateData();
    }

    private void invalidateData() {
        for (int i = 0; i < 6; i++) {
            if (mStatData[BASE][i] == 0)
                mStatData[TOTAL][i] = 0;
            else if (i == 0)
                mStatData[TOTAL][i] = Stats.calculateHp(mStatData[BASE][i], mStatData[IVS][i],
                        mStatData[EVS][i], mLevel);
            else
                mStatData[TOTAL][i] = Stats.calculateStat(mStatData[BASE][i], mStatData[IVS][i],
                        mStatData[EVS][i], mLevel, mNature.getStatModifier(i));
        }
        invalidate();
    }

    public void clear() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                mStatData[i][j] = 0;
            }
        }
        invalidate();
    }

    public void setRowClickListener(OnRowClickListener rowClickListener) {
        mRowClickListener = rowClickListener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mTextColor = enabled ? Color.BLACK : Color.GRAY;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        mMeasurePoint.set(wSize, getPaddingTop());
        mMeasurePoint.y += measureHeader();
        for (int i = 0; i < STAT_NAMES.length; i++) {
            mMeasurePoint.y += measureRow(i);
        }
        mMeasurePoint.y += measureLastLine();
        mMeasurePoint.y += getPaddingBottom();
        setMeasuredDimension(mMeasurePoint.x, mMeasurePoint.y);
    }

    private int measureHeader() {
        mPaint.setFakeBoldText(true);
        mPaint.setColor(mTextColor);
        mStringBuilder.setLength(0);
        for (String name : COLUMN_NAMES) mStringBuilder.append(name);
        mPaint.getTextBounds(mStringBuilder.toString(), 0, mStringBuilder.length(), mRect);
        return mRect.height();
    }

    private int measureRow(int index) {
        mPaint.setFakeBoldText(false);
        mStringBuilder.setLength(0);
        mStringBuilder.append(STAT_NAMES[index]).append(mStatData[0][index]).append(mStatData[1][index])
                .append(mStatData[2][index]).append(mStatData[3][index]);
        mPaint.getTextBounds(mStringBuilder.toString(), 0, mStringBuilder.length(), mRect);
        return mRect.height() + mMargin;
    }

    private int measureLastLine() {
        int sum = 0;
        for (int ev : mStatData[EVS]) sum += ev;
        if (sum <= MAX_EV_SUM) return 0;
        String text = "Too much evs: " + (sum - MAX_EV_SUM);
        mPaint.getTextBounds(text, 0, text.length(), mRect);
        return mRect.height() + mMargin;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mMeasurePoint.set(0, 0);
        mMeasurePoint.y = getPaddingTop();
        drawHeaderRow(canvas, mMeasurePoint);
        for (int i = 0; i < 6; i++)
            drawStatRow(canvas, mMeasurePoint, STAT_NAMES[i], mStatData[0][i], mStatData[1][i],
                    mStatData[2][i], mStatData[3][i]);
        drawLastLine(canvas, mMeasurePoint);
    }

    private void drawHeaderRow(Canvas canvas, Point measurePoint) {
        int width = mWidth - getPaddingStart() - getPaddingEnd();
        mPaint.setFakeBoldText(true);
        mPaint.setColor(mTextColor);
        mStringBuilder.setLength(0);
        for (String name : COLUMN_NAMES) mStringBuilder.append(name);
        mPaint.getTextBounds(mStringBuilder.toString(), 0, mStringBuilder.length(), mRect);
        int baseLine =  measurePoint.y - mRect.top;
        int lineHeight = mRect.height();
        if (measurePoint.y + lineHeight > measurePoint.y)
            measurePoint.y += lineHeight;

        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            int x = getPaddingStart();
            for (int j = 0; j < i; j++) x += (int) (width * COLUMN_WEIGHTS[j]);
            canvas.drawText(COLUMN_NAMES[i], x, baseLine, mPaint);
        }
        mPaint.setFakeBoldText(false);
    }

    private void drawStatRow(Canvas canvas, Point measurePoint, String statName, int base, int evs, int ivs, int total) {
        int width = mWidth - getPaddingStart() - getPaddingEnd();

        String willDraw = statName + base + evs + ivs + total;
        mPaint.getTextBounds(willDraw, 0, willDraw.length(), mRect);
        int rowY = measurePoint.y;
        rowY += mMargin; // Add top margin
        int baseLine =  rowY - mRect.top;
        int lineHeight = mRect.height();
        if (measurePoint.y + lineHeight + mMargin > measurePoint.y)
            measurePoint.y += lineHeight + mMargin;

        int x = getPaddingStart();
        mPaint.setColor(Color.GRAY);
        canvas.drawText(statName, x, baseLine, mPaint);
        int lx = x + (int) mPaint.measureText(statName);

        x += (int) (COLUMN_WEIGHTS[0] * width);
        mPaint.setColor(Color.GRAY);
        canvas.drawLine(lx, baseLine, x, baseLine, mPaint);
        String text = Integer.toString(base);
        mPaint.setColor(mTextColor);
        canvas.drawText(text, x, baseLine, mPaint);
        lx = x + (int) mPaint.measureText(text);

        x += (int) (COLUMN_WEIGHTS[1] * width);
        mPaint.setColor(Color.GRAY);
        canvas.drawLine(lx, baseLine, x, baseLine, mPaint);
        text = Integer.toString(evs);
        mPaint.setColor(mTextColor);
        canvas.drawText(text, x, baseLine, mPaint);
        lx = x + (int) mPaint.measureText(text);

        x += (int) (COLUMN_WEIGHTS[2] * width);
        mPaint.setColor(Color.GRAY);
        canvas.drawLine(lx, baseLine, x, baseLine, mPaint);
        text = Integer.toString(ivs);
        mPaint.setColor(mTextColor);
        canvas.drawText(text, x, baseLine, mPaint);
        lx = x + (int) mPaint.measureText(text);

        x += (int) (COLUMN_WEIGHTS[3] * width);
        mPaint.setColor(Color.GRAY);
        canvas.drawLine(lx, baseLine, x, baseLine, mPaint);
        int maxWidth = (int) (COLUMN_WEIGHTS[4] * width) - mMargin;
        int barWidth = (int) (Math.min((total / 504f), 1f) * maxWidth);
        if (statName.equalsIgnoreCase("hp")) barWidth = (int) (Math.min((total / 704f), 1f) * maxWidth);
        mPaint.setColor(getStatColor(total));
        canvas.drawRect(x, baseLine - mBarThickness, x + barWidth, baseLine, mPaint);
        lx = x + barWidth;

        text = Integer.toString(total);
        mPaint.setColor(mTextColor);
        mPaint.setFakeBoldText(true);
        float dx = mPaint.measureText(text);
        canvas.drawText(text, width - dx, baseLine, mPaint);
        mPaint.setFakeBoldText(false);

        mPaint.setColor(Color.GRAY);
        canvas.drawLine(lx, baseLine, width - dx, baseLine, mPaint);
    }

    private int getStatColor(int s) {
        int h = (int) (s * 180f / 714f);
        if (h > 360) h = 360;
        float[] rgb = Utils.hslToRgb(h, 40, 75);
        return Utils.rgb(rgb[0], rgb[1], rgb[2]);
    }

    private void drawLastLine(Canvas canvas, Point measurePoint) {
        int sum = 0;
        for (int ev : mStatData[EVS]) sum += ev;
        if (sum <= MAX_EV_SUM) return;

        String text = "Too much evs: " + (sum - MAX_EV_SUM);

        mPaint.setColor(Colors.RED);
        mPaint.setFakeBoldText(false);
        mPaint.getTextBounds(text, 0, text.length(), mRect);
        canvas.drawText(text, measurePoint.y - mRect.top, mWidth - mRect.width() - mRect.right, mPaint);
    }

    private Rect mTouchedBounds = new Rect();
    private int mTouchedRowIndex = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Drawable bg = getBackground();
            int touchY = (int) event.getY();
            int y = measureHeader() + getPaddingTop();
            if (touchY < y) {
                mTouchedBounds.set(0, 0, 0, 0);
                bg.setBounds(mTouchedBounds);
                mTouchedRowIndex = -1;
                return super.onTouchEvent(event);
            }

            for (int i = 0; i < STAT_NAMES.length; i++) {
                int rowHeight = measureRow(i);
                if (touchY < y + rowHeight + mMargin / 4) {
                    mTouchedBounds.set(0, y + mMargin / 4, getWidth(), y + rowHeight + mMargin / 4);
                    bg.setBounds(mTouchedBounds);
                    mTouchedRowIndex = i;
                    return super.onTouchEvent(event);
                }
                y += rowHeight;
            }

            // Off table
            mTouchedBounds.set(0, 0, 0, 0);
            bg.setBounds(mTouchedBounds);
            mTouchedRowIndex = -1;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mTouchedBounds.contains((int) event.getX(), (int) event.getY()))
                if (mRowClickListener != null)
                    mRowClickListener.onRowClicked(this, STAT_NAMES[mTouchedRowIndex], mTouchedRowIndex);
        }
        return super.onTouchEvent(event);
    }

    public interface OnRowClickListener {

        public void onRowClicked(StatsTable statsTable, String rowName, int rowIndex);
    }

    //TODO total evs
}
