package com.majeur.psclient.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

    private static SharedPreferences preferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getStrPreference(Context context, String key) {
        return preferences(context).getString(key, null);
    }

    public static boolean getBoolPreference(Context context, String key) {
        return preferences(context).getBoolean(key, false);
    }

    public static void setPreference(Context context, String key, String value) {
        preferences(context).edit().putString(key, value).apply();
    }

    public static void setPreference(Context context, String key, boolean value) {
        preferences(context).edit().putBoolean(key, value).apply();
    }
}
