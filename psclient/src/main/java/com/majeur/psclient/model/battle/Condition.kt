package com.majeur.psclient.model.battle

import com.majeur.psclient.model.common.Colors.statusColor
import kotlin.math.roundToInt

class Condition(rawCondition: String) {

    var status: String?
    val maxHp: Int
    val hp: Int
    val health: Float
        get() = hp.toFloat() / maxHp.toFloat()
    val color: Int
        get() = statusColor(status)

    init {
        val rawHp = rawCondition.substringBefore(" ")
        val rawStatus = rawCondition.substringAfter(" ", "")

        var hp = -1; var maxHp = -1
        if (rawHp.toIntOrNull() == 0 || rawHp.toFloatOrNull() == 0f) {
            hp = 0
            maxHp = 100
        } else if (rawHp.contains("/")) {
            hp = rawHp.substringBefore("/").toFloatOrNull()?.roundToInt() ?: -1
            maxHp = rawHp.substringAfter("/").removeSuffix("g").removeSuffix("y")
                    .toFloatOrNull()?.roundToInt() ?: -1
        } else if (rawHp.toFloatOrNull() != null) {
            maxHp = 100
            hp = (maxHp * rawHp.toFloat() / 100f).roundToInt()
        }

        status = when {
            listOf("par", "brn", "slp", "frz", "tox", "psn").contains(rawStatus) -> rawStatus
            rawStatus == "fnt" -> { hp = 0; maxHp = 100; null }
            else -> null
        }

        this.hp = hp
        this.maxHp = maxHp
    }
}
