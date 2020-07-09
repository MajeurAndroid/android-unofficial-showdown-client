package com.majeur.psclient.model.common

import android.text.SpannableStringBuilder
import com.majeur.psclient.util.concat
import com.majeur.psclient.util.small
import com.majeur.psclient.util.toId
import org.json.JSONObject
import java.io.Serializable
import java.util.*


class Stats() : Serializable {

    var hp = 0
    var atk = 0
    var def = 0
    var spa = 0
    var spd = 0
    var spe = 0

    val array
        get() = intArrayOf(hp, atk, def, spa, spd, spe)

    constructor(json: JSONObject) : this() {
        hp = json.optInt("hp", 0)
        atk = json.getInt("atk")
        def = json.getInt("def")
        spa = json.getInt("spa")
        spd = json.getInt("spd")
        spe = json.getInt("spe")
    }

    constructor(hp: Int, atk: Int, def: Int, spa: Int, spd: Int, spe: Int) : this() {
        this.hp = hp
        this.atk = atk
        this.def = def
        this.spa = spa
        this.spd = spd
        this.spe = spe
    }

    constructor(defaultValue: Int) : this() {
        hp = defaultValue
        atk = defaultValue
        def = defaultValue
        spa = defaultValue
        spd = defaultValue
        spe = defaultValue
    }

    fun setAll(value: Int) {
        hp = value
        atk = value
        def = value
        spa = value
        spd = value
        spe = value
    }

    fun set(stats: Stats) {
        hp = stats.hp
        atk = stats.atk
        def = stats.def
        spa = stats.spa
        spd = stats.spd
        spe = stats.spe
    }

    fun set(name: String, value: Int) = set(toIndex(name), value)

    fun set(index: Int, value: Int) = when (index) {
        0 -> hp = value
        1 -> atk = value
        2 -> def = value
        3 -> spa = value
        4 -> spd = value
        5 -> spe = value
        else -> Unit
    }

    fun get(index: Int) = array[index]

    fun sum() = hp + atk + def + spa + spd + spe

    fun hpType(): String {
        val a = if (hp % 2 == 0) 0 else 1
        val b = if (atk % 2 == 0) 0 else 2
        val c = if (def % 2 == 0) 0 else 4
        val d = if (spe % 2 == 0) 0 else 8
        val e = if (spa % 2 == 0) 0 else 16
        val f = if (spd % 2 == 0) 0 else 32
        val t = (a + b + c + d + e + f) * 15 / 63
        return when (t) {
            0 -> "Fighting"
            1 -> "Flying"
            2 -> "Poison"
            3 -> "Ground"
            4 -> "Rock"
            5 -> "Bug"
            6 -> "Ghost"
            7 -> "Steel"
            8 -> "Fire"
            9 -> "Water"
            10 -> "Grass"
            11 -> "Electric"
            12 -> "Psychic"
            13 -> "Ice"
            14 -> "Dragon"
            15 -> "Dark"
            else -> ""
        }
    }

    fun setForHpType(type: String?) {
        when (type ?: "Dark") { // Dark is default all 31 iv spread
            "Bug" -> {
                hp = 31
                atk = 31
                def = 31
                spa = 31
                spd = 30
                spe = 30
            }
            "Dark" -> {
                hp = 31
                atk = 31
                def = 31
                spa = 31
                spd = 31
                spe = 31
            }
            "Dragon" -> {
                hp = 30
                atk = 31
                def = 31
                spa = 31
                spd = 31
                spe = 31
            }
            "Electric" -> {
                hp = 31
                atk = 31
                def = 31
                spa = 30
                spd = 31
                spe = 31
            }
            "Fighting" -> {
                hp = 31
                atk = 31
                def = 30
                spa = 30
                spd = 30
                spe = 30
            }
            "Fire" -> {
                hp = 31
                atk = 30
                def = 31
                spa = 30
                spd = 31
                spe = 30
            }
            "Flying" -> {
                hp = 31
                atk = 31
                def = 31
                spa = 30
                spd = 30
                spe = 30
            }
            "Ghost" -> {
                hp = 31
                atk = 30
                def = 31
                spa = 31
                spd = 30
                spe = 31
            }
            "Grass" -> {
                hp = 30
                atk = 31
                def = 31
                spa = 30
                spd = 31
                spe = 31
            }
            "Ground" -> {
                hp = 31
                atk = 31
                def = 31
                spa = 30
                spd = 30
                spe = 31
            }
            "Ice" -> {
                hp = 31
                atk = 31
                def = 31
                spa = 31
                spd = 31
                spe = 30
            }
            "Poison" -> {
                hp = 31
                atk = 31
                def = 30
                spa = 30
                spd = 30
                spe = 31
            }
            "Psychic" -> {
                hp = 30
                atk = 31
                def = 31
                spa = 31
                spd = 31
                spe = 30
            }
            "Rock" -> {
                hp = 31
                atk = 31
                def = 30
                spa = 31
                spd = 30
                spe = 30
            }
            "Steel" -> {
                hp = 31
                atk = 31
                def = 31
                spa = 31
                spd = 30
                spe = 31
            }
            "Water" -> {
                hp = 31
                atk = 31
                def = 31
                spa = 30
                spd = 31
                spe = 30
            }
        }
    }

    fun summaryText(nature: Nature = Nature.DEFAULT): CharSequence {
        val a = arrayOf(nature.plus, nature.minus)
        return (0 until 6).filter { i -> get(i) != 0 || a.contains(getName(i)!!.toId()) }
                .joinTo(SpannableStringBuilder()) { i ->
                    val name = getName(i)!!
                    val natSign = if (name.toId() == nature.plus) "+" else if (name.toId() == nature.minus) "-" else ""
                    "${get(i)}" concat  natSign.small()  concat name.small()
                }
    }

    companion object {

        @JvmStatic
        fun getName(index: Int) = when (index) {
            0 -> "HP"
            1 -> "Atk"
            2 -> "Def"
            3 -> "SpA"
            4 -> "Spd"
            5 -> "Spe"
            else -> null
        }

        @JvmStatic
        fun checkHpType(type: String?) = when (type?.trim()?.toLowerCase(Locale.ROOT)) {
            "bug" -> true
            "dark" -> true
            "dragon" -> true
            "electric" -> true
            "fighting" -> true
            "fire" -> true
            "flying" -> true
            "ghost" -> true
            "grass" -> true
            "ground" -> true
            "ice" -> true
            "poison" -> true
            "psychic" -> true
            "rock" -> true
            "steel" -> true
            "water" -> true
            else -> false
        }

        fun calculateSpeedRange(level: Int, baseSpe: Int, tier: String, gen: Int): IntArray {
            val isRandomBattle = tier.contains("Random Battle") || tier.contains("Random") && tier.contains("Battle") && gen >= 6
            val minNature = if (isRandomBattle || gen < 3) 1f else 0.9f
            val maxNature = if (isRandomBattle || gen < 3) 1f else 1.1f
            val maxIv = if (gen < 3) 30 else 31
            val min: Int
            var max: Int
            if (tier.contains("Let's Go")) {
                min = tr(tr(tr(2 * baseSpe * level / 100f + 5) * minNature) * tr((70f / 255f / 10f + 1) * 100) / 100f)
                max = tr(tr(tr((2 * baseSpe + maxIv) * level / 100f + 5) * maxNature) * tr((70f / 255f / 10f + 1) * 100) / 100f)
                if (tier.contains("No Restrictions")) max += 200 else if (tier.contains("Random")) max += 20
            } else {
                val maxIvEvOffset = maxIv + (if (isRandomBattle && gen >= 3) 21 else 63).toFloat()
                min = tr(tr(2 * baseSpe * level / 100f + 5) * minNature)
                max = tr(tr((2 * baseSpe + maxIvEvOffset) * level / 100f + 5) * maxNature)
            }
            return intArrayOf(min, max)
        }

        private fun tr(value: Float): Int {
            return value.toInt()
        }

        @JvmStatic
        fun calculateStat(base: Int, iv: Int, ev: Int, niv: Int, nat: Float): Int {
            return (((2 * base + iv + ev / 4) * niv / 100 + 5) * nat).toInt()
        }

        @JvmStatic
        fun calculateHp(base: Int, iv: Int, ev: Int, niv: Int): Int {
            return (2 * base + iv + ev / 4) * niv / 100 + niv + 10
        }

        fun toIndex(namee: String): Int {
            val name = namee.toLowerCase(Locale.ROOT).trim()
            if (name.contains("atk") || name.contains("attack")) {
                return if (name.contains("sp")) 3 else 1
            } else if (name.contains("def")) {
                return if (name.contains("sp")) 4 else 2
            } else if (name.contains("sp")) {
                return 5
            } else if (name.contains("h")) {
                return 0
            }
            return -1
        }
    }
}
