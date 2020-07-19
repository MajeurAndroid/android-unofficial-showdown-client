package com.majeur.psclient.util.recyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.majeur.psclient.R
import com.majeur.psclient.util.dp

open class ItemTouchHelperCallbacks(
        context: Context,
        allowReordering: Boolean = false,
        allowDeletion: Boolean = false
) : ItemTouchHelper.SimpleCallback(
        if (allowReordering) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0,
        if (allowDeletion) ItemTouchHelper.LEFT else 0) {

    private val background = ColorDrawable(ContextCompat.getColor(context, R.color.error))
    private val icon = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!
            .also {
                it.setTint(Color.WHITE)
            }
    private val iconLeftMargin = context.dp(12f)

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition
        onMoveItem(from, to)
        return true
    }

    protected open fun onMoveItem(from: Int, to: Int) = Unit

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        onRemoveItem(position)
    }

    protected open fun onRemoveItem(position: Int) = Unit

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        val itemView = viewHolder.itemView

        if (dY != 0f) {
            itemView.elevation = itemView.dp(4f).toFloat()
        } else {
            itemView.elevation = 0f
        }

        if (dX < 0) {
            background.setBounds(itemView.right + dX.toInt(),
                    itemView.top, itemView.right, itemView.bottom)
            val cY = (itemView.top + itemView.bottom) / 2
            icon.setBounds(itemView.right - icon.intrinsicWidth - iconLeftMargin,
                    cY - icon.intrinsicHeight / 2, itemView.right - iconLeftMargin,
                    cY + icon.intrinsicHeight / 2)
        } else {
            background.setBounds(0, 0, 0, 0)
            icon.setBounds(0, 0, 0, 0)
        }
        background.draw(c)
        icon.draw(c)
    }
}