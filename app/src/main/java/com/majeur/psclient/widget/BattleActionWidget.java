package com.majeur.psclient.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Property;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import com.majeur.psclient.R;
import com.majeur.psclient.model.Move;
import com.majeur.psclient.model.SidePokemon;
import com.majeur.psclient.util.Utils;

import java.util.LinkedList;
import java.util.List;

public class BattleActionWidget extends FrameLayout implements View.OnClickListener {

    private static final Property<BattleActionWidget, Float> CONTENT_ALPHA =
            new Property<BattleActionWidget, Float>(Float.class, "contentAlpha") {
                @Override
                public void set(BattleActionWidget battleActionWidget, Float value) {
                    battleActionWidget.setContentAlpha(value);
                }

                @Override
                public Float get(BattleActionWidget battleActionWidget) {
                    return battleActionWidget.mContentAlpha;
                }
            };

    public interface OnRevealListener {
        public void onReveal(boolean in);
    }

    private static final long ANIM_REVEAL_DURATION = 250;
    private static final long ANIM_FADE_DURATION = 100;

    private float mContentAlpha;
    private Paint mPaint;
    private ObjectAnimator mContentAlphaAnimator;
    private List<Button> mMoveButtons;
    private CheckBox mMovesCheckBox;
    private List<SwitchButton> mSwitchButtons;

    private OnChoiceListener mOnChoiceListener;

    private Animator mCurrentReveal;
    private boolean mRevealingIn;
    private boolean mRevealingOut;
    private OnRevealListener mOnRevealListener;

    public BattleActionWidget(Context context) {
        this(context, null);
    }

    public BattleActionWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BattleActionWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        setWillNotDraw(false);
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(R.color.divider));
        mPaint.setStrokeWidth(Utils.dpToPx(1));
        setVisibility(GONE);

        mContentAlphaAnimator = new ObjectAnimator();
        mContentAlphaAnimator.setInterpolator(new DecelerateInterpolator());
        mContentAlphaAnimator.setDuration(125);
        mContentAlphaAnimator.setTarget(this);
        mContentAlphaAnimator.setProperty(CONTENT_ALPHA);

        mMoveButtons = new LinkedList<>();
        mSwitchButtons = new LinkedList<>();

        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < 6; i++) {
            if (i < 4) {
                Button moveButton = new Button(context);
                moveButton.setTextColor(Color.WHITE);
                moveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                moveButton.setPadding(Utils.dpToPx(6), Utils.dpToPx(6), Utils.dpToPx(6), Utils.dpToPx(6));
                moveButton.setAllCaps(false);
                addView(moveButton);
                mMoveButtons.add(moveButton);
                moveButton.setOnClickListener(this);
            }

            SwitchButton switchButton = (SwitchButton) inflater.inflate(R.layout.button_switch, this, false);
            addView(switchButton);
            mSwitchButtons.add(switchButton);
            switchButton.setOnClickListener(this);
        }
        mMovesCheckBox = new CheckBox(context);
        ColorStateList colorStateList = new ColorStateList(
                new int[][] {
                        new int[] { -android.R.attr.state_checked },
                        new int[] {  android.R.attr.state_checked }
                },
                new int[] {Color.GRAY, Color.DKGRAY}
        );
        mMovesCheckBox.setButtonTintList(colorStateList);
        addView(mMovesCheckBox, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void setOnRevealListener(OnRevealListener onRevealListener) {
        mOnRevealListener = onRevealListener;
    }


    void setContentAlpha(float alpha) {
        mContentAlpha = alpha;
        mPaint.setAlpha((int) (255 * alpha));
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++)
            getChildAt(i).setAlpha(alpha);

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Handle and enforce Spec modes
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = getPaddingTop() + getPaddingBottom();
        int availableWidth = measuredWidth - getPaddingStart() - getPaddingEnd();
        int N = mMoveButtons.size();
        if (N > 0) {
            int childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth / N, MeasureSpec.EXACTLY);
            int maxChildHeight = 0;
            boolean noVisibleChild = true;
            for (int i = 0; i < N; i++) {
                View child = mMoveButtons.get(i);
                child.measure(childWidthSpec, heightMeasureSpec);
                if (child.getMeasuredHeight() > maxChildHeight)
                    maxChildHeight = child.getMeasuredHeight();

                if (child.getVisibility() == VISIBLE)
                    noVisibleChild = false;
            }
            int childHeightSpec = MeasureSpec.makeMeasureSpec(maxChildHeight, MeasureSpec.EXACTLY);
            for (int i = 0; i < N; i++) {
                mMoveButtons.get(i).measure(childWidthSpec, childHeightSpec);
            }

            if (!noVisibleChild)
                measuredHeight += maxChildHeight;
        }
        N = mSwitchButtons.size();
        if (N > 0) {
            int childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth / 3, MeasureSpec.EXACTLY);

            boolean noVisibleChild = true;
            for (int i = 0; i < N; i++) {
                View child = mSwitchButtons.get(i);
                child.measure(childWidthSpec, heightMeasureSpec);
                if (child.getVisibility() == VISIBLE)
                    noVisibleChild = false;
            }
            if (!noVisibleChild) {
                measuredHeight += mSwitchButtons.get(0).getMeasuredHeight();
                if (N > 3)
                    measuredHeight += mSwitchButtons.get(3).getMeasuredHeight();
            }
        }

        int childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST);
        mMovesCheckBox.measure(childWidthSpec, heightMeasureSpec);
        if (mMovesCheckBox.getVisibility() == VISIBLE)
            measuredHeight += mMovesCheckBox.getMeasuredHeight();

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int paddingStart = getPaddingStart();
        int paddingTop = getPaddingTop();
        int yOffset = paddingTop;

        int N = mMoveButtons.size();
        int childWidth = width / N;
        int childHeight = 0;
        for (int i = 0; i < N; i++) {
            View child = mMoveButtons.get(i);
            if (child.getVisibility() == GONE)
                continue;
            childHeight = child.getMeasuredHeight();
            child.layout(paddingStart + i * childWidth, yOffset,
                    paddingStart + (i + 1) * childWidth, yOffset + childHeight);
        }

        yOffset += childHeight;

        if (mMovesCheckBox.getVisibility() != GONE) {
            int checkBoxWidth = mMovesCheckBox.getMeasuredWidth();
            int checkBoxHeight = mMovesCheckBox.getMeasuredHeight();
            mMovesCheckBox.layout(width / 2 - checkBoxWidth / 2, yOffset,
                    width / 2 + checkBoxWidth / 2, yOffset + checkBoxHeight);
            yOffset += checkBoxHeight;
        }

        N = mSwitchButtons.size();

        childWidth = width / (N / 2);
        for (int i = 0; i < 3; i++) {
            View child = mSwitchButtons.get(i);
            if (child.getVisibility() == GONE)
                continue;
            childHeight = child.getMeasuredHeight();
            child.layout(paddingStart + i * childWidth, yOffset,
                    paddingStart + (i + 1) * childWidth, yOffset + childHeight);
        }
        yOffset += childHeight;
        int j;
        for (int i = 3; i < N; i++) {
            View child = mSwitchButtons.get(i);
            childHeight = child.getMeasuredHeight();
            j = i - 3;
            child.layout(paddingStart + j * childWidth, yOffset,
                    paddingStart + (j + 1) * childWidth, yOffset + childHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mMoveButtons.size() > 0) {
            int offset = mMovesCheckBox.getVisibility() != GONE ? mMovesCheckBox.getHeight() : 0;
            View child = mMoveButtons.get(0);
            if (child.getVisibility() != GONE)
                canvas.drawLine(0, offset + child.getHeight(), getWidth(), offset + child.getHeight(), mPaint);
        }
    }

    public void updateDexIcons(Drawable[] dexIcons) {
        for (int i = 0; i < dexIcons.length; i++) {
            SwitchButton switchButton = mSwitchButtons.get(i);
            switchButton.setDexIcon(dexIcons[i]);
        }
    }

    public void updateMoveExtras(Move.ExtraInfo[] extraInfos) {
        for (int i = 0; i < extraInfos.length; i++) {
            Button button = mMoveButtons.get(i);
            Move move = (Move) button.getTag(R.id.battle_data_tag);
            Move.ExtraInfo extraInfo = extraInfos[i];

            button.getBackground().setColorFilter(extraInfo.color, PorterDuff.Mode.MULTIPLY);
            move.extraInfo = extraInfo;
        }
    }

    public void promptChoice(BattleTipPopup battleTipPopup, List<Move> moves, boolean canMega,
                 List<SidePokemon> team, boolean chooseLead, OnChoiceListener listener) {
        boolean canZMove = false;
        for (int i = 0; i < 4; i++) {
            Button button = mMoveButtons.get(i);
            if (moves != null && i < moves.size()) {
                Move move = moves.get(i);
                button.setText(sp(move));
                setMoveButtonEnabled(button, !move.disabled);
                button.getBackground().clearColorFilter();
                button.setVisibility(VISIBLE);
                button.setTag(R.id.battle_data_tag, move);
                battleTipPopup.addTippedView(button);
                if (move.canZMove()) canZMove = true;
            } else {
                button.setText(null);
                button.setVisibility(GONE);
            }
        }

        if (canMega) {
            mMovesCheckBox.setVisibility(VISIBLE);
            mMovesCheckBox.setText("Mega Evolution");
            mMovesCheckBox.setChecked(false);
            mMovesCheckBox.setOnCheckedChangeListener(null);
        } else if (canZMove) {
            mMovesCheckBox.setVisibility(VISIBLE);
            mMovesCheckBox.setText("Z-Move");
            mMovesCheckBox.setChecked(false);
            mMovesCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    toggleZMoves(checked);
                }
            });
        } else {
            mMovesCheckBox.setVisibility(GONE);
            mMovesCheckBox.setText(null);
            mMovesCheckBox.setOnCheckedChangeListener(null);
        }

        for (int i = 0; i < 6; i++) {
            SwitchButton button = mSwitchButtons.get(i);
            if (team != null && i < team.size()) {
                SidePokemon sidePokemon = team.get(i);
                button.setVisibility(VISIBLE);
                button.setPokemonName(sidePokemon.name);
                boolean enabled = chooseLead || (i != 0 && sidePokemon.condition.health != 0f);
                button.setEnabled(enabled);
                button.setTag(R.id.battle_data_tag, sidePokemon);
                battleTipPopup.addTippedView(button);
            } else {
                button.setPokemonName(null);
                button.setDexIcon(null);
                button.setVisibility(GONE);
            }
        }

        mOnChoiceListener = listener;
        revealIn();
    }

    private void toggleZMoves(boolean toggle) {
        for (int i = 0; i < 4; i++) {
            Button button = mMoveButtons.get(i);
            if (button.getVisibility() == GONE) continue;
            Move move = (Move) button.getTag(R.id.battle_data_tag);
            if (toggle) {
                if (move.canZMove()) {
                    button.setText(move.zName);
                } else {
                    button.setText("—");
                    setMoveButtonEnabled(button, false);
                }
            } else {
                button.setText(sp(move));
                setMoveButtonEnabled(button, true);
            }
           // battleTipPopup.addTippedView(button);

        }
    }

    private void setMoveButtonEnabled(Button button, boolean enabled) {
        if (enabled) {
            button.setEnabled(true);
            button.setTextColor(Color.WHITE);
        } else {
            button.setEnabled(false);
            button.setTextColor(Color.GRAY);
        }
    }

    private Spanned sp(Move move) {
        String ppText = "\n" + move.pp + "/" + move.ppMax;
        SpannableString spannableString = new SpannableString(move.name + ppText);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), move.name.length() + 1,
                spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    @Override
    public void onClick(View view) {
        if (mOnChoiceListener == null)
            return;

        Object data = view.getTag(R.id.battle_data_tag);
        if (data instanceof Move) {
            Move move = (Move) data;
            int which = move.index + 1;
            boolean mega = mMovesCheckBox.getVisibility() == VISIBLE && mMovesCheckBox.isChecked();
            boolean zmove = mega && move.canZMove();
            if (zmove) mega = false;
            mOnChoiceListener.onMoveChose(which, mega, zmove);
        } else if (data instanceof SidePokemon) {
            int who = ((SidePokemon) data).index + 1;
            mOnChoiceListener.onSwitchChose(who);
        }
        mOnChoiceListener = null;
        revealOut();
    }

    private void revealIn() {
        if (mRevealingIn || getVisibility() == VISIBLE)
            return;

        if (mRevealingOut)
            mCurrentReveal.cancel();

        int viewDiagonal = (int) Math.hypot(getWidth(), getHeight());
        ViewAnimationUtils.createCircularReveal(this, 0, 0, 0, viewDiagonal);
        mCurrentReveal = ViewAnimationUtils.createCircularReveal(this, 0, 0, 0, viewDiagonal);
        mCurrentReveal.setStartDelay(0);
        mCurrentReveal.setDuration(ANIM_REVEAL_DURATION);
        mCurrentReveal.setInterpolator(new AccelerateInterpolator());
        mCurrentReveal.removeAllListeners();
        mCurrentReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mRevealingIn = true;
                setVisibility(VISIBLE);
                setContentAlpha(0f);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mRevealingIn = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mRevealingIn = false;
            }
        });
        mCurrentReveal.start();
        mContentAlphaAnimator.setFloatValues(0f, 1f);
        mContentAlphaAnimator.setDuration(ANIM_FADE_DURATION);
        mContentAlphaAnimator.setStartDelay(ANIM_REVEAL_DURATION);
        mContentAlphaAnimator.start();

        if (mOnRevealListener != null)
            mOnRevealListener.onReveal(true);
    }

    private void revealOut() {
        if (mRevealingOut || getVisibility() == GONE)
            return;

        if (mRevealingIn)
            mCurrentReveal.cancel();

        int viewDiagonal = (int) Math.hypot(getWidth(), getHeight());
        mCurrentReveal = ViewAnimationUtils.createCircularReveal(this, 0, 0, viewDiagonal, 0);
        mCurrentReveal.setStartDelay(ANIM_FADE_DURATION);
        mCurrentReveal.setDuration(ANIM_REVEAL_DURATION);
        mCurrentReveal.setInterpolator(new AccelerateInterpolator());
        mCurrentReveal.removeAllListeners();
        mCurrentReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mRevealingOut = false;
                setVisibility(GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mRevealingOut = false;
            }
        });
        mCurrentReveal.start();
        mContentAlphaAnimator.setFloatValues(1f, 0f);
        mContentAlphaAnimator.setDuration(ANIM_FADE_DURATION);
        mContentAlphaAnimator.setStartDelay(0);
        mContentAlphaAnimator.start();

        // Setting the flag directly because reveal anim waits for fade anim to finish before running
        mRevealingOut = true;

        if (mOnRevealListener != null)
            mOnRevealListener.onReveal(false);
    }

    public void dismiss() {
        revealOut();
    }

    public void dismissNow() {
        setVisibility(GONE);
        setContentAlpha(0f);
        if (mCurrentReveal != null)
            mCurrentReveal.cancel();
    }

    public interface OnChoiceListener {
        public void onMoveChose(int which, boolean mega, boolean zmove);

        public void onSwitchChose(int who);
    }
}
