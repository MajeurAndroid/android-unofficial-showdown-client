package com.majeur.psclient.util

import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.model.common.Nature
import com.majeur.psclient.model.common.Stats
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.model.pokemon.TeamPokemon
import java.util.*

object SmogonTeamBuilder {

    suspend fun buildTeams(assetLoader: AssetLoader, teams: List<Team>): String {
        val builder = StringBuilder()
        buildTeams(builder, assetLoader, teams)
        return builder.toString()
    }

    private suspend fun buildTeams(builder: StringBuilder, assetLoader: AssetLoader, teams: List<Team>) {
        for (team in teams) {
            buildTeam(builder, assetLoader, team, headers = teams.size > 1)
        }
    }

    private suspend fun buildTeam(builder: StringBuilder, assetLoader: AssetLoader, team: Team, headers: Boolean = false) {
        if (team.pokemons.isEmpty()) return
        if (headers && (team.label.isNotBlank() || team.format != null)) {
            builder.append("===[${team.format ?: "other"}] ${team.label}===")
            builder.append("\n\n")
        }
        team.pokemons.forEach { p ->
            buildPokemon(builder, assetLoader, p)
        }
        builder.append("\n")
    }

    suspend fun buildPokemon(assetLoader: AssetLoader, pokemon: TeamPokemon): String {
        val builder = StringBuilder()
        buildPokemon(builder, assetLoader, pokemon)
        return builder.toString()
    }

    private suspend fun buildPokemon(builder: StringBuilder, assetLoader: AssetLoader, pokemon: TeamPokemon) {
        val dexPokemon = assetLoader.dexPokemon(pokemon.species.toId()) ?: return

        if (pokemon.name.isNotBlank()) {
            builder.append("${pokemon.name} (${dexPokemon.species})")
        } else {
            builder.append(dexPokemon.species)
        }
        if (dexPokemon.gender == null && pokemon.gender.isNotBlank()) {
            builder.append(" (${pokemon.gender.toUpperCase(Locale.ROOT)})")
        }
        if (pokemon.item.isNotBlank()) {
            val item = assetLoader.item(pokemon.item.toId())?.name ?: pokemon.item
            builder.append(" @ $item")
        } // TODO support hidden power type
        if (pokemon.ability.isNotBlank()) {
            builder.append("\n")
            val ability = dexPokemon.matchingAbility(pokemon.ability)
            builder.append("Ability: $ability")
        }
        if (pokemon.shiny) {
            builder.append("\n")
            builder.append("Shiny: Yes")
        }
        if (pokemon.level != 100) {
            builder.append("\n")
            builder.append("Level: ${pokemon.level}")
        }
        if (pokemon.happiness != 255) {
            builder.append("\n")
            builder.append("Happiness: ${pokemon.happiness}")
        }
        if (pokemon.evs.sum() > 0) {
            builder.append("\n")
            builder.append(
                (0 until 6).filter { i -> pokemon.evs.get(i) > 0 }.joinToString(separator = " / ", prefix = "Evs: ") { i ->
                    "${pokemon.evs.get(i)} ${Stats.getName(i)}"
                }
            )
        }
        if (pokemon.nature.isNotBlank()) {
            builder.append("\n")
            val nature = Nature.get(pokemon.nature).name
            builder.append("$nature Nature")
        }
        if (pokemon.ivs.sum() != 6*31) {
            builder.append("\n")
            builder.append(
                    (0 until 6).filter { i -> pokemon.ivs.get(i) != 31 }.joinToString(separator = " / ", prefix = "Ivs: ") { i ->
                        "${pokemon.ivs.get(i)} ${Stats.getName(i)}"
                    }
            )
        }
        pokemon.moves.forEach { moveId ->
            if (moveId.isBlank()) return@forEach
            builder.append("\n")
            val moveName = assetLoader.moveDetails(moveId)?.name ?: moveId
            builder.append("- $moveName")
        }
        builder.append("\n\n")
    }

    /*

    fun fromPokemon(curSet: TeamPokemon): String {
        var text = ""
        text += if (curSet.name != null && curSet.name != curSet.species) {
            "" + curSet.name + " (" + curSet.species + ")"
        } else {
            "" + curSet.species
        }
        if ("m".equals(curSet.gender, ignoreCase = true)) text += " (M)"
        if ("f".equals(curSet.gender, ignoreCase = true)) text += " (F)"
        if (curSet.item != null && curSet.item.length > 0) {
            text += " @ " + curSet.item
        }
        text += "  \n"
        if (curSet.ability != null) {
            text += """Ability: ${curSet.ability}
"""
        }
        if (curSet.level != 100) {
            text += """Level: ${curSet.level}
"""
        }
        if (curSet.shiny) {
            text += "Shiny: Yes  \n"
        }
        if (curSet.happiness != 255) {
            text += """Happiness: ${curSet.happiness}
"""
        }
        if (curSet.pokeball != null) {
            text += """Pokeball: ${curSet.pokeball}
"""
        }
        if (curSet.hpType != null) {
            text += """Hidden Power: ${curSet.hpType}
"""
        }
        var first = true
        if (curSet.evs != null) {
            for (i in 0..5) {
                if (curSet.evs.get(i) == 0) continue
                if (first) {
                    text += "EVs: "
                    first = false
                } else {
                    text += " / "
                }
                text += curSet.evs.get(i).toString() + " " + getName(i)
            }
        }
        if (!first) {
            text += "  \n"
        }
        if (curSet.nature != null) {
            text += """${curSet.nature} Nature
"""
        }
        first = true
        if (curSet.ivs != null) {
            var defaultIvs = true
            var hpType: String? = null
            val dummyStats = Stats(0)
            for (j in curSet.moves.indices) {
                val move = curSet.moves[j]
                if (move != null && move.length >= 13 && move.substring(0, 13) == "Hidden Power " && move.substring(0, 14) != "Hidden Power [") {
                    hpType = move.substring(13)
                    if (!checkHpType(hpType)) {
                        continue
                    }
                    for (i in 0..5) {
                        dummyStats.setForHpType(hpType)
                        if (curSet.ivs.get(i) != dummyStats.get(i)) {
                            defaultIvs = false
                            break
                        }
                    }
                }
            }
            if (defaultIvs && hpType == null) {
                for (i in 0..5) {
                    if (curSet.ivs.get(i) != 31) {
                        defaultIvs = false
                        break
                    }
                }
            }
            if (!defaultIvs) {
                for (i in 0..5) {
                    if (curSet.ivs.get(i) == 31) continue
                    if (first) {
                        text += "IVs: "
                        first = false
                    } else {
                        text += " / "
                    }
                    text += curSet.ivs.get(i).toString() + " " + getName(i)
                }
            }
        }
        if (!first) {
            text += "  \n"
        }
        if (curSet.moves != null && curSet.moves.size > 0) for (j in curSet.moves.indices) {
            var move = curSet.moves[j] ?: continue
            if (move.length >= 13 && move.substring(0, 13).equals("Hidden Power ", ignoreCase = true)) {
                move = move.substring(0, 13) + '[' + move.substring(13) + ']'
            }
            text += "- $move  \n"
        }
        return text
    }
     */
}