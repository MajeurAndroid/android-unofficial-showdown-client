package com.majeur.psclient.util

import android.graphics.Typeface
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.core.text.toSpannable
import java.util.*

private val ID_REGEX = "[^a-z0-9]".toRegex()

fun String.toId(): String = this.toLowerCase(Locale.ROOT).replace(ID_REGEX, "")

fun CharSequence.bold() = toSpannable().bold()

fun CharSequence.italic() = toSpannable().italic()

fun CharSequence.small() = toSpannable().small()

fun CharSequence.big() = toSpannable().big()

fun CharSequence.relSize(relSize: Float) = toSpannable().relSize(relSize)

fun CharSequence.color(color: Int) = toSpannable().color(color)

fun CharSequence.bg(color: Int) = toSpannable().bg(color)

fun Spannable.bold(): Spannable {
    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    return this
}

fun Spannable.italic(): Spannable {
    setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    return this
}

fun Spannable.small() = relSize(0.8f)

fun Spannable.big() = relSize(1.2f)

fun Spannable.relSize(relSize: Float): Spannable {
    setSpan(RelativeSizeSpan(relSize), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    return this
}

fun Spannable.color(color: Int): Spannable {
    setSpan(ForegroundColorSpan(color), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    return this
}

fun Spannable.bg(color: Int): Spannable {
    setSpan(BackgroundColorSpan(color), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    return this
}