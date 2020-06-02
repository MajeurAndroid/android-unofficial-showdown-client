package com.majeur.psclient.model

import android.graphics.Color

object Colors {

    const val BLACK = Color.BLACK
    const val WHITE = Color.WHITE
    const val GREEN = 0xFF12D600.toInt()
    const val RED = 0xFFD90000.toInt()
    const val BLUE = Color.BLUE
    const val YELLOW = Color.YELLOW
    const val GRAY = 0xFF636363.toInt()

    const val STAT_BOOST = BLUE
    const val STAT_UNBOOST = RED
    const val VOLATILE_STATUS = 0xFF6F35FC.toInt()
    const val VOLATILE_GOOD = 0xFF33AA00.toInt()
    const val VOLATILE_NEUTRAL = 0xFF555555.toInt()
    const val VOLATILE_BAD = 0xFFFF4400.toInt()

    const val CATEGORY_PHYSICAL = 0xFFEB5628.toInt()
    const val CATEGORY_PHY_INNER = 0xFFFFF064.toInt()
    const val CATEGORY_SPECIAL = 0xFF2260C2.toInt()
    const val CATEGORY_STATUS = 0xFF9A9997.toInt()

    const val TYPE_NORMAL = 0xFFA8A77A.toInt()
    const val TYPE_FIRE = 0xFFEE8130.toInt()
    const val TYPE_WATER = 0xFF6390F0.toInt()
    const val TYPE_ELECTRIC = 0xFFF7D02C.toInt()
    const val TYPE_GRASS = 0xFF7AC74C.toInt()
    const val TYPE_ICE = 0xFF7ac7c4.toInt()
    const val TYPE_FIGHTING = 0xFFC22E28.toInt()
    const val TYPE_POISON = 0xFFA33EA1.toInt()
    const val TYPE_GROUND = 0xFFE2BF65.toInt()
    const val TYPE_FLYING = 0xFFA98FF3.toInt()
    const val TYPE_PSYCHIC = 0xFFF95587.toInt()
    const val TYPE_BUG = 0xFFA6B91A.toInt()
    const val TYPE_ROCK = 0xFFB6A136.toInt()
    const val TYPE_GHOST = 0xFF735797.toInt()
    const val TYPE_DRAGON = 0xFF6F35FC.toInt()
    const val TYPE_DARK = 0xFF705746.toInt()
    const val TYPE_STEEL = 0xFFB7B7CE.toInt()
    const val TYPE_FAIRY = 0xFFD685AD.toInt()

    @JvmStatic
    fun typeColor(type: String?) = when (type) {
        "normal" -> TYPE_NORMAL
        "fire" -> TYPE_FIRE
        "water" -> TYPE_WATER
        "electric" -> TYPE_ELECTRIC
        "grass" -> TYPE_GRASS
        "ice" -> TYPE_ICE
        "fighting" -> TYPE_FIGHTING
        "poison" -> TYPE_POISON
        "ground" -> TYPE_GROUND
        "flying" -> TYPE_FLYING
        "psychic" -> TYPE_PSYCHIC
        "bug" -> TYPE_BUG
        "rock" -> TYPE_ROCK
        "ghost" -> TYPE_GHOST
        "dragon" -> TYPE_DRAGON
        "dark" -> TYPE_DARK
        "steel" -> TYPE_STEEL
        "fairy" -> TYPE_FAIRY
        else -> 0
    }

    @JvmStatic
    fun statusColor(status: String?) = when (status) {
        "psn", "tox" -> TYPE_POISON
        "brn" -> TYPE_FIRE
        "frz" -> TYPE_ICE
        "par" -> TYPE_ELECTRIC
        "slp" -> TYPE_NORMAL
        else -> 0
    }

    @JvmStatic
    fun sideColor(side: String?) = when (side) {
        "AV" -> TYPE_ICE
        "LS" -> TYPE_PSYCHIC
        "MI" -> TYPE_ICE
        "RE" -> TYPE_PSYCHIC
        "SG" -> TYPE_NORMAL
        "SP" -> TYPE_GROUND
        "SR" -> TYPE_ROCK
        "SW" -> TYPE_BUG
        "TA" -> TYPE_FLYING
        "TS" -> TYPE_POISON
        else -> BLACK
    }

    @JvmStatic
    fun healthColor(health: Float) = if (health > 0.5f) GREEN else if (health > 0.2f) YELLOW else RED

    @JvmStatic
    fun categoryColor(cat: String?) = when (cat) {
        "physical" -> CATEGORY_PHYSICAL
        "special" -> CATEGORY_SPECIAL
        "status" -> CATEGORY_STATUS
        else -> 0
    }
}
