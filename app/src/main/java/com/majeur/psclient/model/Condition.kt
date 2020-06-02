package com.majeur.psclient.model

import com.majeur.psclient.model.Colors.statusColor

class Condition(rawCondition: String) {

    @JvmField val color: Int
    @JvmField var status: String?
    @JvmField val maxHp: Int
    @JvmField val hp: Int
    @JvmField val health: Float

    init {
        val separator = rawCondition.indexOf('/')
        if (separator == -1) {
            hp = 0
            maxHp = 100
            status = "fnt"
            health = 0f
            color = 0
        } else {
            hp = rawCondition.substring(0, separator).toInt()
            val sep = rawCondition.indexOf(' ')
            if (sep == -1) {
                maxHp = rawCondition.substring(separator + 1).toInt()
                status = null
            } else {
                maxHp = rawCondition.substring(separator + 1, sep).toInt()
                status = rawCondition.substring(sep + 1)
            }
            health = hp.toFloat() / maxHp.toFloat()
            color = statusColor(status)
        }
    }
}
