package com.majeur.psclient.util

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.ListView
import androidx.core.view.children

// Mimic the behaviour of a nested scroll capable list view
class NestedScrollLikeTouchListener : View.OnTouchListener {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        var preventParentScroll = false
        if ((view as ListView).childCount > 0) {
            val isOnTop = view.firstVisiblePosition == 0 && view.children.first().top == view.paddingTop
            val allItemsVisible = isOnTop && view.lastVisiblePosition == view.childCount
            preventParentScroll = !isOnTop && !allItemsVisible
        }
        view.parent.requestDisallowInterceptTouchEvent(preventParentScroll)
        return view.onTouchEvent(event)
    }

}