package com.majeur.psclient.model.common


class Nature private constructor(val name: String, val plus: String, val minus: String) {

    companion object {
        val Adamant = Nature("Adamant", "atk", "spa")
        val Bashful = Nature("Bashful", "", "")
        val Bold = Nature("Bold", "def", "atk")
        val Brave = Nature("Brave", "atk", "spe")
        val Calm = Nature("Calm", "spd", "atk")
        val Careful = Nature("Careful", "spd", "spa")
        val Docile = Nature("Docile", "", "")
        val Gentle = Nature("Gentle", "spd", "def")
        val Hardy = Nature("Hardy", "", "")
        val Hasty = Nature("Hasty", "spe", "def")
        val Impish = Nature("Impish", "def", "spa")
        val Jolly = Nature("Jolly", "spe", "spa")
        val Lax = Nature("Lax", "def", "spd")
        val Lonely = Nature("Lonely", "atk", "def")
        val Mild = Nature("Mild", "spa", "def")
        val Modest = Nature("Modest", "spa", "atk")
        val Naive = Nature("Naive", "spe", "spd")
        val Naughty = Nature("Naughty", "atk", "spd")
        val Quiet = Nature("Quiet", "spa", "spe")
        val Quirky = Nature("Quirky", "", "")
        val Rash = Nature("Rash", "spa", "spd")
        val Relaxed = Nature("Relaxed", "def", "spe")
        val Sassy = Nature("Sassy", "spd", "spe")
        val Serious = Nature("Serious", "", "")
        val Timid = Nature("Timid", "spe", "atk")
        @JvmStatic val DEFAULT = Serious

        // Ordered
        val ALL = arrayOf(Serious, Bashful, Bold, Brave, Calm, Careful, Docile,
                Gentle, Hardy, Hasty, Impish, Jolly, Lax, Lonely, Mild, Modest,
                Naive, Naughty, Quiet, Quirky, Rash, Relaxed, Sassy, Adamant, Timid)
    }

    private var atk = 1f
    private var def = 1f
    private var spa = 1f
    private var spd = 1f
    private var spe = 1f

    fun getStatModifier(index: Int): Float {
        return when (index) {
            1 -> atk
            2 -> def
            3 -> spa
            4 -> spd
            5 -> spe
            else -> 0f
        }
    }

    override fun toString(): String {
        return if (plus.isNotEmpty() && minus.isNotEmpty()) "$name (+$plus/-$minus)" else name
    }

    init {
        when (plus) {
            "atk" -> atk += 0.1f
            "def" -> def += 0.1f
            "spa" -> spa += 0.1f
            "spd" -> spd += 0.1f
            "spe" -> spe += 0.1f
        }
        when (minus) {
            "atk" -> atk -= 0.1f
            "def" -> def -= 0.1f
            "spa" -> spa -= 0.1f
            "spd" -> spd -= 0.1f
            "spe" -> spe -= 0.1f
        }
    }
}