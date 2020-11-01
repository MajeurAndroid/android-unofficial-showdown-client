package com.majeur.psclient.model.battle

import com.majeur.psclient.model.pokemon.SidePokemon
import com.majeur.psclient.util.forEachIndexed
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
    val maxTeamSize = json.optInt("maxTeamSize", Int.MAX_VALUE)
    val shouldWait = json.optBoolean("wait", false)
    val side = json.getJSONObject("side").getJSONArray("pokemon").run {
        (0 until length()).map { i -> SidePokemon(i, getJSONObject(i)) }
    }

    private var forceSwitch = json.optJSONArray("forceSwitch")?.run {
        (0 until length()).map { i -> getBoolean(i) }.toBooleanArray()
    }
    private val trapped: BooleanArray?
    private val canMegaEvo: BooleanArray?
    private val canDynamax: BooleanArray?
    private val moves: Array<Array<Move>>?

    init {
        val actives = json.optJSONArray("active")
        trapped = if (actives != null) BooleanArray(actives.length()) else null
        canMegaEvo = if (actives != null) BooleanArray(actives.length()) else null
        canDynamax = if (actives != null) BooleanArray(actives.length()) else null
        moves = if (actives != null) Array(actives.length()) { emptyArray<Move>() } else null
        actives?.forEachIndexed { i, active ->
            trapped!![i] = active.optBoolean("trapped", false)
            canMegaEvo!![i] = active.optBoolean("canMegaEvo", false)
            canDynamax!![i] = active.optBoolean("canDynamax", false)
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

    fun getMoves(which: Int) = moves?.getOrNull(which)

    fun shouldPass(which: Int) = !forceSwitch(which) && getMoves(which) == null

    fun forceSwitch(which: Int) = forceSwitch?.getOrNull(which) ?: false

    fun trapped(which: Int) = trapped?.getOrNull(which) ?: false

    fun canMegaEvo(which: Int) = canMegaEvo?.getOrNull(which) ?: false

    fun canDynamax(which: Int) = canDynamax?.getOrNull(which) ?: false

    fun isDynamaxed(which: Int): Boolean {
        if (canDynamax(which) || getMoves(which) == null) return false
        return !getMoves(which)!!.all { it.maxMoveId == null }
    }

}
