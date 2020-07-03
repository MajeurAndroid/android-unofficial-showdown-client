package com.majeur.psclient.util.recyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

open class DividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val bounds = Rect()
    private val divider: Drawable?
    var startOffset = 0

    init {
        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))
        divider = a.getDrawable(0)
        a.recycle()
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null || parent.adapter == null || divider == null) {
            return
        }
        canvas.save()
        val left: Int
        val right: Int
        //noinspection AndroidLintNewApi - NewApi lint fails to handle overrides.
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(left, parent.paddingTop, right,
                    parent.height - parent.paddingBottom)
        } else {
            left = 0
            right = parent.width
        }

        val childCount = parent.childCount
        for (i in 1 until childCount) {
            val child = parent.getChildAt(i)
            if (!shouldDrawDivider(parent, child)) continue
            parent.getDecoratedBoundsWithMargins(child, bounds)
            val top = bounds.top + child.translationY.roundToInt()
            val bottom = top + divider.intrinsicHeight
            divider.setBounds(left + startOffset, top, right, bottom)
            divider.draw(canvas)
        }
        canvas.restore()
    }

    protected open fun shouldDrawDivider(parent: RecyclerView, child: View) = true

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (divider == null) return
        outRect.set(0, divider.intrinsicHeight, 0, 0)
    }

}