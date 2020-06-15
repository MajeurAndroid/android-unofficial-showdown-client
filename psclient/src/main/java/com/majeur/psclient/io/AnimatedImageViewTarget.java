package com.majeur.psclient.io;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;

@SuppressWarnings("WeakerAccess")
public abstract class AnimatedImageViewTarget extends ViewTarget<ImageView, Drawable>
        implements Transition.ViewAdapter {

    private Animatable animatable;

    public AnimatedImageViewTarget(ImageView view) {
        super(view);
    }

    @Override
    public Drawable getCurrentDrawable() {
        return view.getDrawable();
    }

    @Override
    public void setDrawable(Drawable drawable) {
        if (drawable == null) return;
        view.setImageDrawable(drawable);
    }


    @Override
    public void onLoadStarted(Drawable placeholder) {
        super.onLoadStarted(placeholder);
        setResourceInternal(null);
        setDrawable(placeholder);
    }

    @Override
    public void onLoadFailed(Drawable errorDrawable) {
        super.onLoadFailed(errorDrawable);
        setResourceInternal(null);
        setDrawable(errorDrawable);
    }

    @Override
    public void onLoadCleared(Drawable placeholder) {
        super.onLoadCleared(placeholder);
        if (animatable != null) {
            animatable.stop();
        }
        setResourceInternal(null);
        setDrawable(placeholder);
    }

    @Override
    public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
        if (transition == null || !transition.transition(resource, this)) {
            setResourceInternal(resource);
        } else {
            maybeUpdateAnimatable(resource);
        }
    }

    @Override
    public void onStart() {
        if (animatable != null) {
            animatable.start();
        }
    }

    @Override
    public void onStop() {
        if (animatable != null) {
            animatable.stop();
        }
    }

    private void setResourceInternal(final Drawable resource) {
        if (resource == null) return;
        // Order matters here. Set the resource first to make sure that the Drawable has a valid and
        // non-null Callback before starting it.
        ViewPropertyAnimator viewPropertyAnimator = view.animate();
        onInitInAnimation(viewPropertyAnimator);
        viewPropertyAnimator.withEndAction(new Runnable() {
            @Override
            public void run() {
                setResource(resource);
                maybeUpdateAnimatable(resource);
                ViewPropertyAnimator viewPropertyAnimator = view.animate();
                onInitOutAnimation(viewPropertyAnimator);
                viewPropertyAnimator.withEndAction(null);
                viewPropertyAnimator.start();
            }
        });
        viewPropertyAnimator.start();
    }

    private void maybeUpdateAnimatable(Drawable resource) {
        if (resource instanceof Animatable) {
            animatable = (Animatable) resource;
            animatable.start();
        } else {
            animatable = null;
        }
    }

    protected abstract void onInitInAnimation(ViewPropertyAnimator viewPropertyAnimator);

    protected abstract void onInitOutAnimation(ViewPropertyAnimator viewPropertyAnimator);

    protected abstract void setResource(Drawable resource);
}

