package com.majeur.psclient.io

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.majeur.psclient.R
import com.majeur.psclient.model.battle.Player
import com.majeur.psclient.model.pokemon.BasePokemon
import com.majeur.psclient.model.pokemon.BattlingPokemon
import com.majeur.psclient.util.Utils
import com.majeur.psclient.util.glide.AnimatedImageViewTarget
import com.majeur.psclient.util.html.Html
import com.majeur.psclient.util.minusFirst
import com.majeur.psclient.widget.BattleLayout
import java.util.concurrent.ExecutionException
import kotlin.math.roundToInt

class GlideHelper(context: Context) {

    enum class SpriteType(private val path: String, private val ext: String) {

        D3ANIMATED("ani", "gif"), // Gen 6+ 3D animated
        D2ANIMATED("gen5ani", "gif"), // Gen 5 2D animated
        D2("gen5", "png"), // Gen 5 2D non animated
        DEX("dex", "png"),
        TRAINER("trainers", "png"); // Dex

        fun uri(spriteId: String, shiny: Boolean, back: Boolean): Uri = Uri.Builder().run {
            scheme("https")
            authority("play.pokemonshowdown.com")
            appendPath("sprites")
            var dir = path
            if (back) dir += "-back"
            if (shiny) dir += "-shiny"
            appendPath(dir)
            appendPath("$spriteId.$ext")
            build()
        }
    }

    companion object {
        private const val MAGIC_SCALE = 0.0027777777777778f
    }

    private val glide = Glide.with(context)

    fun loadBattleSprite(pokemon: BattlingPokemon, imageView: ImageView) {
        val spriteId = pokemon.transformSpecies ?: pokemon.spriteId
        loadSprite(spriteId, pokemon.trainer, pokemon.shiny, true,
                SpriteType.D3ANIMATED, SpriteType.D2ANIMATED, SpriteType.D2)
            .into(object : AnimatedImageViewTarget(imageView) {
            override fun onInitInAnimation(viewPropertyAnimator: ViewPropertyAnimator) {
                viewPropertyAnimator
                        .setDuration(250)
                        .setInterpolator(DecelerateInterpolator())
                        .scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f)
            }

            override fun onInitOutAnimation(viewPropertyAnimator: ViewPropertyAnimator) {
                viewPropertyAnimator
                        .setDuration(250)
                        .setInterpolator(AccelerateInterpolator())
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
            }

            override fun setResource(resource: Drawable) {
                val fieldWidth = (imageView.parent as BattleLayout).width
                var scale = fieldWidth * MAGIC_SCALE
                if (!pokemon.foe) scale *= 1.5f
                imageView.layoutParams.apply {
                    width = (resource.intrinsicWidth * scale).roundToInt()
                    height = (resource.intrinsicHeight * scale).roundToInt()
                }
                imageView.setImageDrawable(resource) // Will request a layout
            }
        })
    }

    fun loadPreviewSprite(player: Player, pokemon: BasePokemon, imageView: ImageView) {
        loadSprite(pokemon.spriteId, player == Player.TRAINER, false, true,
                SpriteType.D3ANIMATED, SpriteType.D2ANIMATED, SpriteType.D2)
                .into(object : AnimatedImageViewTarget(imageView) {
                    override fun onInitInAnimation(viewPropertyAnimator: ViewPropertyAnimator) = Unit
                    override fun onInitOutAnimation(viewPropertyAnimator: ViewPropertyAnimator) = Unit

                    override fun setResource(resource: Drawable) {
                        val fieldWidth = (imageView.parent as BattleLayout?)?.width ?: 0
                        val scale = fieldWidth * MAGIC_SCALE
                        imageView.layoutParams.apply {
                            width = (resource.intrinsicWidth * scale).roundToInt()
                            height = (resource.intrinsicHeight * scale).roundToInt()
                        }
                        imageView.setImageDrawable(resource) // Will request a layout
                    }
                })
    }

    fun loadDexSprite(pokemon: BasePokemon, shiny: Boolean, imageView: ImageView) {
        loadSprite(pokemon.spriteId, false, shiny, true, SpriteType.DEX, SpriteType.D2)
                .into(imageView)
    }

    fun loadAvatar(avatar: String, imageView: ImageView) {
        loadSprite(avatar, false, false, false, SpriteType.TRAINER)
                .into(imageView)
    }

    // Load sprite trying with each sprite type if previous has failed
    private fun loadSprite(spriteId: String, back: Boolean, shiny: Boolean,
                           overrideSize: Boolean, vararg spriteTypes: SpriteType): RequestBuilder<Drawable> {
        val options = RequestOptions().apply {
            if (overrideSize) override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            if (spriteTypes.size == 1) error(R.drawable.missingno) // No more sprite types, default fallback
        }
        return glide.load(spriteTypes.first().uri(spriteId, shiny, back)).apply {
            apply(options)
            if (spriteTypes.size > 1) // There is more sprite types, add an error fallback
                error(loadSprite(spriteId, back, shiny, overrideSize, *spriteTypes.minusFirst()))
        }
    }

    fun getHtmlImageGetter(iconLoader: AssetLoader, maxWidth: Int): Html.ImageGetter {
        val mw = maxWidth - Utils.dpToPx(2f)
        return Html.ImageGetter { source, reqw, reqh ->
            try {
                var d: Drawable? = null
                if (source.startsWith("content://com.majeur.psclient/dex-icon/")) {
                    val species = source.substring(source.lastIndexOf('/') + 1, source.length)
                    val icon = iconLoader.dexIconNonSuspend(species)
                    if (icon != null) d = BitmapDrawable(icon)
                } else {
                    d = glide.asDrawable().load(source).submit().get()
                }
                if (d == null) return@ImageGetter null
                val r = d.intrinsicWidth / d.intrinsicHeight.toFloat()
                var w: Int
                var h: Int
                if (reqw != 0 && reqh == 0) {
                    w = reqw
                    h = (w / r).toInt()
                } else if (reqw == 0 && reqh != 0) {
                    h = reqh
                    w = (h * r).toInt()
                } else {
                    w = reqw
                    h = reqh
                }
                val mr = w / mw.toFloat()
                if (mr > 1) {
                    w = mw
                    h /= mr.toInt()
                }
                d.setBounds(0, 0, w, h)
                return@ImageGetter d
            } catch (e: ExecutionException) {
                e.printStackTrace()
                return@ImageGetter null
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return@ImageGetter null
            }
        }
    }
}