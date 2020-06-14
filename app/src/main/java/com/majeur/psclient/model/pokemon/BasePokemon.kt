package com.majeur.psclient.model.pokemon

import com.majeur.psclient.util.toId
import java.io.Serializable


open class BasePokemon : Serializable {

    var species: String = ""
        set(value) {
            computeForme(value)
            field = value
        }

    var baseSpecies: String = ""
    var forme: String? = null
    var spriteId: String = ""

    private fun computeForme(species: String){
        val id = species.toId()
        val excluded = arrayOf("hooh", "hakamoo", "jangmoo", "kommoo", "porygonz")
        if (!excluded.contains(id)) {
            if (id == "kommoototem") {
                baseSpecies = "Kommo-o"
                forme = "Totem"
            } else if (species.contains('-')) {
                baseSpecies = species.substringBefore('-')
                forme = species.substringAfter('-')
            }
        }
        if (id != "yanmega" && id.endsWith("mega")) {
            baseSpecies = id.removeSuffix("mega")
            forme = "mega"
        } else if (id.endsWith("primal")) {
            baseSpecies = id.removeSuffix("primal")
            forme = "primal"
        } else if (id.endsWith("alola")) {
            baseSpecies = id.removeSuffix("alola")
            forme = "alola"
        }

        if (baseSpecies.isEmpty()) baseSpecies = species

        spriteId = baseSpecies.toId() + "-" + (forme ?: "").toId()
        if (spriteId.endsWith("totem")) spriteId = spriteId.removeSuffix("totem")
        if (spriteId.endsWith('-')) spriteId.removeSuffix("-")
    }
}
