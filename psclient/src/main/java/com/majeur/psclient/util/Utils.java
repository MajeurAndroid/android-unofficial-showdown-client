package com.majeur.psclient.util;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.ScrollView;
import com.majeur.psclient.model.common.Colors;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Utils {

    private static float sScreenDensity;

    public static int dpToPx(float dp) {
        if (sScreenDensity == 0f) sScreenDensity = Resources.getSystem().getDisplayMetrics().density;
        return (int) (sScreenDensity * dp);
    }

    public static boolean isApi28() {
        return Build.VERSION.SDK_INT >= 28;
    }

    public static boolean fullScrolled(ScrollView scrollView) {
        if (scrollView.getChildCount() == 0) return false;
        View child = scrollView.getChildAt(scrollView.getChildCount() - 1);
        int slop = dpToPx(14);
        int diff = (child.getBottom() - slop - (scrollView.getHeight() + scrollView.getScrollY()));
        return diff <= 0;
    }

    public static int hashColor(String string) {
        String md5 = MD5.hash(string);
        if (md5 != null)
            return md5Color(md5);
        else
            return sinColor(string);
    }

    private static int md5Color(String hash) {
        float H = parseInt(substringlen(hash, 4, 4), 16) % 360f; // 0 to 360
        float S = parseInt(substringlen(hash, 0, 4), 16) % 50f + 40; // 40 to 89
        float L = (float) Math.floor(parseInt(substringlen(hash, 8, 4), 16) % 20f + 30); // 30 to 49
        float[] rgb = hslToRgb(H, S, L);
        float R = rgb[0];
        float G = rgb[1];
        float B = rgb[2];
        float lum = R * R * R * 0.2126f + G * G * G * 0.7152f + B * B * B * 0.0722f; // 0.013 (dark blue) to 0.737 (yellow)
        float HLmod = (lum - 0.2f) * -150; // -80 (yellow) to 28 (dark blue)
        if (HLmod > 18) HLmod = (HLmod - 18) * 2.5f;
        else if (HLmod < 0) HLmod = (HLmod - 0) / 3f;
        else HLmod = 0f;
        // let mod = ';border-right: ' + Math.abs(HLmod) + 'px solid ' + (HLmod > 0 ? 'red' : '#0088FF');
        float Hdist = Math.min(Math.abs(180 - H), Math.abs(240 - H));
        if (Hdist < 15) {
            HLmod += (15 - Hdist) / 3f;
        }
        L += HLmod;
        rgb =hslToRgb(H, S, L);
        R = rgb[0];
        G = rgb[1];
        B = rgb[2];
        return rgb(R, G, B);
    }

    public static float[] hslToRgb(float h, float s, float l){
        float C = (100 - Math.abs(2 * l - 100)) * s / 100 / 100;
        float X = C * (1 - Math.abs((h / 60) % 2 - 1));
        float m = l / 100 - C / 2;
        float R, G, B;
        switch ((int) Math.floor(h / 60)) {
            case 1: R = X; G = C; B = 0; break;
            case 2: R = 0; G = C; B = X; break;
            case 3: R = 0; G = X; B = C; break;
            case 4: R = X; G = 0; B = C; break;
            case 5: R = C; G = 0; B = X; break;
            case 0: default: R = C; G = X; B = 0; break;
        }
        return new float[] {R + m, G + m, B + m};
    }

    private static int sinColor(String username) {
        int sum = 0;
        for (int i = 0; i < username.length(); i++)
            sum += username.charAt(i);

        int r = (int) (Double.parseDouble("0." + Double.toString(Math.sin(sum + 1)).substring(6)) * 256);
        int g = (int) (Double.parseDouble("0." + Double.toString(Math.sin(sum + 2)).substring(6)) * 256);
        int b = (int) (Double.parseDouble("0." + Double.toString(Math.sin(sum + 3)).substring(6)) * 256);

        return Color.rgb(r, g, b);
    }

    public static int alphaColor(int color, float a) {
        return Color.argb((int)(a*255), Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int getTagColor(int textColor) {
        float B = (0.299f * Color.red(textColor) + 0.587f * Color.green(textColor) + 0.114f * Color.blue(textColor)) / 255;
        return (B <= 0.65f) ? 0xFFEDEDED : 0xFF606060;
    }

    /* Compat method for {@link Color.rgb(float, float, float)} */
    public static int rgb(float red, float green, float blue) {
        return 0xff000000 |
                ((int) (red   * 255.0f + 0.5f) << 16) |
                ((int) (green * 255.0f + 0.5f) <<  8) |
                (int) (blue  * 255.0f + 0.5f);
    }

    public static int parseWithDefault(String s, int defaultValue) {
        return s != null && s.matches("-?\\d+") ? Integer.parseInt(s) : defaultValue;
    }

    public static String firstCharUpperCase(String string) {
        char firstChar = string.charAt(0);
        if (Character.isAlphabetic(firstChar) || string.length() < 3)
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        else
            return firstChar + string.substring(1, 2).toUpperCase() + string.substring(2);
    }

    public static String convertStreamToString(InputStream is) {
        try (Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    public static void replace(Editable e, String what, CharSequence with) {
        int startIndex = e.toString().indexOf(what);
        if (startIndex < 0) return;
        e.replace(startIndex, startIndex + what.length(), with);
    }

    public static <T> T[] append(T[] array, T value) {
        array = Arrays.copyOf(array, array.length + 1);
        array[array.length - 1] = value;
        return array;
    }

    public static <T> void swap(T[] arr, int i, int j) {
        T t = arr[i];
        arr[i] = arr[j];
        arr[j] = t;
    }

    public static boolean contains(String string, String... ss) {
        if (string == null) return false;
        for (String s : ss)
            if (string.contains(s)) return true;
        return false;
    }

    public static String concat(String[] strings) {
        StringBuilder builder = new StringBuilder();
        for (String s : strings) builder.append(s);
        return builder.toString();
    }

    public static <T> int indexOf(T value, T[] array) {
        for (int i = 0; i < array.length; i++)
            if (Objects.equals(array[i], value)) return i;
        return -1;
    }

    public static String substring(String string, int startIndex) {
        if (startIndex < 0) startIndex = string.length() + startIndex;
        if (startIndex < 0 || startIndex >= string.length()) return null;
        return string.substring(startIndex);
    }

    public static String substring(String string, int startIndex, int endIndex) {
        if (startIndex < 0) startIndex = string.length() + startIndex;
        if (endIndex < 0) endIndex = string.length() + endIndex;
        return string.substring(startIndex, endIndex);
    }

    public static String substringEnd(String string, int endIndex) {
        if (endIndex < 0) endIndex = string.length() + endIndex;
        if (endIndex < 0) endIndex = 0;
        if (endIndex >= string.length()) return string;
        return string.substring(0, endIndex);
    }

    public static String substringlen(String string, int startIndex, int len) {
        if (startIndex < 0) startIndex = string.length() + startIndex;
        return string.substring(startIndex, startIndex + len);
    }

    public static <T> ArrayList<T> toArrayList(Collection<T> collection) {
        if (collection instanceof ArrayList<?>)
            return (ArrayList<T>) collection;
        ArrayList<T> arrayList = new ArrayList<>(collection.size());
        arrayList.addAll(collection);
        return arrayList;
    }

    public static <T> void addNullSafe(Collection<T> collection, T t) {
        if (t != null) collection.add(t);
    }

    public static <T> T getOobSafe(List<T> list, int index) {
        if (index >= 0 && index < list.size()) return list.get(index);
        return null;
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Spannable boldText(String s) {
        SpannableString spannableString = new SpannableString(s);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, s.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static Spannable boldText(String s, int color) {
        SpannableString spannableString = new SpannableString(s);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, s.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(color), 0, s.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static Spannable italicText(String s) {
        SpannableString spannableString = new SpannableString(s);
        spannableString.setSpan(new StyleSpan(Typeface.ITALIC), 0, s.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static Spannable smallText(CharSequence cs) {
        SpannableString spannableString = new SpannableString(cs);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), 0, cs.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static Spannable coloredText(CharSequence cs, int color) {
        SpannableString spannableString = new SpannableString(cs);
        spannableString.setSpan(new ForegroundColorSpan(color), 0, cs.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static Spannable tagText(String s) {
        SpannableString spannableString = new SpannableString(s);
        spannableString.setSpan(new TextTagSpan(Color.GRAY, Colors.WHITE), 0, s.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static Spannable tagText(String s, int color) {
        SpannableString spannableString = new SpannableString(s);
        spannableString.setSpan(new TextTagSpan(color, Colors.WHITE), 0, s.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static CharSequence parseBoldTags(String string) {
        if (!string.contains("**")) return string;
        SpannableStringBuilder spannable = new SpannableStringBuilder(string);
        int openIndex, closeIndex = -1;
        while ((openIndex = string.indexOf("**", closeIndex + 1)) != -1) {
            if ((closeIndex = string.indexOf("**", openIndex + 1)) == -1) break;
            spannable.delete(openIndex, openIndex + 2);
            closeIndex -= 2;
            spannable.delete(closeIndex, closeIndex + 2);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), openIndex, closeIndex, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    private static final String[] MD_TOKENS_OP = {"**", "__", "~~", "``", "[[", "http", "https"};
    private static final String[] MD_TOKENS_CL = {"**", "__", "~~", "``", "]]", " ", " "};
    private static final Factory[] MD_SPANS = {(p) -> new StyleSpan(Typeface.BOLD), (p) -> new StyleSpan(Typeface.ITALIC),
            (p) -> new StyleSpan(Typeface.BOLD_ITALIC), (p) -> isApi28() ? new TypefaceSpan(Typeface.MONOSPACE) : new Object(), (p) -> new URLSpan((String) p),
            (p) -> new URLSpan((String) p), (p) -> new URLSpan((String) p)};


    private interface Factory {
        Object get(Object param);
    }

    public static void applyStylingTags(SpannableStringBuilder builder) {
        int openIndex, closeIndex;
        String tokenOpen, tokenClose;
        Object span;
        for (int i = 0; i < MD_TOKENS_OP.length; i++) {
            tokenOpen = MD_TOKENS_OP[i];
            tokenClose = MD_TOKENS_CL[i];
            closeIndex = -1;
            boolean urlToken = tokenOpen.contains("http");
            while ((openIndex = builder.indexOf(tokenOpen, closeIndex + 1)) != -1) {
                if ((closeIndex = builder.indexOf(tokenClose, openIndex + 1)) == -1 && !urlToken) break; // Leave a chance to close an url span
                if (!urlToken) {
                    builder.delete(openIndex, openIndex + tokenOpen.length());
                    closeIndex -= tokenOpen.length();
                    builder.delete(closeIndex, closeIndex + tokenClose.length());
                }
                if (closeIndex == -1) closeIndex = builder.length(); // Url end token not found, so we are at the end of our string
                span = MD_SPANS[i].get(builder.substring(openIndex, closeIndex));
                builder.setSpan(span, openIndex, closeIndex, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static String specChars(String string) {
        return string
                .replace("<<", "«")
                .replace(">>", "»");
    }

    public static String prepareForHtml(String string) {
        return string
                .replace("&ThickSpace;", "  ");
    }

    public static JSONObject jsonObject(String string) {
        if (string == null) return null;
        try {
            return new JSONObject(string);
        } catch (JSONException e) {
            return null;
        }
    }

    public static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int parseInt(String s, int b) {
        try {
            return Integer.parseInt(s, b);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @SafeVarargs
    public static <T> T[] array(T... ts) {
        return ts;
    }

    public static String nonNull(String s) {
        return s == null ? "" : s;
    }

    public static boolean notAllNull(Object[] objects) {
        for (Object o : objects)
            if (o == null) return false;
        return true;
    }

    public static int sum(int[] array) {
        int sum = 0;
        for (int value : array) sum += value;
        return sum;
    }

    public static String toStringSigned(int number) {
        return number < 0 ? "-" : "+" + number;
    }

    public static String str(int number) {
        return Integer.toString(number);
    }

    public static String truncate(String s, int max) {
        if (s.length() > max) return s.substring(0, max) + "…";
        return s;
    }

    public static int convertXmlValueToInt(CharSequence charSeq, int defaultValue) {
        if (null == charSeq || charSeq.length() == 0)
            return defaultValue;
        String nm = charSeq.toString();
        // XXX This code is copied from Integer.decode() so we don't
        // have to instantiate an Integer!
        int value;
        int sign = 1;
        int index = 0;
        int len = nm.length();
        int base = 10;
        if ('-' == nm.charAt(0)) {
            sign = -1;
            index++;
        }
        if ('0' == nm.charAt(index)) {
            //  Quick check for a zero by itself
            if (index == (len - 1))
                return 0;
            char c = nm.charAt(index + 1);
            if ('x' == c || 'X' == c) {
                index += 2;
                base = 16;
            } else {
                index++;
                base = 8;
            }
        } else if ('#' == nm.charAt(index)) {
            index++;
            base = 16;
        }
        return Integer.parseInt(nm.substring(index), base) * sign;
    }
}
