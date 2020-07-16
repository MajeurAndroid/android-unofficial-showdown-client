package com.majeur.psclient.widget

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Property
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.collection.ArrayMap
import com.majeur.psclient.model.battle.StatModifiers
import com.majeur.psclient.model.battle.StatModifiers.Companion.STAT_KEYS
import com.majeur.psclient.model.battle.VolatileStatus
import com.majeur.psclient.model.battle.VolatileStatus.Companion.getForId
import com.majeur.psclient.model.common.Colors
import com.majeur.psclient.model.common.Colors.healthColor
import com.majeur.psclient.model.common.Colors.statusColor
import com.majeur.psclient.model.pokemon.BattlingPokemon
import com.majeur.psclient.util.*
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.roundToInt

class StatusView(context: Context?) : View(context) {

    private var label = ""
    private var health = 0f
    private var status = ""
    private val volatileStatus = ArrayMap<String, VolatileStatus>()
    private val statsModifiers = ArrayMap<String, Float>()

    private val healthAnimator = ObjectAnimator()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawingRect by lazy { Rect() }
    private val textBounds by lazy { Rect() }
    private val tempRect by lazy { Rect() }
    private val tempRect2 by lazy { Rect() }
    private val tempRect3 by lazy { Rect() }
    private val defaultTypeFace by lazy { Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
    private val boldTypeFace by lazy { Typeface.create("sans-serif-medium", Typeface.NORMAL) }

    private val labelTextSize = sp(14f)
    private val tagTextSize = sp(9f)
    private val tagCornerRadius = dp(2f)
    private val verticalSpacing = dp(3.5f)
    private val horizontalSpacing = dp(3f)
    private val healthBarWidth = dp(126f)
    private val healthBarHeight = dp(5f)
    private val healthBarStrokeWidth = dp(1f)
    private val shadowRadius = dp(4f).toFloat()
    private val shadowDy = shadowRadius / 4f

    private val maxTagLineWidth = healthBarWidth + healthBarStrokeWidth

    init {
        setPadding(shadowRadius.toInt(), (shadowRadius - shadowDy).roundToInt(), shadowRadius.toInt(),
                (shadowRadius + shadowDy).roundToInt())
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        healthAnimator.interpolator = DecelerateInterpolator()
        healthAnimator.duration = 500
        healthAnimator.target = this
        healthAnimator.setProperty(HEALTH_PROP)
        statsModifiers["atk"] = 1f
        statsModifiers["def"] = 1f
        statsModifiers["spa"] = 1f
        statsModifiers["spd"] = 1f
        statsModifiers["spe"] = 1f
        statsModifiers["eva"] = 1f
    }

    fun setPokemon(pokemon: BattlingPokemon) {
        label = "${pokemon.name} ${pokemon.gender ?: ""} l.${pokemon.level}"
        health = pokemon.condition?.health ?: 0f
        status = pokemon.condition?.status ?: ""
        volatileStatus.clear()
        updateModifier(pokemon.statModifiers)
    }

    fun setHealth(health: Float) {
        if (healthAnimator.isStarted) healthAnimator.cancel()
        healthAnimator.setFloatValues(health)
        healthAnimator.start()
    }

    fun setStatus(status: String?) {
        this.status = status ?: ""
        requestLayout()
        invalidate()
    }

    fun clear() {
        label = ""
        health = 0f
        status = ""
        volatileStatus.clear()
        statsModifiers["atk"] = 1f
        statsModifiers["def"] = 1f
        statsModifiers["spa"] = 1f
        statsModifiers["spd"] = 1f
        statsModifiers["spe"] = 1f
        statsModifiers["eva"] = 1f
        invalidate()
    }

    fun updateModifier(statModifiers: StatModifiers) {
        for (statKey in STAT_KEYS) statsModifiers[statKey] = statModifiers.modifier(statKey)
        requestLayout()
        invalidate()
    }

    fun addVolatileStatus(vStatus: String?) {
        val vs = getForId(vStatus) ?: return
        volatileStatus[vs.id] = vs
        requestLayout()
        invalidate()
    }

    fun removeVolatileStatus(vStatus: String?) {
        val vs = getForId(vStatus) ?: return
        volatileStatus.remove(vs.id)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        drawingRect.setEmpty()
        drawContent(null, drawingRect)
        setMeasuredDimension(drawingRect.width() + paddingLeft + paddingRight,
                drawingRect.height() + paddingTop + paddingBottom)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawingRect.setEmpty()
        drawContent(canvas, drawingRect)
    }

    private fun drawContent(canvas: Canvas?, drawingRect: Rect) {
        if (label.isBlank()) return  // Nothing to draw
        drawLabelText(canvas, paddingLeft, paddingTop, tempRect.apply { setEmpty() })
        drawingRect.set(tempRect)

        drawHealthBar(canvas, paddingLeft, tempRect.bottom + verticalSpacing / 2, tempRect.apply { setEmpty() })
        drawingRect.union(tempRect)

        drawTags(canvas, paddingLeft, tempRect.bottom + verticalSpacing, tempRect.apply { setEmpty() })
        drawingRect.union(tempRect)
    }

    private fun drawLabelText(canvas: Canvas?, minLeft: Int, minTop: Int, drawingRect: Rect) {
        val left = minLeft
        val top = minTop
        paint.apply {
            typeface = boldTypeFace
            color = Colors.WHITE
            textSize = labelTextSize.toFloat()
            getTextBounds(label, 0, label.length, textBounds)
            setShadowLayer(shadowRadius, 0f, shadowDy, Colors.BLACK)
        }
        canvas?.drawText(label, textBounds.xForLeft(left).toFloat(), textBounds.yForTop(top).toFloat(), paint)
        drawingRect.set(left, top, left + textBounds.width(), top + textBounds.height())
        paint.clearShadowLayer()
    }

    private fun drawHealthBar(canvas: Canvas?, minLeft: Int, minTop: Int, drawingRect: Rect) {
        val left = minLeft
        val top = minTop
        paint.apply {
            strokeWidth = healthBarStrokeWidth.toFloat()
            color = Colors.WHITE
            style = Paint.Style.FILL_AND_STROKE
            setShadowLayer(shadowRadius, 0f, shadowDy, Colors.BLACK)
        }
        canvas?.drawRoundRect(left.toFloat(), top.toFloat(),
                left + healthBarWidth.toFloat(),
                top + healthBarHeight.toFloat(),
                tagCornerRadius.toFloat(), tagCornerRadius.toFloat(), paint)
        drawingRect.set(left, top, left + healthBarWidth , top + healthBarHeight)
        paint.apply {
            paint.color = healthColor(health)
            paint.style = Paint.Style.FILL
            paint.clearShadowLayer()
        }
        canvas?.drawRoundRect(left.toFloat(), top.toFloat(),
                left + ceil(healthBarWidth * health),
                top + healthBarHeight.toFloat(),
                tagCornerRadius.toFloat(), tagCornerRadius.toFloat(), paint)
    }

    private fun drawTags(canvas: Canvas?, minLeft: Int, minTop: Int, drawingRect: Rect) {
        var left = minLeft
        val top = minTop

        val totalText = status + volatileStatus.values.filter { it.label != null }.joinToString(separator = "") +
                statsModifiers.map { DECIMAL_FORMAT.format(it.value) + Utils.firstCharUpperCase(it.key) }.joinToString(separator = "")
        paint.apply {
            typeface = defaultTypeFace
            textSize = tagTextSize.toFloat()
            getTextBounds(totalText, 0, totalText.length, textBounds)
        }
        val lineHeight = textBounds.height()
        var cY = (top + lineHeight / 2f).roundToInt()

        if (status.isNotBlank()) {
            drawTag(canvas, left, cY, status.toUpperCase(), Colors.WHITE, statusColor(status.toId()), tempRect2)
            drawingRect.set(tempRect2)
            left += tempRect2.width() + horizontalSpacing
        }
        for (vStatus in volatileStatus.values) {
            if (vStatus.label == null) continue
            tempRect3.set(tempRect2)
            drawTag(null, left, cY, vStatus.label, vStatus.color, Colors.WHITE, tempRect3)
            if (tempRect3.right > maxTagLineWidth + paddingLeft) { // Our tag will not fit, break line
                left = minLeft
                cY += lineHeight + verticalSpacing
            }
            drawTag(canvas, left, cY, vStatus.label, vStatus.color, Colors.WHITE, tempRect2)
            if (drawingRect.isEmpty) drawingRect.set(tempRect2) else drawingRect.union(tempRect2)
            left += tempRect2.width() + horizontalSpacing
        }
        for (entry in statsModifiers.entries) {
            if (entry.value == 1f) continue
            val text = DECIMAL_FORMAT.format(entry.value) + Utils.firstCharUpperCase(entry.key)
            tempRect3.set(tempRect2)
            drawTag(null, left, cY, text, if (entry.value < 1f) Colors.STAT_UNBOOST else Colors.STAT_BOOST,
                    Colors.WHITE, tempRect3)
            if (tempRect3.right > maxTagLineWidth + paddingLeft) { // Our tag will not fit, break line
                left = minLeft
                cY += lineHeight + verticalSpacing
            }
            drawTag(canvas, left, cY, text, if (entry.value < 1f) Colors.STAT_UNBOOST else Colors.STAT_BOOST,
                    Colors.WHITE, tempRect2)
            if (drawingRect.isEmpty) drawingRect.set(tempRect2) else drawingRect.union(tempRect2)
            left += tempRect2.width() + horizontalSpacing
        }
    }

    private fun drawTag(canvas: Canvas?, x: Int, cY: Int, text: String, textColor: Int, bgColor: Int, drawingRect: Rect) {
        paint.apply {
            color = bgColor
            typeface = defaultTypeFace
            textSize = tagTextSize.toFloat()
            getTextBounds(text, 0, text.length, textBounds)
            setShadowLayer(shadowRadius, 0f, shadowDy, Colors.BLACK)
        }
        val w = textBounds.width() + 2 * tagCornerRadius
        val h = textBounds.height() + 2 * tagCornerRadius
        canvas?.drawRoundRect(x.toFloat(), cY - h / 2f, x + w.toFloat(), cY + h / 2f,
                tagCornerRadius.toFloat(), tagCornerRadius.toFloat(), paint)
        drawingRect.set(x, cY - h / 2, x + w, cY + h / 2)
        paint.apply {
            color = textColor
            clearShadowLayer()
        }
        canvas?.drawText(text, textBounds.xForLeft(x + tagCornerRadius).toFloat(),
                textBounds.yForTop(cY - textBounds.height() / 2).toFloat(), paint)
    }

    companion object {
        private val DECIMAL_FORMAT = DecimalFormat("0.##")
        private val HEALTH_PROP = object : Property<StatusView, Float>(Float::class.java, "health") {
            override fun set(statusView: StatusView, health: Float) {
                statusView.health = health
                statusView.invalidate()
            }

            override fun get(statusView: StatusView): Float {
                return statusView.health
            }
        }
    }
    
}