package com.majeur.psclient.widget

import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.View
import androidx.collection.ArrayMap
import com.majeur.psclient.model.common.Colors
import com.majeur.psclient.model.common.Colors.sideColor
import com.majeur.psclient.util.dp
import com.majeur.psclient.util.sp
import com.majeur.psclient.util.xForLeft
import com.majeur.psclient.util.yForTop
import kotlin.math.roundToInt

class SideView(context: Context?) : View(context) {

    var gravity = Gravity.NO_GRAVITY
        set(value) {
            field = value
            setPadding(if (isGravityRight) shadowRadius else 0, (shadowRadius - shadowDy).roundToInt(),
                    if (!isGravityRight) shadowRadius else 0, (shadowRadius + shadowDy).roundToInt())
        }
    private val isGravityRight
        get() = Gravity.getAbsoluteGravity(gravity, layoutDirection) and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.RIGHT

    private val sides = ArrayMap<String, Int>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var widthSize = 0
    private val path = Path()
    private val drawingRect = Rect()
    private val textBounds = Rect()
    private val tempRect by lazy { Rect() }
    private val shadowRadius = dp(2f)
    private val shadowDy = shadowRadius / 4f
    private val verticalSpacing = dp(4f)
    private val tagRadius = dp(2f)
    private val tagTextSize = sp(9f)

    init {
        gravity = Gravity.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    
    fun sideStart(rawSide: String) {
        val key = rawSideToKey(rawSide)
        var currentCount = sides[key]
        if (currentCount == null) currentCount = 0
        sides[key] = currentCount + 1
        requestLayout()
        invalidate()
    }

    fun sideEnd(rawSide: String) {
        val key = rawSideToKey(rawSide)
        sides.remove(key)
        requestLayout()
        invalidate()
    }

    fun clearAllSides() {
        sides.clear()
        requestLayout()
        invalidate()
    }

    private fun rawSideToKey(rawSide: String): String {
        var rawSide = rawSide
        rawSide = rawSide.trim { it <= ' ' }
        val spaceIndex = rawSide.indexOf(' ')
        return if (spaceIndex >= 0) (rawSide.substring(0, 1) + rawSide.substring(spaceIndex + 1, spaceIndex + 2))
                .toUpperCase() else rawSide.substring(0, 2).toUpperCase()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        drawingRect.setEmpty()
        drawContent(null, drawingRect)
        setMeasuredDimension(drawingRect.width() + paddingLeft + paddingRight,
                drawingRect.height() + paddingTop + paddingBottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        widthSize = w
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawingRect.setEmpty()
        drawContent(canvas, drawingRect)
    }

    private fun drawContent(canvas: Canvas?, drawingRect: Rect) {
        var top = paddingTop
        for ((key, count) in sides) {
            val countString = if (count > 1) (if (isGravityRight) "" else " ") + "x" + count + (if (isGravityRight) " " else "") else ""
            val text = if (isGravityRight) countString + key else key + countString
            paint.apply {
                color = sideColor(key)
                textSize = tagTextSize.toFloat()
                getTextBounds(text, 0, text.length, textBounds)
                setShadowLayer(shadowRadius.toFloat(), 0f, shadowDy, Colors.BLACK)
            }

            val rectWith = textBounds.width() + 2 * tagRadius
            val rectHeight = textBounds.height() + 2 * tagRadius

            drawSemiRoundRect(canvas, tempRect,
                    if (isGravityRight) widthSize - paddingRight - rectWith else paddingLeft,
                    top,
                    if (isGravityRight) widthSize - paddingRight else paddingLeft + rectWith,
                    top + rectHeight,
                    isGravityRight)
            drawingRect.union(tempRect)
            paint.apply {
                color = Colors.WHITE
                clearShadowLayer()
            }
            canvas?.drawText(text,
                    textBounds.xForLeft(
                            if (isGravityRight) widthSize - paddingRight - rectWith + tagRadius else paddingLeft + tagRadius
                    ).toFloat(), textBounds.yForTop(top + tagRadius).toFloat(), paint)
            top += rectHeight + verticalSpacing
        }
    }

    private fun drawSemiRoundRect(canvas: Canvas?, drawingRect: Rect, left: Int, top: Int, right: Int, bottom: Int, flatAtRight: Boolean) {
        path.reset()
        if (flatAtRight) path.apply {
            moveTo(right.toFloat(), top.toFloat())
            lineTo(left + tagRadius.toFloat(), top.toFloat())
            quadTo(left.toFloat(), top.toFloat(), left.toFloat(), top + tagRadius.toFloat())
            lineTo(left.toFloat(), bottom - tagRadius.toFloat())
            quadTo(left.toFloat(), bottom.toFloat(), left + tagRadius.toFloat(), bottom.toFloat())
            lineTo(right.toFloat(), bottom.toFloat())
            lineTo(right.toFloat(), top.toFloat())
        } else path.apply {
            moveTo(left.toFloat(), top.toFloat())
            lineTo(right - tagRadius.toFloat(), top.toFloat())
            quadTo(right.toFloat(), top.toFloat(), right.toFloat(), top + tagRadius.toFloat())
            lineTo(right.toFloat(), bottom - tagRadius.toFloat())
            quadTo(right.toFloat(), bottom.toFloat(), right - tagRadius.toFloat(), bottom.toFloat())
            lineTo(left.toFloat(), bottom.toFloat())
            lineTo(left.toFloat(), top.toFloat())
        }
        canvas?.drawPath(path, paint)
        drawingRect.set(left, top, right, bottom)
    }
}