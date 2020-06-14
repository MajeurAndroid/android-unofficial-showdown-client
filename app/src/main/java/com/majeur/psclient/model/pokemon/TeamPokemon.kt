package com.majeur.psclient.model.pokemon

import com.majeur.psclient.model.common.Stats

class TeamPokemon(species: String) : BasePokemon() {

    var name: String? = null
    var item: String? = null
    var ability: String? = null
    var moves = mutableListOf<String>()
    var nature: String? = null
    var evs: Stats
    var gender: String? = null
    var ivs: Stats
    var shiny = false
    var level: Int
    var happiness: Int
    var hpType: String? = null
    var pokeball: String? = null

    init {
        this.species = species
        ivs = Stats(31)
        evs = Stats(0)
        level = 100
        happiness = 255
    }

    companion object {

        @JvmStatic
        fun dummyPokemon(): TeamPokemon {
            return TeamPokemon("MissingNo")
        }

    }
}
