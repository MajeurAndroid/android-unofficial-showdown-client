package com.majeur.psclient.widget;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import androidx.appcompat.widget.AppCompatTextView;
import com.majeur.psclient.R;
import com.majeur.psclient.model.pokemon.BasePokemon;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.majeur.psclient.model.Id.toId;

public class PlayerInfoView extends AppCompatTextView {

    private static final String SUFFIX_PATTERN = "  ------  ";
    private static final int SUFFIX_OFFSET = 2;
    private static final int MAX_TEAM_SIZE = 6;

    private int mDexIconSize;
    private SpannableStringBuilder mSpannableBuilder;
    private Drawable mPokeballDrawable;
    private Drawable mEmptyPokeballDrawable;

    private Set<String> mPokemonIds;

    public PlayerInfoView(Context context) {
        this(context, null);
    }

    public PlayerInfoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPokemonIds = new LinkedHashSet<>();
        mSpannableBuilder = new SpannableStringBuilder(SUFFIX_PATTERN);
        mDexIconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        mPokeballDrawable = getResources().getDrawable(R.drawable.ic_team_poke);
        mPokeballDrawable.setBounds(0, 0, size, size);
        mEmptyPokeballDrawable = getResources().getDrawable(R.drawable.ic_team_poke_empty);
        mEmptyPokeballDrawable.setBounds(0, 0, size, size);
    }

    public void clear() {
        mPokemonIds.clear();
        mSpannableBuilder.clear();
        mSpannableBuilder.clearSpans();
        mSpannableBuilder.append(SUFFIX_PATTERN);
        setText(null);
    }

    public void setUsername(String username) {
        int k = mSpannableBuilder.length() - SUFFIX_PATTERN.length();
        int start;
        if ((getGravity() & Gravity.END) == Gravity.END) {
            if (k != 0) {
                start = SUFFIX_PATTERN.length() - 1;
                mSpannableBuilder.replace(start, mSpannableBuilder.length(), username);
            } else {
                mSpannableBuilder.append(username);
                start = mSpannableBuilder.length() - username.length();
            }
        } else {
            if (k != 0) {
                mSpannableBuilder.replace(0, k, username);
                start = 0;
            } else {
                mSpannableBuilder.insert(0, username);
                start = 0;
            }
        }

        StyleSpan[] potentialStyleSpans = mSpannableBuilder.getSpans(0, mSpannableBuilder.length(), StyleSpan.class);
        if (potentialStyleSpans != null && potentialStyleSpans.length > 0)
            mSpannableBuilder.removeSpan(potentialStyleSpans[0]);
        mSpannableBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, start + username.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        invalidateText();
    }

    public void setTeamSize(int teamSize) {
        mPokemonIds.clear();
        int l = mSpannableBuilder.length();
        for (ImageSpan span : mSpannableBuilder.getSpans(0, l, ImageSpan.class))
            mSpannableBuilder.removeSpan(span);
        int k = mSpannableBuilder.length() - MAX_TEAM_SIZE - SUFFIX_OFFSET;
        if ((getGravity() & Gravity.END) == Gravity.END) {
            for (int i = SUFFIX_OFFSET; i < SUFFIX_OFFSET + teamSize; i++)
                mSpannableBuilder.setSpan(new ImageSpan(mPokeballDrawable), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            for (int i = SUFFIX_OFFSET + teamSize; i < SUFFIX_OFFSET + MAX_TEAM_SIZE; i++)
                mSpannableBuilder.setSpan(new ImageSpan(mEmptyPokeballDrawable), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        } else {
            for (int i = k; i < k + teamSize; i++)
                mSpannableBuilder.setSpan(new ImageSpan(mPokeballDrawable), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            for (int i = k + teamSize; i < l - SUFFIX_OFFSET; i++)
                mSpannableBuilder.setSpan(new ImageSpan(mEmptyPokeballDrawable), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        invalidateText();
    }

    public void appendPokemon(BasePokemon pokemon, Drawable dexIcon) {
        if (!mPokemonIds.add(toId(pokemon.baseSpecies)))
            return;

        int i;
        if ((getGravity() & Gravity.END) == Gravity.END)
            i = SUFFIX_OFFSET + MAX_TEAM_SIZE - mPokemonIds.size();
        else
            i = mSpannableBuilder.length() - MAX_TEAM_SIZE - SUFFIX_OFFSET + mPokemonIds.size() - 1;

        ImageSpan previousSpan = mSpannableBuilder.getSpans(i , i + 1, ImageSpan.class)[0];
        mSpannableBuilder.removeSpan(previousSpan);
        float aspectRatio = dexIcon.getIntrinsicWidth() / (float)  dexIcon.getIntrinsicHeight();
        dexIcon.setBounds(0, 0, Math.round(aspectRatio * mDexIconSize), mDexIconSize);
        mSpannableBuilder.setSpan(new ImageSpan(dexIcon), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        invalidateText();
    }

    public void updatePokemon(BasePokemon pokemon, Drawable dexIcon) {
        if (!mPokemonIds.contains(toId(pokemon.baseSpecies))) {
            appendPokemon(pokemon, dexIcon);
            return;
        }

        int index = 0;
        for (String id : mPokemonIds) {
            if (id.equals(toId(pokemon.baseSpecies))) break;
            index++;
        }

        int i;
        if ((getGravity() & Gravity.END) == Gravity.END)
            i = SUFFIX_OFFSET + MAX_TEAM_SIZE - (index + 1);
        else
            i = mSpannableBuilder.length() - MAX_TEAM_SIZE - SUFFIX_OFFSET + index;

        ImageSpan previousSpan = mSpannableBuilder.getSpans(i , i + 1, ImageSpan.class)[0];
        mSpannableBuilder.removeSpan(previousSpan);
        float aspectRatio = dexIcon.getIntrinsicWidth() / (float)  dexIcon.getIntrinsicHeight();
        dexIcon.setBounds(0, 0, Math.round(aspectRatio * mDexIconSize), mDexIconSize);
        mSpannableBuilder.setSpan(new ImageSpan(dexIcon), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        invalidateText();
    }

    public void setPokemonFainted(BasePokemon pokemon) {
        if (!mPokemonIds.contains(toId(pokemon.baseSpecies)))
            return;

        int index = 0;
        for (String id : mPokemonIds) {
            if (id.equals(toId(pokemon.baseSpecies))) break;
            index++;
        }

        int i;
        if ((getGravity() & Gravity.END) == Gravity.END)
            i = SUFFIX_OFFSET + MAX_TEAM_SIZE - (index + 1);
        else
            i = mSpannableBuilder.length() - MAX_TEAM_SIZE - SUFFIX_OFFSET + index;

        ImageSpan previousSpan = mSpannableBuilder.getSpans(i , i + 1, ImageSpan.class)[0];
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        previousSpan.getDrawable().setColorFilter(filter);
        invalidateText();
    }

    private void invalidateText() {
        setText(mSpannableBuilder);
    }
}
