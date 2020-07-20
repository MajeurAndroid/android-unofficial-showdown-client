package com.majeur.psclient.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.collection.ArraySet
import com.majeur.psclient.util.baselineForTop
import com.majeur.psclient.util.dp
import com.majeur.psclient.util.sp
import com.majeur.psclient.util.startForLeft

class ToasterView(context: Context) : FrameLayout(context) {

    private val maxToastViewCacheSize = 6
    private val toastViewCache = ArraySet<ToastView>()

    fun makeToast(text: String?, color: Int) {
        if (text == null) return
        obtainToastView().apply {
            toastText = text
            toastColor = color
            addView(this)
            post { startToastAnimation(this) }
        }
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        if (toastViewCache.size < maxToastViewCacheSize)
            toastViewCache.add(child as ToastView)
    }

    private fun startToastAnimation(toastView: ToastView) {
        toastView.apply {
            translationY = 0f
            alpha = 1f
            animate()
                .setDuration(1000)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationY(-toastView.height.toFloat())
                .alpha(0f)
                .withEndAction { removeView(toastView) }
                .start()
        }
    }

    private fun obtainToastView(): ToastView {
        if (toastViewCache.isNotEmpty()) return toastViewCache.first().also { toastViewCache.remove(it) }
        return ToastView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childCount = childCount
        if (childCount == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val childWidthSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST)
        var maxWidth = 0
        var maxHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(childWidthSpec, childHeightSpec)
            maxWidth = maxWidth.coerceAtLeast(child.measuredWidth)
            maxHeight = maxHeight.coerceAtLeast(child.measuredHeight)
        }
        setMeasuredDimension(maxWidth, maxHeight * 2)
    }

    private class ToastView(context: Context?) : View(context) {

        var toastText = ""
        var toastColor = 0

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textBounds = Rect()
        private val cornerRadius = dp(5f)
        private val shadowRadius = dp(4f)
        private val shadowDy = shadowRadius / 4f

        init {
            setLayerType(LAYER_TYPE_HARDWARE, null)
            paint.textSize = sp(14f).toFloat()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            paint.getTextBounds(toastText, 0, toastText!!.length, textBounds)
            setMeasuredDimension(textBounds.width() + 2 * cornerRadius + 2 * shadowRadius,
                    textBounds.height() + 2 * cornerRadius + shadowRadius)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            paint.apply {
                color = toastColor
                setShadowLayer(shadowRadius.toFloat(), 0f, shadowDy, Color.BLACK)
            }
           canvas.drawRoundRect(shadowRadius.toFloat(), 0f,
                   width - shadowRadius.toFloat(),
                   height - shadowRadius.toFloat(),
                    cornerRadius.toFloat(), cornerRadius.toFloat(), paint)
            paint.apply {
                color = Color.WHITE
                clearShadowLayer()
            }
            canvas.drawText(toastText, textBounds.startForLeft(shadowRadius + cornerRadius).toFloat(),
                    textBounds.baselineForTop(cornerRadius).toFloat(), paint)
        }
    }
}