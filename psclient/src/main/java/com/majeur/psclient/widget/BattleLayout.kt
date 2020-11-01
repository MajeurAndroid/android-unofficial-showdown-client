package com.majeur.psclient.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.majeur.psclient.R
import com.majeur.psclient.model.battle.Player
import com.majeur.psclient.model.battle.PokemonId
import com.majeur.psclient.util.dp
import kotlin.math.roundToInt

class BattleLayout @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var battleLayoutMode = MODE_BATTLE_SINGLE
    private val statusBarOffset = dp(18f)
    private var p1PreviewTeamSize = 6
    private var p2PreviewTeamSize = 6
    private val imageViewCache = mutableListOf<ImageView>()
    private val p1ImageViews = SparseArray<ImageView>()
    private val p1StatusViews = SparseArray<StatusView>()
    private val p1ToasterViews = SparseArray<ToasterView>()
    private val p2ImageViews = SparseArray<ImageView>()
    private val p2StatusViews = SparseArray<StatusView>()
    private val p2ToasterViews = SparseArray<ToasterView>()
    private val p1SideView = SideView(context)
    private val p2SideView = SideView(context)
    private val fxDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_hit, null)!!
    private var drawFx = false

    init {
        addView(p1SideView)
        addView(p2SideView)
        p2SideView.gravity = Gravity.END
    }

    fun setMode(mode: Int) {
        if (mode < MODE_PREVIEW || mode > MODE_BATTLE_TRIPLE) return
        battleLayoutMode = mode
        for (i in 0 until p1ImageViews.size()) p1ImageViews[i].setImageDrawable(null)
        for (i in 0 until p2ImageViews.size()) p2ImageViews[i].setImageDrawable(null)
        // This will ensure that next calls to getXXXView() returns proper view without waiting for a layout pass
        prepareViews(false, 0, 0)
        requestLayout()
    }

    fun setPreviewTeamSize(player: Player, teamSize: Int) {
        if (player === Player.TRAINER) p1PreviewTeamSize = teamSize
        if (player === Player.FOE) p2PreviewTeamSize = teamSize
    }

    fun getToasterView(id: PokemonId): ToasterView? {
        if (id.position < 0) return null
        val toasterViews = if (id.trainer) p1ToasterViews else p2ToasterViews
        return toasterViews[id.position]
    }

    fun getStatusView(id: PokemonId): StatusView? {
        if (id.position < 0) return null
        val statusViews = if (id.trainer) p1StatusViews else p2StatusViews
        return statusViews[id.position]
    }

    fun getStatusViews(player: Player): List<StatusView> {
        val statusViews = if (player === Player.TRAINER) p1StatusViews else p2StatusViews
        return (0 until statusViews.size()).map { i -> statusViews.valueAt(i) }
    }

    fun getSpriteView(id: PokemonId): ImageView? {
        if (id.position < 0) return null
        val imageViews = if (id.trainer) p1ImageViews else p2ImageViews
        return imageViews[id.position]
    }

    fun getSpriteViews(player: Player): List<ImageView> {
        val imageViews = if (player === Player.TRAINER) p1ImageViews else p2ImageViews
        return (0 until imageViews.size()).map { i -> imageViews.valueAt(i) }
    }

    fun getSideView(player: Player): SideView {
        val sideView = if (player === Player.TRAINER) p1SideView else p2SideView
        sideView.bringToFront() // TODO Fix this not working
        return sideView
    }

    fun swap(id: PokemonId, targetIndex: Int) {
        if (id.position < 0 || targetIndex < 0) return
        val imageViews = if (id.trainer) p1ImageViews else p2ImageViews
        val imageView1 = imageViews[id.position]
        val imageView2 = imageViews[targetIndex]
        imageViews.remove(id.position)
        imageViews.remove(targetIndex)
        imageViews.put(id.position, imageView2)
        imageViews.put(targetIndex, imageView1)
        val statusViews = if (id.trainer) p1StatusViews else p2StatusViews
        val statusView1 = statusViews[id.position]
        val statusView2 = statusViews[targetIndex]
        statusViews.remove(id.position)
        statusViews.remove(targetIndex)
        statusViews.put(id.position, statusView2)
        statusViews.put(targetIndex, statusView1)
        requestLayout()
    }

    fun displayHitIndicator(id: PokemonId) {
        val view = getSpriteView(id) ?: return
        val cX = (view.right + view.left) / 2
        val cY = (view.bottom + view.top) / 2
        val w = dp(40f)
        val h = w
        fxDrawable.bounds = Rect(cX - w / 2, cY - h / 2, cX + w / 2, cY + h / 2)
        val rw = Math.random().toFloat()
        val rh = Math.random().toFloat()
        if (id.foe) fxDrawable.bounds.offset(-(rw * w / 4).roundToInt(), (rh * h / 4).roundToInt())
        else fxDrawable.bounds.offset((rw * w / 4).roundToInt(), -(rh * h / 4).roundToInt())
        postDelayed({
            drawFx = false
            invalidate()
        }, 175)
        drawFx = true
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth: Int
        val measuredHeight: Int
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        //if (widthMode == MeasureSpec.UNSPECIFIED)
        //    throw new IllegalStateException("Fixed parent width required");
        measuredWidth = widthSize
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        measuredHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> (measuredWidth / ASPECT_RATIO).coerceAtMost(heightSize.toFloat()).toInt()
            // Here heightMode is equal to MeasureSpec.UNSPECIFIED, we take all the space we want.
            else -> (measuredWidth / ASPECT_RATIO).toInt()
        }
        measureChildren(MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.AT_MOST))
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun measureChildInLayout(child: View, parentWidth: Int, parentHeight: Int) {
        val layoutParams = child.layoutParams as LayoutParams
        val widthMeasureSpec = getChildMeasureSpec(layoutParams.width, parentWidth)
        val heightMeasureSpec = getChildMeasureSpec(layoutParams.height, parentHeight)
        child.measure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun getChildMeasureSpec(dim: Int, parentDim: Int) = when (dim) {
        ViewGroup.LayoutParams.WRAP_CONTENT -> MeasureSpec.makeMeasureSpec(parentDim, MeasureSpec.AT_MOST)
        ViewGroup.LayoutParams.MATCH_PARENT -> MeasureSpec.makeMeasureSpec(parentDim, MeasureSpec.EXACTLY)
        else -> MeasureSpec.makeMeasureSpec(dim, MeasureSpec.EXACTLY)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        prepareViews(true, width, height)
        when (battleLayoutMode) {
            MODE_PREVIEW -> layoutPreviewMode(width, height)
            MODE_BATTLE_SINGLE -> layoutBattleMode(1, width, height)
            MODE_BATTLE_DOUBLE -> layoutBattleMode(2, width, height)
            MODE_BATTLE_TRIPLE -> layoutBattleMode(3, width, height)
        }
    }

    private fun layoutChild(child: View, xIn: Int, yIn: Int, gravity: Int, fitInParent: Boolean, out: Point? = null) {
        val w = child.measuredWidth
        val h = child.measuredHeight
        var x = xIn
        var y = yIn
        if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL) {
            x = xIn - w / 2
        }
        if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
            y = yIn - h / 2
        }
        if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.BOTTOM) {
            y = yIn - h
        }
        if (fitInParent) {
            if (x < 0) x = 0
            if (y < 0) y = 0
            if (x + w > width) x = width - w
            if (y + h > height) y = height - h
        }
        out?.set(x, y)
        child.layout(x, y, x + w, y + h)
    }

    private fun layoutPreviewMode(width: Int, height: Int) {
        val p1Count = p1PreviewTeamSize
        var startPoint = Point((REL_TEAMPREV_P1_LINE[0].x * width).toInt(),
                (REL_TEAMPREV_P1_LINE[0].y * height).toInt())
        var endPoint = Point((REL_TEAMPREV_P1_LINE[1].x * width).toInt(),
                (REL_TEAMPREV_P1_LINE[1].y * height).toInt())
        var xStep = (endPoint.x - startPoint.x) / p1Count
        var yStep = (endPoint.y - startPoint.y) / p1Count
        for (i in 0 until p1Count) layoutChild(p1ImageViews[i], startPoint.x + i * xStep,
                startPoint.y + i * yStep, Gravity.CENTER, false)
        val p2Count = p2PreviewTeamSize
        startPoint = Point((REL_TEAMPREV_P2_LINE[0].x * width).toInt(),
                (REL_TEAMPREV_P2_LINE[0].y * height).toInt())
        endPoint = Point((REL_TEAMPREV_P2_LINE[1].x * width).toInt(),
                (REL_TEAMPREV_P2_LINE[1].y * height).toInt())
        xStep = (endPoint.x - startPoint.x) / p2Count
        yStep = (endPoint.y - startPoint.y) / p2Count
        for (i in 0 until p2Count) layoutChild(p2ImageViews[i], startPoint.x + (i + 1) * xStep,
                startPoint.y + (i + 1) * yStep, Gravity.CENTER, false)
    }

    private fun layoutBattleMode(count: Int, width: Int, height: Int) {
        val point = Point()
        var j: Int
        for (i in 0 until count) {
            point[(REL_BATTLE_P1_POS[count - 1][i].x * width).toInt()] = (REL_BATTLE_P1_POS[count - 1][i].y * height).toInt()
            var cX = point.x
            var cY = point.y
            layoutChild(p1ImageViews[i], cX, cY, Gravity.CENTER, false, point)
            layoutChild(p1StatusViews[i], cX, point.y - statusBarOffset, Gravity.CENTER_HORIZONTAL, true)
            layoutChild(p1ToasterViews[i], cX, cY, Gravity.CENTER, false)
            j = count - i - 1
            point[(REL_BATTLE_P2_POS[count - 1][j].x * width).toInt()] = (REL_BATTLE_P2_POS[count - 1][j].y * height).toInt()
            cX = point.x
            cY = point.y
            layoutChild(p2ImageViews[i], cX, cY, Gravity.CENTER, false, point)
            layoutChild(p2StatusViews[i], cX, point.y - statusBarOffset, Gravity.CENTER_HORIZONTAL, true)
            layoutChild(p2ToasterViews[i], cX, cY, Gravity.CENTER, false)
        }
        layoutChild(p1SideView, 0, 4 * height / 5, Gravity.CENTER_VERTICAL, true)
        layoutChild(p2SideView, width, 3 * height / 5, Gravity.CENTER_VERTICAL, true)
    }

    private fun prepareViews(inLayout: Boolean, width: Int, height: Int) {
        val p1ImageViewCount: Int
        val p1StatusViewCount: Int
        val p1ToasterViewCount: Int
        val p2ImageViewCount: Int
        val p2StatusViewCount: Int
        val p2ToasterViewCount: Int
        when (battleLayoutMode) {
            MODE_BATTLE_SINGLE -> {
                p1ToasterViewCount = 1
                p1StatusViewCount = p1ToasterViewCount
                p1ImageViewCount = p1StatusViewCount
                p2ToasterViewCount = 1
                p2StatusViewCount = p2ToasterViewCount
                p2ImageViewCount = p2StatusViewCount
            }
            MODE_BATTLE_DOUBLE -> {
                p1ToasterViewCount = 2
                p1StatusViewCount = p1ToasterViewCount
                p1ImageViewCount = p1StatusViewCount
                p2ToasterViewCount = 2
                p2StatusViewCount = p2ToasterViewCount
                p2ImageViewCount = p2StatusViewCount
            }
            MODE_BATTLE_TRIPLE -> {
                p1ToasterViewCount = 3
                p1StatusViewCount = p1ToasterViewCount
                p1ImageViewCount = p1StatusViewCount
                p2ToasterViewCount = 3
                p2StatusViewCount = p2ToasterViewCount
                p2ImageViewCount = p2StatusViewCount
            }
            else -> { // MODE_BATTLE_PREVIEW
                p1ImageViewCount = p1PreviewTeamSize
                p1ToasterViewCount = 0
                p1StatusViewCount = p1ToasterViewCount
                p2ImageViewCount = p2PreviewTeamSize
                p2ToasterViewCount = 0
                p2StatusViewCount = p2ToasterViewCount
            }
        }
        fillNeededViews(p1ImageViews, p1ImageViewCount, inLayout, width, height)
        fillNeededStatusViews(p1StatusViews, p1StatusViewCount, inLayout, width, height)
        fillNeededToasterViews(p1ToasterViews, p1ToasterViewCount, inLayout, width, height)
        fillNeededViews(p2ImageViews, p2ImageViewCount, inLayout, width, height)
        fillNeededStatusViews(p2StatusViews, p2StatusViewCount, inLayout, width, height)
        fillNeededToasterViews(p2ToasterViews, p2ToasterViewCount, inLayout, width, height)
        for (i in 0 until 6) {
            set(p2ImageViews[i])
            set(p2ToasterViews[i])
            set(p2StatusViews[i])
        }
        for (i in 0 until 6) {
            set(p1ImageViews[i])
            set(p1ToasterViews[i])
            set(p1StatusViews[i])
        }
    }

    private fun set(view: View?) {
        if (view == null) return
        view.bringToFront()
        if (battleLayoutMode == MODE_BATTLE_SINGLE) {
            view.scaleX = 1f
            view.scaleY = 1f
        } else {
            view.scaleX = 0.9f
            view.scaleY = 0.9f
        }
    }

    private fun fillNeededViews(pXImageViews: SparseArray<ImageView>, needed: Int, inLayout: Boolean, width: Int, height: Int) {
        val current = pXImageViews.size()
        if (current < needed) {
            for (i in current until needed) {
                val child = obtainImageView()
                pXImageViews.append(i, child)
                if (inLayout) {
                    addViewInLayout(child, -1, child.layoutParams)
                    measureChildInLayout(child, width, height)
                } else {
                    addView(child, -1, child.layoutParams)
                }
            }
        } else if (current > needed) {
            for (i in needed until current) {
                val child = pXImageViews[i]
                if (inLayout) {
                    removeViewInLayout(child)
                } else {
                    removeView(child)
                }
                pXImageViews.remove(i)
                cacheImageView(child)
            }
        }
    }

    private fun obtainImageView(): ImageView {
        if (imageViewCache.size > 0) return imageViewCache.removeAt(0)
        val imageView = ImageView(context)
        imageView.scaleType = ImageView.ScaleType.FIT_XY
        val layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.layoutParams = layoutParams
        return imageView
    }

    private fun cacheImageView(child: ImageView) {
        child.setImageDrawable(null)
        val layoutParams = child.layoutParams as LayoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams.width = layoutParams.height
        imageViewCache.add(child)
    }

    private fun fillNeededStatusViews(pXStatusViews: SparseArray<StatusView>, needed: Int, inLayout: Boolean, width: Int, height: Int) {
        val current = pXStatusViews.size()
        if (current < needed) {
            for (i in current until needed) {
                val child = StatusView(context)
                child.layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                pXStatusViews.append(i, child)
                if (inLayout) {
                    addViewInLayout(child, -1, child.layoutParams)
                    measureChildInLayout(child, width, height)
                } else {
                    addView(child, -1, child.layoutParams)
                }
            }
        } else if (current > needed) {
            for (i in needed until current) {
                val child = pXStatusViews[i]
                pXStatusViews.remove(i)
                if (inLayout) {
                    removeViewInLayout(child)
                } else {
                    removeView(child)
                }
            }
        }
    }

    private fun fillNeededToasterViews(pXToasterViews: SparseArray<ToasterView>, needed: Int, inLayout: Boolean, width: Int, height: Int) {
        val current = pXToasterViews.size()
        if (current < needed) {
            for (i in current until needed) {
                val child = ToasterView(context)
                child.layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                pXToasterViews.append(i, child)
                if (inLayout) {
                    addViewInLayout(child, -1, child.layoutParams)
                    measureChildInLayout(child, width, height)
                } else {
                    addView(child, -1, child.layoutParams)
                }
            }
        } else if (current > needed) {
            for (i in needed until current) {
                val child = pXToasterViews[i]
                pXToasterViews.remove(i)
                if (inLayout) {
                    removeViewInLayout(child)
                } else {
                    removeView(child)
                }
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (drawFx) {
            fxDrawable.draw(canvas)
        }
    }

    private class LayoutParams(width: Int, height: Int) : ViewGroup.LayoutParams(width, height)

    companion object {
        const val MODE_PREVIEW = 0
        const val MODE_BATTLE_SINGLE = 1
        const val MODE_BATTLE_DOUBLE = 2
        const val MODE_BATTLE_TRIPLE = 3

        private const val ASPECT_RATIO = 16f / 9f
        private val REL_TEAMPREV_P1_LINE = arrayOf(PointF(0.125f, 0.728f), PointF(0.820f, 0.806f))
        private val REL_TEAMPREV_P2_LINE = arrayOf(PointF(0.180f, 0.267f), PointF(0.875f, 0.344f))
        private val REL_BATTLE_P1_POS = arrayOf(arrayOf(PointF(0.225f, 0.748f)), arrayOf(PointF(0.225f, 0.738f), PointF(0.425f, 0.758f)), arrayOf(PointF(0.125f, 0.728f), PointF(0.325f, 0.748f), PointF(0.525f, 0.768f)))
        private val REL_BATTLE_P2_POS = arrayOf(arrayOf(PointF(0.775f, 0.397f)), arrayOf(PointF(0.775f, 0.297f), PointF(0.575f, 0.267f)), arrayOf(PointF(0.475f, 0.267f), PointF(0.675f, 0.287f), PointF(0.875f, 0.307f)))
    }
}