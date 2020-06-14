package com.majeur.psclient.model.battle

import com.majeur.psclient.model.common.Colors.typeColor
import com.majeur.psclient.util.toId
import org.json.JSONObject

class Move(val index: Int, json: JSONObject, zJson: JSONObject?, maxJson: JSONObject?) {

    val name = json.getString("move").replace("Hidden Power", "HP")
    val id: String = json.getString("id")
    val pp = json.optInt("pp", -1)
    val ppMax = json.optInt("maxpp", -1)
    val target = Target.parse(json.optString("target"))
    val disabled = json.optBoolean("disabled")
    val zName = zJson?.optString("move")
    var details: Details? = null
    var zDetails: Details? = null
    var maxMoveId = maxJson?.optString("move")
    var maxMoveTarget = if (maxJson != null) Target.parse(maxJson.optString("target")) else null
    var maxDetails: Details? = null

    // Flag to know if this move should be read as
    // z-move, max-move or regular move
    var zflag = false
    var maxflag = false

    val canZMove get() = zName != null

    val maxMoveName: String?
        get() = MAX_MOVES.firstOrNull { it.toId() == maxMoveId } ?: maxMoveId

    class Details {

        lateinit var name: String
        lateinit var category: String
        var desc: String? = null
        var accuracy = 0
        var priority = 0
        var basePower = 0
        var zPower = 0
        var color = 0
        var pp = 0
        var type: String? = null
            set(value) {
                field = value
                color = typeColor(value?.toId())
            }
        var target: Target? = null
        var zEffect: String? = null
            set(value) { field = zMoveEffects(value)
            }

        var maxPower = 0

        companion object {

            private fun zMoveEffects(effect: String?) = when (effect) {
                "clearnegativeboost" -> "Restores negative stat stages to 0"
                "crit2" -> "Crit ratio +2"
                "heal" -> "Restores HP 100%"
                "curse" -> "Restores HP 100% if user is Ghost type, otherwise Attack +1"
                "redirect" -> "Redirects opposing attacks to user"
                "healreplacement" -> "Restores replacement's HP 100%"
                else -> null

            }

        }
    }

    enum class Target {

        NORMAL, ALL_ADJACENT_FOES, SELF, ANY, ADJACENT_ALLY_OR_SELF, ALLY_TEAM, ADJACENT_ALLY,
        ALLY_SIDE, ALL_ADJACENT, SCRIPTED, ALL, ADJACENT_FOE, RANDOM_NORMAL, FOE_SIDE;

        val isChoosable: Boolean
            get() = this == NORMAL || this == ANY || this == ADJACENT_ALLY || this == ADJACENT_ALLY_OR_SELF
                    || this == ADJACENT_FOE

        companion object {

            fun parse(target: String) = when (target.toId()) {
                "normal" -> NORMAL
                "alladjacentfoes" -> ALL_ADJACENT_FOES
                "self" -> SELF
                "any" -> ANY
                "adjacentallyorself" -> ADJACENT_ALLY_OR_SELF
                "allyteam" -> ALLY_TEAM
                "adjacentally" -> ADJACENT_ALLY
                "allyside" -> ALLY_SIDE
                "alladjacent" -> ALL_ADJACENT
                "scripted" -> SCRIPTED
                "all" -> ALL
                "adjacentfoe" -> ADJACENT_FOE
                "randomnormal" -> RANDOM_NORMAL
                "foeside" -> FOE_SIDE
                else -> NORMAL
            }

            //  Triples       Doubles     Singles
            //  3  2  1         2  1         1
            // -1 -2 -3        -1 -2        -1
            fun computeTargetAvailabilities(target: Target, position: Int, pokeCount: Int): Array<BooleanArray> {
                val availabilities = Array(2) { BooleanArray(pokeCount) }
                for (i in 0..1) for (j in 0 until pokeCount) availabilities[i][j] = true
                if (target == ADJACENT_FOE) {
                    for (i in 0 until pokeCount) {
                        if (position != i && position != i + 1 && position != i - 1) availabilities[0][i] = false
                        availabilities[1][i] = false
                    }
                }
                if (target == ADJACENT_ALLY || target == ADJACENT_ALLY_OR_SELF) {
                    for (i in 0 until pokeCount) {
                        if (position != i && position != i + 1 && position != i - 1) availabilities[1][i] = false
                        availabilities[0][i] = false
                    }
                    if (target == ADJACENT_ALLY) availabilities[1][position] = false
                }
                if (target == NORMAL) {
                    availabilities[1][position] = false
                }
                return availabilities
            }
        }
    }

    companion object {
        private val MAX_MOVES = arrayOf("Max Guard", "Max Ooze", "Max Knuckle", "Max Darkness",
                "Max Overgrowth", "Max Strike", "Max Rockfall", "Max Steelspike", "Max Wyrmwind",
                "Max Lightning", "Max Geyser", "Max Flare", "Max Phantasm", "Max Flutterby", "Max Mindstorm",
                "Max Hailstorm", "Max Airstream", "Max Quake", "Max Starfall")
    }
}