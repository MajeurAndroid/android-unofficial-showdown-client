package com.majeur.psclient.util

import android.content.Context

object Preferences {

    private const val PREF_NAME = "user-preferences"
    private const val KEY_NEWS_BANNER = "news-banner-enabled"
    private const val KEY_BATTLE_SOUND = "battle-sound-enabled"

    fun isNewsBannerEnabled(c: Context) = readBool(c, KEY_NEWS_BANNER, true)
    fun setNewsBannerEnabled(c: Context, value: Boolean) = writeBool(c, KEY_NEWS_BANNER, value)

    fun isBattleSoundEnabled(c: Context) = readBool(c, KEY_BATTLE_SOUND, true)
    fun setBattleSoundEnabled(c: Context, value: Boolean) = writeBool(c, KEY_BATTLE_SOUND, value)

    private fun readBool(c: Context, key: String, def: Boolean) = get(c).getBoolean(key, def)
    private fun writeBool(c: Context, key: String, value: Boolean) = get(c).edit().putBoolean(key, value).apply()

    private fun get(c: Context) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

}