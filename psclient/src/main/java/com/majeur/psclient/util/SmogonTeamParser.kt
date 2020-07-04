package com.majeur.psclient.util

import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.model.pokemon.TeamPokemon
import java.util.*
import java.util.regex.Pattern

object SmogonTeamParser {

    suspend fun parseTeams(importString: String, assetLoader: AssetLoader): List<Team> {
        val teams = mutableListOf<Team>()

        val pattern = Pattern.compile("(?<====).*?(?====)")
        val matcher = pattern.matcher(importString)
        val teamHeaders: Queue<String> = LinkedList()
        while (matcher.find()) teamHeaders.add(matcher.group().trim())
        val rawTeams = importString.split("\\===.*?\\===").toTypedArray()
        if (rawTeams.isEmpty()) return teams

        rawTeams.filter { it.isNotBlank() }.forEach { rawTeam ->
            val teamHeader = teamHeaders.poll()
            val team = if (teamHeader != null) {
                val format = getFormatFromHeader(teamHeader) ?: "Other"
                val teamLabel = getLabelFromHeader(teamHeader).or("Unnamed team")
                parseTeam(rawTeam, format, teamLabel, assetLoader)
            } else {
                parseTeam(rawTeam, "[Other]".toId(), "Unnamed team", assetLoader)
            }
            if (team != null) teams.add(team)
        }
        return teams
    }

    private fun getFormatFromHeader(teamHeader: String): String? {
        val startIndex = teamHeader.indexOf('[')
        val endIndex = teamHeader.indexOf(']')
        return if (startIndex < 0 || endIndex < 0) null else teamHeader.substring(startIndex + 1, endIndex)
    }

    private fun getLabelFromHeader(teamHeader: String): String {
        val startIndex = teamHeader.indexOf(']')
        return if (startIndex < 0) teamHeader.trim() else teamHeader.substring(startIndex + 1).trim()
    }

    private suspend fun parseTeam(rawString: String, format: String, label: String, assetLoader: AssetLoader): Team? {
        // Make sure there is no CR char when we split with LF
        val rawPokemons = rawString.replace("\r", "").split("\n\n").toTypedArray()
        if (rawPokemons.isEmpty()) return null

        val pokemons = rawPokemons.mapNotNull { parsePokemon(it.trim(), assetLoader) }
        return Team(label, pokemons, format)
    }

    suspend fun parsePokemon(rawPokemon: String, assetLoader: AssetLoader): TeamPokemon? {
        val lines = rawPokemon.trim().split("\n").toTypedArray()
        if (lines.isEmpty())  return null

        var firstLine = lines[0] // split 0 is Name @ Item or Name or nickname (Name) or  nickname (Name) @ Item
        val species: String
        var nickname = ""
        var item = ""
        var gender = ""
        if (firstLine.contains('@')) {
            item = firstLine.substringAfter('@').trim()
            firstLine = firstLine.substringBefore('@').trim()
        }
        if (firstLine.contains("(") && firstLine.contains(")")) {
            val countOpen = firstLine.count { it == '(' }
            val countClosed = firstLine.count { it == ')' }
            if (countOpen == 1 && countClosed == 1) {
                // either name or gender
                val genderOrName = firstLine.substringAfter('(').substringBefore(')')
                if (genderOrName == "M" || genderOrName == "F" || genderOrName == "N") {
                    gender = genderOrName
                    species = firstLine.substringBefore("($gender)")
                } else {
                    species = genderOrName
                    nickname = firstLine.substringBefore("($species)")
                }
            } else {
                // both name + gender
                val genderOrName = firstLine.substringAfterLast('(').substringBefore(')')
                if (genderOrName == "M" || genderOrName == "F" || genderOrName == "N") {
                    gender = genderOrName
                    firstLine = firstLine.substringBeforeLast('(')
                    species = firstLine.substringAfter('(').substringBefore(')')
                    nickname = firstLine.substringBefore('(')
                } else {
                    // is nickname with ()()() and (name)
                    species = genderOrName
                    nickname = firstLine.substringBeforeLast('(')
                }
            }
        } else {
            species = firstLine
        }
        val p = TeamPokemon(species)
        if (nickname.isNotBlank()) {
            p.name = nickname.trim()
        }
        if (item.isNotBlank()) {
            p.item = item.toId()
        }
        if (gender.isNotBlank()) {
            if (gender == "M" || gender == "F" || gender == "N") {
                p.gender = gender
            }
        }
        val moves = mutableListOf<String>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.startsWith("-")) {
                // its a move!
                // same as items, it's a real name , we need an id.
                moves.add(line.removePrefix("-").toId())
            } else if (line.startsWith("IVs:")) {
                val ivPairs = line.removePrefix("IVs:").split("/").map { it.trim() }
                        .map { it.substringAfter(' ') to it.substringBefore(' ').toIntOrNull() }
                for (pair in ivPairs) {
                    when (pair.first) {
                        "HP" -> p.ivs.hp =   pair.second ?: 31
                        "Atk" -> p.ivs.atk = pair.second ?: 31
                        "Def" -> p.ivs.def = pair.second ?: 31
                        "SpA" -> p.ivs.spa = pair.second ?: 31
                        "SpD" -> p.ivs.spd = pair.second ?: 31
                        "Spe" -> p.ivs.spe = pair.second ?: 31
                    }
                }
            } else if (line.startsWith("EVs:")) {
                val evPairs = line.removePrefix("EVs:").split("/").map { it.trim() }
                        .map { it.substringAfter(' ') to it.substringBefore(' ').toIntOrNull() }
                for (pair in evPairs) {
                    when (pair.first) {
                        "HP" -> p.ivs.hp =   pair.second ?: 0
                        "Atk" -> p.ivs.atk = pair.second ?: 0
                        "Def" -> p.ivs.def = pair.second ?: 0
                        "SpA" -> p.ivs.spa = pair.second ?: 0
                        "SpD" -> p.ivs.spd = pair.second ?: 0
                        "Spe" -> p.ivs.spe = pair.second ?: 0
                    }
                }
            } else if (line.trim().endsWith("Nature")) {
                p.nature = line.removeSuffix("Nature").trim()
            } else if (line.startsWith("Ability:")) {
                val ability = line.removePrefix("Ability:").trim()
                val dexPokemon = assetLoader.dexPokemon(p.species.toId())
                val matchingAbility = dexPokemon?.matchingAbility(ability, "") ?: ""
                if (matchingAbility.isNotBlank())
                    p.ability = matchingAbility
            } else if (line.startsWith("Level:")) {
                val level = line.removePrefix("Level:").trim()
                p.level = level.toIntOrNull() ?: 100
            } else if (line.startsWith("Shiny")) {
                p.shiny = true
            }
        }
        p.moves = moves
        return if (p.species.isNotBlank()) p else null
    }
}