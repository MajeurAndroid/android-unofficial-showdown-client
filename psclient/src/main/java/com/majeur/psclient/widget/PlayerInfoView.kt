package com.majeur.psclient.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.majeur.psclient.R
import com.majeur.psclient.model.pokemon.BasePokemon
import com.majeur.psclient.util.addIfNotIn
import com.majeur.psclient.util.sp
import com.majeur.psclient.util.toId
import timber.log.Timber
import kotlin.math.roundToInt

@SuppressLint("RtlHardcoded")
class PlayerInfoView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val dexIconSize = sp(16f)
    private val spannableBuilder: SpannableStringBuilder
    private val pokeballDrawable: Drawable
    private val emptyPokeballDrawable: Drawable

    private val pokemonIds = mutableListOf<String>()

    private val isGravityRight
        get() = Gravity.getAbsoluteGravity(gravity, layoutDirection) and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.RIGHT

    init {
        spannableBuilder = SpannableStringBuilder(SUFFIX_PATTERN)
        pokeballDrawable = ContextCompat.getDrawable(context!!, R.drawable.ic_team_poke)!!
        pokeballDrawable.setBounds(0, 0, dexIconSize, dexIconSize)
        emptyPokeballDrawable = ContextCompat.getDrawable(context, R.drawable.ic_team_poke_empty)!!
        emptyPokeballDrawable.setBounds(0, 0, dexIconSize, dexIconSize)
    }

    fun clear() {
        pokemonIds.clear()
        spannableBuilder.clear()
        spannableBuilder.clearSpans()
        spannableBuilder.append(SUFFIX_PATTERN)
        text = null
    }

    fun setUsername(username: String) {
        val k = spannableBuilder.length - SUFFIX_PATTERN.length
        val start: Int
        if (isGravityRight) {
            if (k != 0) {
                start = SUFFIX_PATTERN.length - 1
                spannableBuilder.replace(start, spannableBuilder.length, username)
            } else {
                spannableBuilder.append(username)
                start = spannableBuilder.length - username.length
            }
        } else {
            start = if (k != 0) {
                spannableBuilder.replace(0, k, username)
                0
            } else {
                spannableBuilder.insert(0, username)
                0
            }
        }
        val potentialStyleSpans = spannableBuilder.getSpans(0, spannableBuilder.length, StyleSpan::class.java)
        if (potentialStyleSpans != null && potentialStyleSpans.isNotEmpty()) spannableBuilder.removeSpan(potentialStyleSpans[0])
        spannableBuilder.setSpan(StyleSpan(Typeface.BOLD), start, start + username.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        invalidateText()
    }

    fun setTeamSize(teamSize: Int) {
        pokemonIds.clear()
        val l = spannableBuilder.length
        for (span in spannableBuilder.getSpans(0, l, ImageSpan::class.java)) spannableBuilder.removeSpan(span)
        val k = spannableBuilder.length - MAX_TEAM_SIZE - SUFFIX_OFFSET
        if (isGravityRight) {
            for (i in SUFFIX_OFFSET until SUFFIX_OFFSET + teamSize) spannableBuilder.setSpan(ImageSpan(pokeballDrawable), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            for (i in SUFFIX_OFFSET + teamSize until SUFFIX_OFFSET + MAX_TEAM_SIZE) spannableBuilder.setSpan(ImageSpan(emptyPokeballDrawable), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        } else {
            for (i in k until k + teamSize) spannableBuilder.setSpan(ImageSpan(pokeballDrawable), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            for (i in k + teamSize until l - SUFFIX_OFFSET) spannableBuilder.setSpan(ImageSpan(emptyPokeballDrawable), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        invalidateText()
    }

    fun appendPokemon(pokemon: BasePokemon, dexIcon: Drawable) {
        Timber.d("Append poke ${pokemon.baseSpecies}")
        if (!pokemonIds.addIfNotIn(pokemon.baseSpecies.toId())) return
        val i = if (isGravityRight) SUFFIX_OFFSET + MAX_TEAM_SIZE - pokemonIds.size else spannableBuilder.length - MAX_TEAM_SIZE - SUFFIX_OFFSET + pokemonIds.size - 1
        val previousSpan = spannableBuilder.getSpans(i, i + 1, ImageSpan::class.java)[0]
        spannableBuilder.removeSpan(previousSpan)
        val aspectRatio = dexIcon.intrinsicWidth / dexIcon.intrinsicHeight.toFloat()
        dexIcon.setBounds(0, 0, (aspectRatio * dexIconSize).roundToInt(), dexIconSize)
        spannableBuilder.setSpan(ImageSpan(dexIcon), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        invalidateText()
    }

    fun updatePokemon(pokemon: BasePokemon, dexIcon: Drawable) {
        Timber.d("Update poke ${pokemon.baseSpecies}")
        if (!pokemonIds.contains(pokemon.baseSpecies.toId())) {
            appendPokemon(pokemon, dexIcon)
            return
        }
        val index = pokemonIds.indexOf(pokemon.baseSpecies.toId())
        val i = if (isGravityRight) SUFFIX_OFFSET + MAX_TEAM_SIZE - (index + 1) else spannableBuilder.length - MAX_TEAM_SIZE - SUFFIX_OFFSET + index
        val previousSpan = spannableBuilder.getSpans(i, i + 1, ImageSpan::class.java)[0]
        spannableBuilder.removeSpan(previousSpan)
        val aspectRatio = dexIcon.intrinsicWidth / dexIcon.intrinsicHeight.toFloat()
        dexIcon.setBounds(0, 0, (aspectRatio * dexIconSize).roundToInt(), dexIconSize)
        spannableBuilder.setSpan(ImageSpan(dexIcon), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        invalidateText()
    }

    fun setPokemonFainted(pokemon: BasePokemon?) {
        Timber.d("Faint poke ${pokemon?.baseSpecies ?: "null"}")
        if (pokemon == null || !pokemonIds.contains(pokemon.baseSpecies.toId())) return
        val index = pokemonIds.indexOf(pokemon.baseSpecies.toId())
        val i = if (isGravityRight) SUFFIX_OFFSET + MAX_TEAM_SIZE - (index + 1) else spannableBuilder.length - MAX_TEAM_SIZE - SUFFIX_OFFSET + index
        val previousSpan = spannableBuilder.getSpans(i, i + 1, ImageSpan::class.java)[0]
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        previousSpan.drawable.colorFilter = ColorMatrixColorFilter(matrix)
        invalidateText()
    }

    private fun invalidateText() {
        text = spannableBuilder
    }

    companion object {
        private const val SUFFIX_PATTERN = "  ------  "
        private const val SUFFIX_OFFSET = 2
        private const val MAX_TEAM_SIZE = 6
    }

}