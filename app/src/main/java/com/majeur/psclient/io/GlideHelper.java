package com.majeur.psclient.io;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ViewTarget;
import com.majeur.psclient.R;
import com.majeur.psclient.model.BattlingPokemon;
import com.majeur.psclient.model.Player;
import com.majeur.psclient.util.Utils;

import static com.majeur.psclient.model.Player.FOE;

public class GlideHelper {

    static {
        ViewTarget.setTagId(R.id.glide_tag);
    }

    private RequestManager mRequestManager;

    public GlideHelper(Context context) {
        mRequestManager = Glide.with(context);
    }

    private final float MAGIC_SCALE = 0.0027777777777778f;

    @SuppressWarnings("CheckResult")
    public void loadSprite(final BattlingPokemon pokemon, final ImageView imageView, final int fieldWidth) {
        RequestBuilder<Drawable> request = mRequestManager.load(spriteUri(pokemon.species, pokemon.foe, pokemon.shiny));
        request.apply(new RequestOptions().error(R.drawable.missingno));
        request.into(new AnimatedImageViewTarget(imageView) {
            @Override
            protected void onInitInAnimation(ViewPropertyAnimator viewPropertyAnimator) {
                viewPropertyAnimator
                        .setDuration(250)
                        .setInterpolator(new DecelerateInterpolator())
                        .scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f);
            }

            @Override
            protected void onInitOutAnimation(ViewPropertyAnimator viewPropertyAnimator) {
                viewPropertyAnimator
                        .setDuration(250)
                        .setInterpolator(new AccelerateInterpolator())
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f);
            }

            @Override
            protected void setResource(Drawable resource) {
                int scale = Math.round(fieldWidth * MAGIC_SCALE);
                if (!pokemon.foe) scale = Math.round(scale * 1.5f);

                ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                layoutParams.width = resource.getIntrinsicWidth() * scale;
                layoutParams.height = resource.getIntrinsicHeight() * scale;

                imageView.setImageDrawable(resource);
            }
        });
    }

    public void loadPreviewSprite(Player player, String species, ImageView imageView) {
        RequestBuilder<Drawable> request = mRequestManager.load(spriteUri(species, player == FOE, false));
        request.apply(new RequestOptions().error(R.drawable.missingno));
        request.into(imageView);
    }

    public void loadTypeSprite(String type, ImageView imageView) {
        RequestBuilder<Drawable> request = mRequestManager.load(typeSpriteUri(type));
        request.into(imageView);
    }

    public void loadCategorySprite(String category, ImageView imageView) {
        RequestBuilder<Drawable> request = mRequestManager.load(categorySpriteUri(category));
        request.into(imageView);
    }

    public void loadDexSprite(String species, boolean shiny, ImageView imageView) {
        RequestBuilder<Drawable> request = mRequestManager.load(dexSpriteUri(species, shiny));
        request.apply(new RequestOptions().error(R.drawable.placeholder_pokeball));
        request.into(imageView);
    }

    public void loadAvatar(String avatar, ImageView imageView) {
        RequestBuilder<Drawable> request = mRequestManager.load(avatarSpriteUri(avatar));
        request.into(imageView);
    }

    private StringBuilder baseUri() {
        return new StringBuilder()
                .append("https://play.pokemonshowdown.com/sprites/");
    }

    private String spriteUri(String species, boolean foe, boolean shiny) {
        return baseUri()
                .append(foe ? "xyani" : "xyani-back")
                .append(shiny ? "-shiny/" : "/")
                .append(species.toLowerCase())
                .append(".gif")
                .toString();
    }

    private String dexSpriteUri(String species, boolean shiny) {
        return baseUri()
                .append("xydex")
                .append(shiny ? "-shiny/" : "/")
                .append(species.toLowerCase().replace("%", "").replace("-", "").replace(" ", ""))
                .append(".png")
                .toString();
    }

    private String typeSpriteUri(String type) {
        return baseUri()
                .append("types/")
                .append(Utils.firstCharUpperCase(type))
                .append(".png")
                .toString();
    }

    private String categorySpriteUri(String category) {
        return baseUri()
                .append("categories/")
                .append(Utils.firstCharUpperCase(category))
                .append(".png")
                .toString();
    }

    private String avatarSpriteUri(String avatar) {
        return baseUri()
                .append("trainers/")
                .append(avatar)
                .append(".png")
                .toString();
    }
}
