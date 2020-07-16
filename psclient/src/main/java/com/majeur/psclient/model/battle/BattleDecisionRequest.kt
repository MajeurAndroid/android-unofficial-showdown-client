package com.majeur.psclient.model.battle

import com.majeur.psclient.model.pokemon.SidePokemon
import org.json.JSONObject


class BattleDecisionRequest(json: JSONObject, var gameType: GameType?) {

    val count: Int
        get() = when (gameType) {
            GameType.SINGLE -> 1
            GameType.DOUBLE -> 2
            GameType.TRIPLE -> 3
            else -> throw IllegalStateException("Request count can not be known without game type")
        }

    val id = json.getInt("rqid")
    val teamPreview = json.optBoolean("teamPreview", false)
    val shouldWait = json.optBoolean("wait", false)
    val side = json.getJSONObject("side").getJSONArray("pokemon").run {
        (0 until length()).map { SidePokemon(it, getJSONObject(it)) }
    }

    private var forceSwitch = json.optJSONArray("forceSwitch")?.run {
        (0 until length()).map { getBoolean(it) }.toBooleanArray()
    }
    private var trapped: BooleanArray? = null
    private var canMegaEvo: BooleanArray? = null
    private var canDynamax: BooleanArray? = null
    private var moves: Array<Array<Move>>? = null

    init {
        json.optJSONArray("active")?.run {
            for (i in 0 until length()) {
                getJSONObject(i).let { active ->
                    var value = active.optBoolean("trapped", false)
                    if (value) {
                        if (trapped == null) trapped = BooleanArray(length())
                        trapped!![i] = true
                    }
                    value = active.optBoolean("canMegaEvo", false)
                    if (value) {
                        if (canMegaEvo == null) canMegaEvo = BooleanArray(length())
                        canMegaEvo!![i] = true
                    }
                    value = active.optBoolean("canDynamax", false)
                    if (value) {
                        if (canDynamax == null) canDynamax = BooleanArray(length())
                        canDynamax!![i] = true
                    }
                    if (moves == null) moves = Array(length()) { emptyArray<Move>() }
                    val movesJson = active.getJSONArray("moves")
                    val canZMove = active.optJSONArray("canZMove")
                    val maxMoves = active.optJSONObject("maxMoves")?.optJSONArray("maxMoves")
                    moves!![i] = (0 until movesJson.length()).map { j ->
                        Move(j, movesJson.getJSONObject(j),
                                canZMove?.optJSONObject(j),
                                maxMoves?.optJSONObject(j))
                    }.toTypedArray()
                }
            }
        }
    }

    fun getMoves(which: Int): Array<Move>? = if (moves != null) moves!![which] else null

    fun shouldPass(which: Int) = !forceSwitch(which) && getMoves(which) == null

    fun forceSwitch(which: Int) = if (forceSwitch != null) forceSwitch!![which] else false

    fun trapped(which: Int) = if (trapped != null) trapped!![which] else false

    fun canMegaEvo(which: Int) = if (canMegaEvo != null) canMegaEvo!![which] else false

    fun canDynamax(which: Int) = if (canDynamax != null) canDynamax!![which] else false

    fun isDynamaxed(which: Int): Boolean {
        if (canDynamax(which) || getMoves(which) == null) return false
        return !getMoves(which)!!.all { it.maxMoveId == null }
    }

}
