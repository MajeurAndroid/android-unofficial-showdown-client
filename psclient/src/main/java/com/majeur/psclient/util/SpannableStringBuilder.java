package com.majeur.psclient.util;

public class SpannableStringBuilder extends android.text.SpannableStringBuilder {

    private char[] mTempChars;

    public SpannableStringBuilder() {
        super();
    }

    public SpannableStringBuilder(CharSequence text) {
        super(text);
    }

    public SpannableStringBuilder(CharSequence text, int start, int end) {
        super(text, start, end);
    }

    public String substring(int startIndex) {
        return substring(startIndex, length());
    }

    public String substring(int startIndex, int endIndex) {
        if (mTempChars == null || mTempChars.length < (endIndex - startIndex))
            mTempChars = new char[endIndex - startIndex];
        getChars(startIndex, endIndex, mTempChars, 0);
        return String.copyValueOf(mTempChars, 0, endIndex-startIndex);
    }


    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    public int indexOf(String str, int fromIndex) {
        if (mTempChars == null || mTempChars.length != length())
            mTempChars = new char[length()];
        getChars(0, length(), mTempChars, 0);
        return indexOf(mTempChars, 0, mTempChars.length,
                str.toCharArray(), 0, str.length(), fromIndex);
    }

    /**
     * From java.lang.String source code.
     */
    private int indexOf(char[] source, int sourceOffset, int sourceCount,
                       char[] target, int targetOffset, int targetCount,
                       int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        char first = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (source[i] != first) {
                while (++i <= max && source[i] != first);
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j]
                        == target[k]; j++, k++);

                if (j == end) {
                    /* Found whole string. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
}
