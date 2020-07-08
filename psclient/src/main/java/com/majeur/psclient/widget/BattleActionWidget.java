package com.majeur.psclient.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import com.majeur.psclient.R;
import com.majeur.psclient.model.battle.BattleActionRequest;
import com.majeur.psclient.model.battle.BattleDecision;
import com.majeur.psclient.model.battle.Move;
import com.majeur.psclient.model.battle.Player;
import com.majeur.psclient.model.battle.PokemonId;
import com.majeur.psclient.model.pokemon.BattlingPokemon;
import com.majeur.psclient.model.pokemon.SidePokemon;
import com.majeur.psclient.service.observer.BattleRoomMessageObserver;
import com.majeur.psclient.util.SimpleAnimatorListener;
import com.majeur.psclient.util.Utils;

import java.util.LinkedList;
import java.util.List;

import static com.majeur.psclient.util.ExtensionsKt.toId;
import static com.majeur.psclient.util.Utils.addNullSafe;

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
        void onReveal(boolean in);
    }

    private static final long ANIM_REVEAL_DURATION = 250;
    private static final long ANIM_REVEAL_FADE_DURATION = 100;
    private static final long ANIM_NEXTCHOICE_FADE_DURATION = 225;

    private float mContentAlpha;
    private final Paint mPaint;
    private final ObjectAnimator mContentAlphaAnimator;
    private final List<Button> mMoveButtons;
    private final CheckBox mMovesCheckBox;
    private final List<SwitchButton> mSwitchButtons;

    private BattleRoomMessageObserver mObserver;
    private int mCurrentPrompt;
    private BattleTipPopup mBattleTipPopup;
    private BattleActionRequest mRequest;
    private BattleDecision mDecision;
    private Move.Target mTargetToChoose;

    private OnDecisionListener mOnDecisionListener;

    private Animator mCurrentReveal;
    private boolean mRevealingIn;
    private boolean mRevealingOut;
    private boolean mIsAnimatingContentAlpha;
    private OnRevealListener mOnRevealListener;

    public BattleActionWidget(Context context) {
        this(context, null);
    }

    public BattleActionWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BattleActionWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        mPaint = new Paint();
        mPaint.setColor(ContextCompat.getColor(context, R.color.divider));
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

    @SuppressWarnings("UnnecessaryLocalVariable")
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

    public void notifyDexIconsUpdated() {
        for (int i = 0; i < 6; i++) {
            SwitchButton switchButton = mSwitchButtons.get(i);
            Object tag = switchButton.getTag(R.id.battle_data_tag);
            if (tag instanceof SidePokemon) {
                Bitmap icon = ((SidePokemon) tag).getIcon();
                if (icon != null) switchButton.setDexIcon(new BitmapDrawable(getResources(), icon));
            }
        }
    }

    public void notifyDetailsUpdated() {
        for (Button button : mMoveButtons) {
            Move move = (Move) button.getTag(R.id.battle_data_tag);
            if (move == null || move.getMaxflag() || move.getDetails() == null) continue;
            button.getBackground().setColorFilter(
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            move.getDetails().getColor(), BlendModeCompat.MODULATE)
            );
        }
    }

    public void notifyMaxDetailsUpdated() {
        for (Button button : mMoveButtons) {
            Move move = (Move) button.getTag(R.id.battle_data_tag);
            if (move == null || !move.getMaxflag() || move.getMaxDetails() == null) continue;
            button.setText(move.getMaxDetails().name);
            button.getBackground().setColorFilter(
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            move.getMaxDetails().getColor(), BlendModeCompat.MODULATE)
            );
        }
    }

    public void promptDecision(BattleRoomMessageObserver observer, BattleTipPopup battleTipPopup, BattleActionRequest request,
                               OnDecisionListener listener) {
        mObserver = observer;
        mBattleTipPopup = battleTipPopup;
        mRequest = request;
        mCurrentPrompt = -1;
        mOnDecisionListener = listener;
        mDecision = new BattleDecision();
        mTargetToChoose = null;
        promptNext();
        revealIn();
    }

    private void promptNext() {
        if (mTargetToChoose != null) { // Needs target selection
            List<BattlingPokemon> targets = new LinkedList<>();
            List<BattlingPokemon> foeTargets = new LinkedList<>();
            for (int i = 0; i < mRequest.getCount(); i++) {
                addNullSafe(targets, mObserver.getBattlingPokemon(new PokemonId(Player.TRAINER, i)));
                addNullSafe(foeTargets, mObserver.getBattlingPokemon(new PokemonId(Player.FOE, i)));
            }
            boolean[][] b = Move.Target.computeTargetAvailabilities(mTargetToChoose, mCurrentPrompt, mRequest.getCount());
            showTargetChoice(mBattleTipPopup, targets, foeTargets, b);
        } else if (mRequest.getTeamPreview() ? mCurrentPrompt == 0 : mCurrentPrompt + 1 >= mRequest.getCount()) { // Request completed
            mOnDecisionListener.onDecisionTook(mDecision);
            revealOut();
            mObserver = null;
            mCurrentPrompt = 0;
            mBattleTipPopup = null;
            mRequest = null;
            mDecision = null;
            mTargetToChoose = null;
        } else {
            mCurrentPrompt += 1;
            if (!mRequest.getTeamPreview()) {
                boolean activeFainted = mRequest.getSide().get(mCurrentPrompt).getCondition().getHealth() == 0f;
                int unfaintedCount = 0;
                for (int i = mRequest.getCount(); i < mRequest.getSide().size(); i++)
                    if (mRequest.getSide().get(i).getCondition().getHealth() != 0f) unfaintedCount++;
                int switchChoicesCount = mDecision.switchChoicesCount();
                boolean pass = activeFainted && (unfaintedCount - switchChoicesCount) <= 0;
                if (mRequest.shouldPass(mCurrentPrompt) || pass) {
                    mDecision.addPassChoice();
                    promptNext();
                    return;
                }
            }
            boolean hideMoves = mRequest.forceSwitch(mCurrentPrompt) || mRequest.getTeamPreview();
            boolean hideSwitch = mRequest.trapped(mCurrentPrompt);
            final Move[] moves = hideMoves ? null : mRequest.getMoves(mCurrentPrompt);
            List<SidePokemon> team = hideSwitch ? null : mRequest.getSide();
            showChoice(mBattleTipPopup,
                    moves,
                    mRequest.canMegaEvo(mCurrentPrompt),
                    mRequest.canDynamax(mCurrentPrompt),
                    mRequest.isDynamaxed(mCurrentPrompt),
                    team,
                    mRequest.getTeamPreview());
        }
    }

    private void showTargetChoice(final BattleTipPopup battleTipPopup, final List<BattlingPokemon> trainerTargets,
                                  final List<BattlingPokemon> foeTargets, final boolean[][] availabilities) {
        mContentAlphaAnimator.setFloatValues(1f, 0f);
        mContentAlphaAnimator.setDuration(ANIM_NEXTCHOICE_FADE_DURATION);
        mContentAlphaAnimator.setStartDelay(0);
        mContentAlphaAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        mContentAlphaAnimator.setRepeatCount(1);
        mContentAlphaAnimator.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mIsAnimatingContentAlpha = true;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                setTargetChoiceLayout(battleTipPopup, trainerTargets, foeTargets, availabilities);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mIsAnimatingContentAlpha = false;
                mContentAlphaAnimator.removeListener(this);
            }
        });
        mContentAlphaAnimator.start();
    }

    private void setTargetChoiceLayout(BattleTipPopup battleTipPopup, List<BattlingPokemon> trainerTargets,
                                       List<BattlingPokemon> foeTargets, boolean[][] availabilities) {
        for (int i = 0; i < 4; i++) {
            Button button = mMoveButtons.get(i);
            button.setText(null);
            button.setVisibility(GONE);
        }
        mMovesCheckBox.setVisibility(GONE);
        mMovesCheckBox.setText(null);
        mMovesCheckBox.setOnCheckedChangeListener(null);

        for (int i = 0; i < 6; i++) {
            SwitchButton button = mSwitchButtons.get(i);
            List<BattlingPokemon> targets = i < 3 ? foeTargets : trainerTargets;
            int offset = i < 3 ? 0 : 3;
            if (targets != null && i - offset < targets.size()) {
                BattlingPokemon pokemon = targets.get(i - offset);
                button.setVisibility(VISIBLE);
                button.setPokemonName(pokemon.getName());
                boolean enabled = availabilities[i < 3 ? 0 : 1][i - offset] && pokemon.getCondition().getHealth() != 0f;
                button.setEnabled(enabled);
                button.setTag(R.id.battle_data_tag, pokemon);
                battleTipPopup.removeTippedView(button);
                button.setDexIcon(null);
            } else {
                button.setPokemonName(null);
                button.setVisibility(GONE);
            }
        }
    }

    private void showChoice(final BattleTipPopup battleTipPopup, final Move[] moves, final boolean canMega,
                            final boolean canDynamax, final boolean isDynamaxed, final List<SidePokemon> team,
                            final boolean chooseLead) {
        if (mCurrentPrompt == 0 || mDecision.hasOnlyPassChoice()) {
            setChoiceLayout(battleTipPopup, moves, canMega, canDynamax, isDynamaxed, team, chooseLead);
            return;
        }
        mContentAlphaAnimator.setFloatValues(1f, 0f);
        mContentAlphaAnimator.setDuration(ANIM_NEXTCHOICE_FADE_DURATION);
        mContentAlphaAnimator.setStartDelay(0);
        mContentAlphaAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        mContentAlphaAnimator.setRepeatCount(1);
        mContentAlphaAnimator.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mIsAnimatingContentAlpha = true;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                setChoiceLayout(battleTipPopup, moves, canMega, canDynamax, isDynamaxed, team, chooseLead);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mIsAnimatingContentAlpha = false;
                mContentAlphaAnimator.removeListener(this);
            }
        });
        mContentAlphaAnimator.start();
    }

    private void setChoiceLayout(BattleTipPopup battleTipPopup, Move[] moves, boolean canMega, boolean canDynamax,
                                 boolean isDynamaxed, List<SidePokemon> team, boolean chooseLead) {
        boolean canZMove = false;
        for (int i = 0; i < 4; i++) {
            Button button = mMoveButtons.get(i);
            if (moves != null && i < moves.length) {
                Move move = moves[i];
                button.setText(sp(move));
                setMoveButtonEnabled(button, !move.getDisabled());
                button.getBackground().clearColorFilter();
                button.setVisibility(VISIBLE);
                button.setTag(R.id.battle_data_tag, move);
                battleTipPopup.addTippedView(button);
                if (move.getCanZMove()) canZMove = true;
                if (move.getDetails() != null)
                    button.getBackground().setColorFilter(
                        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                                move.getDetails().getColor(), BlendModeCompat.MODULATE)
                    );
            } else {
                button.setText(null);
                button.setVisibility(GONE);
            }
        }

        if (isDynamaxed) toggleMaxMoves(true);

        if (canMega) {
            mMovesCheckBox.setVisibility(VISIBLE);
            mMovesCheckBox.setText("Mega Evolution");
            mMovesCheckBox.setChecked(false);
            mMovesCheckBox.setOnCheckedChangeListener(null);
        } else if (canZMove) {
            mMovesCheckBox.setVisibility(VISIBLE);
            mMovesCheckBox.setText("Z-Move");
            mMovesCheckBox.setChecked(false);
            mMovesCheckBox.setOnCheckedChangeListener((compoundButton, checked) -> toggleZMoves(checked));
        } else if (canDynamax) {
            mMovesCheckBox.setVisibility(VISIBLE);
            mMovesCheckBox.setText("Dynamax");
            mMovesCheckBox.setChecked(false);
            mMovesCheckBox.setOnCheckedChangeListener((compoundButton, checked) -> toggleMaxMoves(checked));
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
                button.setPokemonName(sidePokemon.getName());
                boolean enabled = chooseLead || (i >= mRequest.getCount() && sidePokemon.getCondition().getHealth() != 0f);
                if (mCurrentPrompt > 0 && mRequest.getCount() > 1)
                    if (mDecision.hasSwitchChoice(i+1)) enabled = false;
                button.setEnabled(enabled);
                button.setTag(R.id.battle_data_tag, sidePokemon);
                battleTipPopup.addTippedView(button);
                if (sidePokemon.getIcon() != null)
                    button.setDexIcon(new BitmapDrawable(getResources(), sidePokemon.getIcon()));
            } else {
                button.setPokemonName(null);
                button.setDexIcon(null);
                button.setVisibility(GONE);
            }
        }
    }

    private void toggleZMoves(boolean toggle) {
        for (int i = 0; i < 4; i++) {
            Button button = mMoveButtons.get(i);
            if (button.getVisibility() == GONE) continue;
            Move move = (Move) button.getTag(R.id.battle_data_tag);
            if (toggle) {
                if (move.getCanZMove()) {
                    button.setText(move.getZName());
                    move.setZflag(true);
                } else {
                    button.setText("—");
                    setMoveButtonEnabled(button, false);
                }
            } else {
                button.setText(sp(move));
                setMoveButtonEnabled(button, true);
                move.setZflag(false);
            }
           // battleTipPopup.addTippedView(button);

        }
    }

    private void toggleMaxMoves(boolean toggle) {
        for (int i = 0; i < 4; i++) {
            Button button = mMoveButtons.get(i);
            if (button.getVisibility() == GONE) continue;
            Move move = (Move) button.getTag(R.id.battle_data_tag);
            if (toggle) {
                if (move.getMaxMoveId() != null) {
                    String text = move.getMaxDetails() != null ? move.getMaxDetails().name : move.getMaxMoveId();
                    button.setText(text);
                    if (move.getMaxDetails() != null)
                        button.getBackground().setColorFilter(
                                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                                        move.getMaxDetails().getColor(), BlendModeCompat.MODULATE)
                        );
                    else button.getBackground().clearColorFilter();
                    move.setMaxflag(true);
                    setMoveButtonEnabled(button, true);
                } else {
                    button.setText("—");
                    setMoveButtonEnabled(button, false);
                }
            } else {
                button.setText(sp(move));
                if (move.getDetails() != null)
                    button.getBackground().setColorFilter(
                            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                                    move.getDetails().getColor(), BlendModeCompat.MODULATE)
                    );
                else button.getBackground().clearColorFilter();
                setMoveButtonEnabled(button, !move.getDisabled());
                move.setMaxflag(false);
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
        String ppText = "\n" + move.getPp() + "/" + move.getPpMax();
        SpannableString spannableString = new SpannableString(move.getName() + ppText);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), move.getName().length() + 1,
                spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    @Override
    public void onClick(View view) {
        if (mRevealingIn || mRevealingOut || mIsAnimatingContentAlpha) return;
        Object data = view.getTag(R.id.battle_data_tag);
        if (data instanceof Move) {
            Move move = (Move) data;
            int which = move.getIndex() + 1;
            boolean mega = mMovesCheckBox.getVisibility() == VISIBLE && mMovesCheckBox.isChecked();
            boolean zmove = mega && move.getZflag();
            boolean dynamax = mega && move.getMaxflag();
            if (dynamax) mega = zmove = false;
            else if (zmove) mega = false;
            mDecision.addMoveChoice(which, mega, zmove, dynamax);
            if (mRequest.getCount() > 1 && move.getTarget().isChoosable())
                mTargetToChoose = move.getTarget();
        } else if (data instanceof BattlingPokemon) {
            PokemonId id = ((BattlingPokemon) data).getId();
            int index = id.getPosition() + 1;
            if (!id.getFoe()) index *= -1;
            mDecision.setLastMoveTarget(index);
            mTargetToChoose = null;
        } else if (data instanceof SidePokemon) {
            int who = ((SidePokemon) data).getIndex() + 1;
            if (mRequest.getTeamPreview()) {
                mDecision.addLeadChoice(who, mRequest.getSide().size());
                view.setEnabled(false);
                // Request full team order if one of our Pokémon has Illusion
                boolean fullTeamOrder = false;
                for (SidePokemon pokemon : mRequest.getSide()) {
                    if (toId(pokemon.getBaseAbility() == null ? "" : pokemon.getBaseAbility()).equals("illusion")) {
                        fullTeamOrder = true;
                        break;
                    }
                }
                if (mDecision.leadChoicesCount() < (fullTeamOrder ? mRequest.getSide().size() : mRequest.getCount()))
                    return; // Avoid going to next prompt;
            } else {
                mDecision.addSwitchChoice(who);
            }
        }
        promptNext();
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
        mContentAlphaAnimator.setDuration(ANIM_REVEAL_FADE_DURATION);
        mContentAlphaAnimator.setStartDelay(ANIM_REVEAL_DURATION);
        mContentAlphaAnimator.setRepeatCount(0);
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
        mCurrentReveal.setStartDelay(ANIM_REVEAL_FADE_DURATION);
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
        mContentAlphaAnimator.setDuration(ANIM_REVEAL_FADE_DURATION);
        mContentAlphaAnimator.setStartDelay(0);
        mContentAlphaAnimator.setRepeatCount(0);
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

    public interface OnDecisionListener {
        void onDecisionTook(BattleDecision decision);
    }
}
