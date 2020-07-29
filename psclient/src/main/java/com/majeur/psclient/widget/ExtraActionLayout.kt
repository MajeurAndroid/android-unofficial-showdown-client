package com.majeur.psclient.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.majeur.psclient.util.dp

class ExtraActionLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TRANSLATE_ANIM_DURATION = 500L
        private val TRANSLATE_ANIM_INTERPOLATOR = OvershootInterpolator(1.4f)
        private const val SHOW_HIDE_ANIM_DURATION = 300L
        private val SHOW_HIDE_ANIM_INTERPOLATOR = FastOutSlowInInterpolator()
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.END
        setPadding(dp(16f), 0, 0, 0)
        clipToPadding = false
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return super.generateLayoutParams(attrs).apply { topMargin = dp(12f) }
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        if (child?.visibility == GONE) child.alpha = 0f
    }

    fun setTopOffset(offset: Int, delay: Long = 0L) {
        children.forEach { child ->
            child.animate().apply {
                duration = TRANSLATE_ANIM_DURATION
                interpolator = TRANSLATE_ANIM_INTERPOLATOR
                startDelay = delay
                translationY(offset.toFloat())
                start()
            }
        }
    }

    fun showItem(id: Int) {
        val child = children.firstOrNull { it.id == id } ?: throw IllegalArgumentException("No direct child for this id")
        showChild(child, true)
    }

    fun hideItem(id: Int) {
        val child = children.firstOrNull { it.id == id } ?: throw IllegalArgumentException("No direct child for this id")
        showChild(child, false)
    }

    private fun showChild(view: View, show: Boolean) {
        if (show) view.visibility = View.VISIBLE
        view.animate().apply {
            duration = SHOW_HIDE_ANIM_DURATION
            interpolator = SHOW_HIDE_ANIM_INTERPOLATOR
            translationX(if (show) 0f else view.width.toFloat())
            alpha(if (show) 1f else 0f)
            withEndAction { if (!show) view.visibility = View.GONE }
            start()
        }
        setEnabledRec(view, show)
    }

    private fun setEnabledRec(view: View, enabled: Boolean) {
        if (view is ViewGroup) view.children.forEach { setEnabledRec(it, enabled) }
        view.isEnabled = enabled
    }

}