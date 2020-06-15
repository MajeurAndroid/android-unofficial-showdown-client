package com.majeur.psclient.model.common

import com.majeur.psclient.R
import java.util.*


object Type {

    val ALL = arrayOf(
            "Bug",
            "Dark",
            "Dragon",
            "Electric",
            "Fighting",
            "Fire",
            "Flying",
            "Ghost",
            "Grass",
            "Ground",
            "Ice",
            "Poison",
            "Psychic",
            "Rock",
            "Steel",
            "Water",
            "Normal",
            "Fairy"
    )

    val HP_TYPES = arrayOf(
            "Dark",
            "Bug",
            "Dragon",
            "Electric",
            "Fighting",
            "Fire",
            "Flying",
            "Ghost",
            "Grass",
            "Ground",
            "Ice",
            "Poison",
            "Psychic",
            "Rock",
            "Steel",
            "Water")

    fun getResId(rawType: String?) = when (rawType?.trim()?.toLowerCase(Locale.ROOT)) {
        "bug" -> R.drawable.ic_type_bug
        "dark" -> R.drawable.ic_type_dark
        "dragon" -> R.drawable.ic_type_dragon
        "electric" -> R.drawable.ic_type_electric
        "fighting" -> R.drawable.ic_type_fighting
        "fire" -> R.drawable.ic_type_fire
        "flying" -> R.drawable.ic_type_flying
        "ghost" -> R.drawable.ic_type_ghost
        "grass" -> R.drawable.ic_type_grass
        "ground" -> R.drawable.ic_type_ground
        "ice" -> R.drawable.ic_type_ice
        "poison" -> R.drawable.ic_type_poison
        "psychic" -> R.drawable.ic_type_psychic
        "rock" -> R.drawable.ic_type_rock
        "steel" -> R.drawable.ic_type_steel
        "water" -> R.drawable.ic_type_water
        "normal" -> R.drawable.ic_type_normal
        "fairy" -> R.drawable.ic_type_fairy
        else -> R.drawable.ic_type_unknown
    }
}
