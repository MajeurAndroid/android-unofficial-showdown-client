package com.majeur.psclient.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.majeur.psclient.R;
import com.majeur.psclient.model.Move;

public class MoveButton extends RelativeLayout {

    private TextView mNameView;
    private TextView mPPView;
    private TextView mTypeView;

    public MoveButton(Context context) {
        super(context, null, R.style.Widget_MaterialComponents_Button);
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.background, outValue, true);
        //setBackgroundResource(R.drawable.actionbar_background);
        setBackground(new Button(context).getBackground());
        //?attr/drawablePrimaryButtonBackground
        setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        mNameView = new TextView(context);
        mNameView.setId(R.id.button_battle);
        mNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mNameView.setTextColor(Color.WHITE);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addView(mNameView, layoutParams);

        mPPView = new TextView(context);
        mPPView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        mPPView.setTextColor(Color.WHITE);
        layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(BELOW, R.id.button_battle);
        layoutParams.addRule(ALIGN_PARENT_END);
        addView(mPPView, layoutParams);

        mTypeView = new TextView(context);
        mTypeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        mTypeView.setTypeface(mTypeView.getTypeface(), Typeface.ITALIC);
        mTypeView.setTextColor(Color.WHITE);
        layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(BELOW, R.id.button_battle);
        layoutParams.addRule(ALIGN_PARENT_START);
        addView(mTypeView, layoutParams);
    }

    public void setMove(Move move) {
        if (move != null) {
            mNameView.setText(move.name.toUpperCase());
            mPPView.setText(move.pp + "/" + move.ppMax);
            setEnabled(!move.disabled);
        } else {
            mNameView.setText(null);
            mPPView.setText(null);
            mTypeView.setText(null);
            getBackground().clearColorFilter();
            setEnabled(false);
        }
    }

    public void setExtraInfo(final Move.ExtraInfo extraInfo) {
        if (extraInfo != null) {
            mTypeView.setText(extraInfo.type);
            getBackground().setColorFilter(extraInfo.color, PorterDuff.Mode.MULTIPLY);
            setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Toast.makeText(getContext(), extraInfo.desc, 0).show();
                    return true;
                }
            });
        } else {
            mTypeView.setText(null);
            getBackground().clearColorFilter();
            setOnLongClickListener(null);
        }
    }
}
