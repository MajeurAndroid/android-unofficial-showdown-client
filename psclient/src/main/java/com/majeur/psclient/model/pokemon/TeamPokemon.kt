package com.majeur.psclient.model.pokemon

import com.majeur.psclient.model.common.Stats

class TeamPokemon() : BasePokemon() {

    var name: String = ""
    var item: String = ""
    var ability: String = ""
    var moves: List<String> = emptyList()
    var nature: String = ""
    var evs = Stats(0)
    var gender: String = ""
    var ivs = Stats(31)
    var shiny = false
    var level = 100
    var happiness = 255
    var hpType: String = ""
    var pokeball: String = ""

    constructor(species: String) : this() {
        this.species = species
    }

    companion object {

        @JvmStatic
        fun dummyPokemon(): TeamPokemon {
            return TeamPokemon("MissingNo")
        }

    }
}
