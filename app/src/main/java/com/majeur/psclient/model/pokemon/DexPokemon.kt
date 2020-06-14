package com.majeur.psclient.model.pokemon

import com.majeur.psclient.model.common.Stats


class DexPokemon : BasePokemon() {

    var num = 0
    lateinit var firstType: String
    var secondType: String? = null
    lateinit var baseStats: Stats
    var abilities = emptyList<String>()
    var hiddenAbility: String? = null
    var height = 0f
    var weight = 0f
    var color: String? = null
    var gender: String? = null
    var tier: String? = null

}