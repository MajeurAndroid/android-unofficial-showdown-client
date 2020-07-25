package com.majeur.psclient.model.battle

import com.majeur.psclient.util.bold

class StatModifiers {

    private var atk = 0
        set(value) { field = value.coerceIn(-6, 6) }
    private var def = 0
        set(value) { field = value.coerceIn(-6, 6) }
    private var spa = 0
        set(value) { field = value.coerceIn(-6, 6) }
    private var spd = 0
        set(value) { field = value.coerceIn(-6, 6) }
    private var spe = 0
        set(value) { field = value.coerceIn(-6, 6) }
    private var eva = 0
        set(value) { field = value.coerceIn(-6, 6) }
    private var acc = 0
        set(value) { field = value.coerceIn(-6, 6) }

    operator fun get(stat: String?) = when (stat) {
        "atk" -> atk
        "def" -> def
        "spa" -> spa
        "spd" -> spd
        "spe" -> spe
        "evasion" -> eva
        "accuracy" -> acc
        else -> 0
    }

    fun inc(stat: String?, value: Int) {
        when (stat) {
            "atk" -> atk += value
            "def" -> def += value
            "spa" -> spa += value
            "spd" -> spd += value
            "spe" -> spe += value
            "evasion" -> eva += value
            "accuracy" -> acc += value
        }
    }

    operator fun set(stat: String?, value: Int) {
        when (stat) {
            "atk" -> atk = value
            "def" -> def = value
            "spa" -> spa = value
            "spd" -> spd = value
            "spe" -> spe = value
            "evasion" -> eva = value
            "accuracy" -> acc = value
        }
    }

    fun set(modifiers: StatModifiers) {
        atk = modifiers.atk
        def = modifiers.def
        spa = modifiers.spa
        spd = modifiers.spd
        spe = modifiers.spe
        eva = modifiers.eva
        acc = modifiers.acc
    }

    fun invert() {
        atk = -atk
        def = -def
        spa = -spa
        spd = -spd
        spe = -spe
        eva = -eva
        acc = -acc
    }

    fun clear() {
        acc = 0
        eva = 0
        spe = 0
        spd = 0
        spa = 0
        def = 0
        atk = 0
    }

    fun clearPositive() {
        if (atk > 0) atk = 0
        if (def > 0) def = 0
        if (spa > 0) spa = 0
        if (spd > 0) spd = 0
        if (spe > 0) spe = 0
        if (eva > 0) eva = 0
        if (acc > 0) acc = 0
    }

    fun clearNegative() {
        if (atk < 0) atk = 0
        if (def < 0) def = 0
        if (spa < 0) spa = 0
        if (spd < 0) spd = 0
        if (spe < 0) spe = 0
        if (eva < 0) eva = 0
        if (acc < 0) acc = 0
    }

    fun modifier(stat: String?): Float = when (stat) {
        "atk" -> LEVELS[atk + 6]
        "def" -> LEVELS[def + 6]
        "spa" -> LEVELS[spa + 6]
        "spd" -> LEVELS[spd + 6]
        "spe" -> LEVELS[spe + 6]
        "evasion" -> LEVELS_ALT[eva + 6]
        "accuracy" -> LEVELS_ALT[acc + 6]
        else -> 0f
    }


    fun calcReadableStat(stat: String?, baseStat: Int): CharSequence {
        val m = modifier(stat)
        if (m == 1f) return baseStat.toString()
        val afterModifier = (baseStat * m).toInt()
        return afterModifier.toString().bold()
    }

    companion object {
        @JvmStatic val STAT_KEYS = arrayOf("atk", "def", "spa", "spd", "spe", "evasion", "accuracy")
        private val LEVELS = floatArrayOf(1f / 4f, 2f / 7f, 1f / 3f, 2f / 5f, 1f / 2f, 2f / 3f, 1f, 3f / 2f, 2f, 5f / 2f, 3f, 7f / 2f, 4f)
        private val LEVELS_ALT = floatArrayOf(3f / 9f, 3f / 8f, 3f / 7f, 3f / 6f, 3f / 5f, 3f / 4f, 3f / 3f, 4f / 3f, 5f / 3f, 6f / 3f, 7f / 3f, 8f / 3f, 9f / 3f)
    }
}
