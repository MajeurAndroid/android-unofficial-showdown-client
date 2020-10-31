package com.majeur.psclient.util.glide

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.ViewPropertyAnimator
import android.widget.ImageView
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition

abstract class AnimatedImageViewTarget(im: ImageView)
    : CustomViewTarget<ImageView, Drawable>(im) {

    private var animatable: Animatable? = null

    protected fun setResource(resource: Drawable?) {
        getView().setImageDrawable(resource)
        if (resource is Animatable) {
            animatable = resource
            resource.start()
        } else {
            animatable = null
        }
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        setResource(errorDrawable)
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        setResource(placeholder)
    }

    override fun onResourceReady(resource: Drawable, unused: Transition<in Drawable>?) {
        setResourceInternal(resource)
    }

    override fun onStart() {
        super.onStart()
        animatable?.start()
    }

    override fun onStop() {
        super.onStop()
        animatable?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        animatable?.stop()
        animatable = null
    }

    private fun setResourceInternal(resource: Drawable?) {
        if (resource == null) return
        // Order matters here. Set the resource first to make sure that the Drawable has a valid and
        // non-null Callback before starting it.
        val viewPropertyAnimator = view.animate()
        onInitInAnimation(viewPropertyAnimator)
        viewPropertyAnimator.withEndAction {
            setResource(resource)
            onApplyResourceSize(resource.intrinsicWidth, resource.intrinsicHeight)
            onInitOutAnimation(viewPropertyAnimator)
            viewPropertyAnimator.withEndAction(null)
            viewPropertyAnimator.start()
        }
        viewPropertyAnimator.start()
    }

    protected abstract fun onInitInAnimation(viewPropertyAnimator: ViewPropertyAnimator)
    protected abstract fun onInitOutAnimation(viewPropertyAnimator: ViewPropertyAnimator)
    protected abstract fun onApplyResourceSize(w: Int, h: Int)
}