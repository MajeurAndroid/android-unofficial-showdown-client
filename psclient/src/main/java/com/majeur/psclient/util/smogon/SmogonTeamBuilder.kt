package com.majeur.psclient.util.smogon

import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.model.common.Nature
import com.majeur.psclient.model.common.Stats
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.model.pokemon.TeamPokemon
import com.majeur.psclient.util.toId
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
        if (dexPokemon.gender == null && (pokemon.gender.equals("m", ignoreCase = true) ||
                pokemon.gender.equals("f", ignoreCase = true))) {
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
        if (pokemon.pokeball.isNotBlank()) {
            builder.append("\n")
            builder.append("Pokeball: ${pokemon.pokeball}")
        }
        if (pokemon.hpType.isNotBlank()) {
            builder.append("\n")
            builder.append("Hidden Power: ${pokemon.hpType}")
        }
        if (pokemon.evs.sum() > 0) {
            builder.append("\n")
            builder.append(
                (0 until 6).filter { i -> pokemon.evs.get(i) > 0 }.joinToString(separator = " / ", prefix = "EVs: ") { i ->
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
                    (0 until 6).filter { i -> pokemon.ivs.get(i) != 31 }.joinToString(separator = " / ", prefix = "IVs: ") { i ->
                        "${pokemon.ivs.get(i)} ${Stats.getName(i)}"
                    }
            )
        }
        pokemon.moves.forEach { moveId ->
            if (moveId.isBlank()) return@forEach
            builder.append("\n")
            val moveName = assetLoader.moveDetails(moveId)?.name ?: moveId
            builder.append("- $moveName")

            // TODO If hidden power type is specified in move name, set ivs accordingly
        }
        builder.append("\n\n")
    }
}