package com.majeur.psclient.model.pokemon

import android.graphics.Bitmap
import com.majeur.psclient.model.battle.Condition
import com.majeur.psclient.model.battle.StatModifiers
import com.majeur.psclient.model.common.Stats
import org.json.JSONObject


class SidePokemon(val index: Int, json: JSONObject) : BasePokemon() {

    val name = json.getString("ident").substringAfter(":")
    val condition = Condition(json.getString("condition"))
    val active = json.getBoolean("active")
    val stats = Stats(json.getJSONObject("stats"))
    val moves = json.getJSONArray("moves").run { (0 until length()).map { getString(it) } }
    val baseAbility = json.getString("baseAbility")
    val item: String = json.getString("item")
    val pokeBall = json.getString("pokeball")
    val ability: String = json.optString("ability").run { if (isBlank()) json.optString("baseAbility") else this } // TODO baseability is when ability is a replacement, investigate this

    var gender: String = ""
    var shiny: Boolean = false
    var level: Int = 100

    var icon: Bitmap? = null
    private val statsModifiers = StatModifiers()

    init {
        val detailsArray = json.getString("details").split(", ")
        species = detailsArray[0]
        detailsArray.drop(1).forEach {
            when (it[0].toLowerCase()) {
                's' -> shiny = true
                'm' -> gender = "♂"
                'f' -> gender = "♀"
                'l' -> level = it.drop(1).toInt()
            }
        }
    }
}