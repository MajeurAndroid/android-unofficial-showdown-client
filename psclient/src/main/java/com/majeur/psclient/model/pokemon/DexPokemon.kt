package com.majeur.psclient.model.pokemon

import com.majeur.psclient.model.common.Stats
import com.majeur.psclient.util.toId


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
    var requiredItem: String? = null

    fun matchingAbility(abilityId: String, or: String = abilityId): String {
        abilities.forEach { a -> if (a.toId() == abilityId) return a }
        if (hiddenAbility?.toId() == abilityId) return hiddenAbility!!
        return or
    }

}