package com.majeur.psclient.util.glide

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.ViewPropertyAnimator
import android.widget.ImageView
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.transition.Transition.ViewAdapter

abstract class AnimatedImageViewTarget(im: ImageView)
    : CustomViewTarget<ImageView, Drawable>(im), ViewAdapter {

    private var animatable: Animatable? = null

    override fun getCurrentDrawable(): Drawable? {
        return getView().drawable
    }

    override fun setDrawable(drawable: Drawable?) {
        if (drawable == null) return
        getView().setImageDrawable(drawable)
    }

    override fun onResourceLoading(placeholder: Drawable?) {
        super.onResourceLoading(placeholder)
        setResourceInternal(null)
        setDrawable(placeholder)
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        setResourceInternal(null)
        setDrawable(errorDrawable)
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        animatable?.stop()
        setResourceInternal(null)
        setDrawable(placeholder)
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        if (transition?.transition(resource, this) == false) {
            setResourceInternal(resource)
        } else {
            maybeUpdateAnimatable(resource)
        }
    }

    override fun onStart() {
        super.onStart()
        animatable?.start()
    }

    override fun onStop() {
        super.onStop()
        animatable?.stop()
    }


    private fun setResourceInternal(resource: Drawable?) {
        if (resource == null) return
        // Order matters here. Set the resource first to make sure that the Drawable has a valid and
        // non-null Callback before starting it.
        val viewPropertyAnimator = view.animate()
        onInitInAnimation(viewPropertyAnimator)
        viewPropertyAnimator.withEndAction {
            setResource(resource)
            maybeUpdateAnimatable(resource)
            onInitOutAnimation(viewPropertyAnimator)
            viewPropertyAnimator.withEndAction(null)
            viewPropertyAnimator.start()
        }
        viewPropertyAnimator.start()
    }

    private fun maybeUpdateAnimatable(resource: Drawable) {
        if (resource is Animatable) {
            animatable = resource
            resource.start()
        } else {
            animatable = null;
        }
    }


    protected abstract fun onInitInAnimation(viewPropertyAnimator: ViewPropertyAnimator)
    protected abstract fun onInitOutAnimation(viewPropertyAnimator: ViewPropertyAnimator)
    protected abstract fun setResource(resource: Drawable)
}