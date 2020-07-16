package com.majeur.psclient.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.majeur.psclient.R;

public class SwitchButton extends LinearLayout {

    private TextView mNameView;
    private ImageView mIconView;

    public SwitchButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child.getId() == R.id.name_view)
            mNameView = (TextView) child;
        else if (child.getId() == R.id.dex_icon_view)
            mIconView = (ImageView) child;
        else
            throw new IllegalStateException("SwitchButton is missing its two children in XML declaration");
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mNameView.setEnabled(enabled);
        mIconView.setAlpha(enabled ? 1f : 0.65f);
    }

    public void setDexIcon(Drawable dexIcon) {
        mIconView.setImageDrawable(dexIcon);
    }

    public void setPokemonName(String name) {
        mNameView.setText(name);
    }
}
