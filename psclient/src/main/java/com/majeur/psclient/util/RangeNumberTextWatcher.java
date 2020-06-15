package com.majeur.psclient.util;

import android.text.Editable;

import static com.majeur.psclient.util.Utils.parseInt;

public class RangeNumberTextWatcher extends SimpleTextWatcher {

    private final int mMin, mMax;

    public RangeNumberTextWatcher(int min, int max) {
        mMin = min;
        mMax = max;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (editable.length() == 0) return;
        Integer integer = parseInt(editable.toString());
        if (integer == null || integer < mMin || integer > mMax) {
            editable.clear();
            editable.append(Integer.toString(mMax));
        }
    }

}
