package com.majeur.psclient.widget

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.*
import android.view.View.MeasureSpec
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.majeur.psclient.R
import com.majeur.psclient.databinding.PopupBattleTipsBinding
import com.majeur.psclient.util.dp
import kotlin.math.max

class BattleTipPopup(context: Context) : PopupWindow(context), OnTouchListener {

    var bindPopupListener: ((anchorView: View, titleView: TextView, descView: TextView, placeHolderTop: ImageView, placeHolderBottom: ImageView) -> Unit)? = null

    private val binding: PopupBattleTipsBinding
    private var currentAnchorView: View? = null

    private val thumbOffset = context.dp(8f)
    private var downY = 0
    private var isUserTouching = false
    private var longPressPerformed = false
    private val tempArr = IntArray(2)
    private val tempRect = Rect()

    private val longPressTimeout
        get() = ViewConfiguration.getLongPressTimeout().toLong()
    private val topWindowInset
        get() = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) currentAnchorView!!.rootWindowInsets.stableInsetTop else 0

    init {
        setBackgroundDrawable(null)
        animationStyle = R.style.Animation_PSClient_TipPopup
        binding = PopupBattleTipsBinding.inflate(LayoutInflater.from(context))
        contentView = binding.root
    }

    fun addTippedView(view: View) = view.setOnTouchListener(this)

    fun removeTippedView(view: View) = view.setOnTouchListener(null)

    private val longPressCheckRunnable = Runnable {
        if (!isUserTouching) return@Runnable
        longPressPerformed = true
        bindPopupListener?.invoke(currentAnchorView!!, binding.title, binding.content, binding.im1, binding.im2)
        showPopup()
    }

    // This one is a bit tricky. PopupWindow does not provide any support for showing a popup above a view.
    // We have to measure our content view ourselves (assuming our content view is the Popup's view itself, which
    // is the case when Popup's background is null). Then the Window created for the Popup is fullscreen with the
    // content view container using setFitsSystemWindows, so we have to offset our y coordinate to avoid issue
    // in measurement due to window inset.
    private fun showPopup() {
        measureContentView(tempRect)
        currentAnchorView!!.getLocationInWindow(tempArr)
        val x = tempArr[0] + currentAnchorView!!.width / 2 - tempRect.width() / 2
        val windowInsetTop = topWindowInset
        val y = max(windowInsetTop, windowInsetTop + tempArr[1] + downY - tempRect.height() - thumbOffset)
        showAtLocation(currentAnchorView, Gravity.NO_GRAVITY, x, y)
    }

    override fun onTouch(view: View, motionEvent: MotionEvent) = when (motionEvent.action) {
        MotionEvent.ACTION_DOWN -> {
            currentAnchorView = view
            downY = motionEvent.y.toInt()
            isUserTouching = true
            // If view is clickable, we wait for a long press to be done before triggering popup, if not, we show immediately
            if (view.isClickable) view.postDelayed(longPressCheckRunnable, longPressTimeout)
            else longPressCheckRunnable.run()
            true
        }
        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
            isUserTouching = false
            currentAnchorView = null
            if (longPressPerformed) {
                longPressPerformed = false
                dismiss()
            } else { // If pointer is up before long press timeout, perform a regular click
                view.performClick()
            }
            true
        }
        else -> false
    }

    private fun measureContentView(out: Rect) {
        currentAnchorView!!.getWindowVisibleDisplayFrame(out)
        val wSpec = MeasureSpec.makeMeasureSpec(out.width(), MeasureSpec.AT_MOST)
        val hSpec = MeasureSpec.makeMeasureSpec(out.height(), MeasureSpec.AT_MOST)
        contentView.measure(wSpec, hSpec)
        out.set(0, 0, contentView.measuredWidth, contentView.measuredHeight)
    }

    // Really faster than its only equivalent using ConstraintLayout
    class Layout @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : ViewGroup(context, attrs, defStyleAttr) {

        private val imageSpacing = dp(2f)
        private lateinit var title: View
        private lateinit var content: View
        private lateinit var image1: View
        private lateinit var image2: View

        override fun onViewAdded(child: View) {
            super.onViewAdded(child)
            when (child.id) {
                R.id.title -> title = child
                R.id.content -> content = child
                R.id.im1 -> image1 = child
                R.id.im2 -> image2 = child
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            var lp = image1.layoutParams
            if (lp.width < 0 || lp.height < 0) throw UnsupportedOperationException("popup_im1 must have explicit width and height")
            image1.measure(MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY))
            lp = image2.layoutParams
            if (lp.width < 0 || lp.height < 0) throw UnsupportedOperationException("popup_im2 must have explicit width and height")
            image2.measure(MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY))
            val imagesWidth = Math.max(image1.measuredWidth, image2.measuredWidth)
            title.measure(MeasureSpec.makeMeasureSpec(width - imagesWidth, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
            content.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height - title.measuredHeight, MeasureSpec.AT_MOST))
            val measuredWidth = Math.max(title.measuredWidth + imagesWidth, content.measuredWidth)
            val measuredHeight = title.measuredHeight + content.measuredHeight
            setMeasuredDimension(measuredWidth, measuredHeight)
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            val parentLeft = paddingLeft
            val parentRight = right - left - paddingRight
            val parentTop = paddingTop
            val parentBottom = bottom - top - paddingBottom
            title.layout(parentLeft,
                    parentTop,
                    parentLeft + title.measuredWidth,
                    parentTop + title.measuredHeight)
            content.layout(parentLeft,
                    title.bottom,
                    parentLeft + content.measuredWidth,
                    title.bottom + content.measuredHeight)
            image1.layout(parentRight - image1.measuredWidth,
                    parentTop,
                    parentRight,
                    parentTop + image1.measuredHeight)
            image2.layout(parentRight - image2.measuredWidth,
                    image1.bottom + imageSpacing,
                    parentRight,
                    image1.bottom + imageSpacing + image2.measuredHeight)
        }
    }

}