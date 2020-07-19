package com.majeur.psclient.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Spannable
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import androidx.core.text.toSpannable
import androidx.core.text.toSpanned
import androidx.fragment.app.Fragment
import java.util.*
import kotlin.text.Typography.ellipsis

private val ID_REGEX = "[^a-z0-9]".toRegex()

fun String.toId(): String = this.toLowerCase(Locale.ROOT).replace(ID_REGEX, "")
fun String.or(or: String) : String = if (isBlank()) or else this
fun String.toUpperCase() = toUpperCase(Locale.ROOT)
fun String.toLowerCase() = toLowerCase(Locale.ROOT)
fun String.truncate(n: Int) = if (length > n) take(n) + ellipsis else this

fun Int.toSignedString() = if (this < 0) "-" else "+" + toString()

// We cannot use a custom "operator fun plus()" because String.plus() override is impossible
infix fun CharSequence.concat(other: CharSequence): CharSequence = TextUtils.concat(this, other)

fun CharSequence.spanned() = toSpanned()
fun CharSequence.bold() = toSpannable().bold()
fun CharSequence.italic() = toSpannable().italic()
fun CharSequence.small() = toSpannable().small()
fun CharSequence.big() = toSpannable().big()
fun CharSequence.relSize(relSize: Float) = toSpannable().relSize(relSize)
fun CharSequence.color(color: Int) = toSpannable().color(color)
fun CharSequence.bg(color: Int) = toSpannable().bg(color)
fun CharSequence.tag(color: Int) = toSpannable().tag(color)

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

fun Spannable.tag(color: Int, textColor: Int = Color.WHITE): Spannable {
    setSpan(TextTagSpan(color, textColor), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    return this
}

fun View.dp(dp: Float) = context.dp(dp)
fun Fragment.dp(dp: Float) = resources.dp(dp)
fun Context.dp(dp: Float) = resources.dp(dp)
fun Resources.dp(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
        displayMetrics).toInt()

fun View.sp(sp: Float) = context.sp(sp)
fun Fragment.sp(sp: Float) = resources.sp(sp)
fun Context.sp(sp: Float) = resources.sp(sp)
fun Resources.sp(sp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
        displayMetrics).toInt()

fun Rect.xForLeft(textLeft: Int) = textLeft - left // Offset by Glyph's AdvanceX value added by Skia
fun Rect.yForTop(textTop: Int) = textTop - top // Computes text's baseline for a given top

fun <T> Array<T>.minusLast() = copyOfRange(0, size - 1)
fun <T> Array<T>.minusFirst() = copyOfRange(1, size)