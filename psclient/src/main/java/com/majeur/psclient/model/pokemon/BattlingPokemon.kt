package com.majeur.psclient.model.pokemon

import com.majeur.psclient.model.battle.Condition
import com.majeur.psclient.model.battle.Player
import com.majeur.psclient.model.battle.PokemonId
import com.majeur.psclient.model.battle.StatModifiers

class BattlingPokemon(val player: Player, switchMessage: String) : BasePokemon() {

    val id: PokemonId = PokemonId(player, switchMessage.substringBefore('|'))
    val name = switchMessage.substringAfter(":").substringBefore('|').trim()
    val statModifiers = StatModifiers()
    val volatiles = mutableSetOf<String>()

    var gender: String = ""
    var shiny: Boolean = false
    var level: Int = 100
    var condition: Condition? = null
    var transformSpecies: String? = null

    val position get() = id.position
    val foe get() = id.foe
    val trainer get() = id.trainer

    // name|details[|condition]
    init {
        val details = switchMessage.substringAfter('|').substringBefore('|')
        val detailsArray = details.split(", ")
        species = detailsArray[0]
        detailsArray.drop(1).forEach {
            when (it[0].toLowerCase()) {
                's' -> shiny = true
                'm' -> gender = "♂"
                'f' -> gender = "♀"
                'l' -> level = it.drop(1).toInt()
            }
        }

        val sepCount = switchMessage.count { it == '|' }
        if (sepCount > 1) condition = Condition(switchMessage.substringAfterLast('|'))
    }

    /*
    copyAll = false means Baton Pass,
    copyAll = true means Illusion breaking
    TODO: check use for illusion breaking
     */
    fun copyVolatiles(pokemon: BattlingPokemon?, copyAll: Boolean) {
        if (pokemon == null) return
        statModifiers?.set(pokemon.statModifiers)
        volatiles.addAll(pokemon.volatiles)
        if (!copyAll) {
            volatiles.remove("airballoon")
            volatiles.remove("attract")
            volatiles.remove("autotomize")
            volatiles.remove("disable")
            volatiles.remove("encore")
            volatiles.remove("foresight")
            volatiles.remove("imprison")
            volatiles.remove("laserfocus")
            volatiles.remove("mimic")
            volatiles.remove("miracleeye")
            volatiles.remove("nightmare")
            volatiles.remove("smackdown")
            volatiles.remove("stockpile1")
            volatiles.remove("stockpile2")
            volatiles.remove("stockpile3")
            volatiles.remove("torment")
            volatiles.remove("typeadd")
            volatiles.remove("typechange")
            volatiles.remove("yawn")
        }
        volatiles.remove("transform")
        volatiles.remove("formechange")
    }

}