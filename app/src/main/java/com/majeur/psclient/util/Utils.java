package com.majeur.psclient.util;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.majeur.psclient.model.Colors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Utils {

    private static float sScreenDensity;

    public static void setStaticScreenDensity(Resources resources) {
        sScreenDensity = resources.getDisplayMetrics().density;
    }

    public static int dpToPx(float dp) {
        return (int) (sScreenDensity * dp);
    }

    public static int hashColor(String name) {
        int sum = 0;
        for (int i = 0; i < name.length(); i++)
            sum += name.charAt(i);

        int r = (int) (Double.parseDouble("0." + Double.toString(Math.sin(sum+1)).substring(6)) * 256);
        int g = (int) (Double.parseDouble("0." + Double.toString(Math.sin(sum+2)).substring(6)) * 256);
        int b = (int) (Double.parseDouble("0." + Double.toString(Math.sin(sum+3)).substring(6)) * 256);

        return Color.rgb(r, g, b);
    }

    public static int getTagColor(int textColor) {
        float B = (0.299f * Color.red(textColor) + 0.587f * Color.green(textColor) + 0.114f * Color.blue(textColor)) / 255;
        return (B <= 0.65f) ? 0xFFEDEDED : 0xFF606060;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getList(JSONArray array) throws JSONException {
        List<T> list = new LinkedList<>();
        for (int i = 0; i < array.length(); i++)
            list.add((T) array.get(i));
        return list;
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

    public static <T> T[] append(T[] array, T value) {
        array = Arrays.copyOf(array, array.length + 1);
        array[array.length - 1] = value;
        return array;
    }

    public static boolean contains(String string, String... ss) {
        if (string == null) return false;
        for (String s : ss)
            if (string.contains(s)) return true;
        return false;
    }

    public static <T> int indexOf(T value, T[] array) {
        for (int i = 0; i < array.length; i++)
            if (array[i].equals(value)) return i;
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
}
