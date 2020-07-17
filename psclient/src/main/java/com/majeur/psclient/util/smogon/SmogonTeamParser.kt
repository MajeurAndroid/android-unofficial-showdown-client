package com.majeur.psclient.util.smogon

import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.model.common.toId
import com.majeur.psclient.model.pokemon.TeamPokemon
import com.majeur.psclient.util.or
import com.majeur.psclient.util.toId

object SmogonTeamParser {

    private val TEAM_HEADER_CONTENT_PATTERN = "(?<=={3}).*(?=={3})".toRegex().toPattern()
    private val TEAM_HEADER_SPLIT_REGEX = "={3}.*={3}".toRegex()

    suspend fun parseTeams(importString: String, assetLoader: AssetLoader): List<Team> {
        val teams = mutableListOf<Team>()

        val matcher = TEAM_HEADER_CONTENT_PATTERN.matcher(importString)
        val headers = mutableListOf<String>()
        while (matcher.find()) headers.add(matcher.group().trim())

        if (headers.isEmpty()) { // Single team import
            parseTeam(importString, BattleFormat.FORMAT_OTHER.toId(), "Unnamed team", assetLoader)?.also {
                teams.add(it)
            }
        } else { // Multiple teams import
            val rawTeams = importString.split(TEAM_HEADER_SPLIT_REGEX).drop(1)
            rawTeams.zip(headers).forEach { (rawTeam, header) ->
                val format = getFormatFromHeader(header) ?: BattleFormat.FORMAT_OTHER.toId()
                val teamLabel = getLabelFromHeader(header).or("Unnamed team")
                val team = parseTeam(rawTeam, format, teamLabel, assetLoader)
                if (team != null) teams.add(team)
            }
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
        if (lines.isEmpty()) return null

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
                        "HP" -> p.evs.hp =   pair.second ?: 0
                        "Atk" -> p.evs.atk = pair.second ?: 0
                        "Def" -> p.evs.def = pair.second ?: 0
                        "SpA" -> p.evs.spa = pair.second ?: 0
                        "SpD" -> p.evs.spd = pair.second ?: 0
                        "Spe" -> p.evs.spe = pair.second ?: 0
                    }
                }
            } else if (line.trim().endsWith("Nature")) {
                p.nature = line.trim().removeSuffix("Nature").trim()
            } else if (line.startsWith("Ability:")) {
                val ability = line.removePrefix("Ability:").trim()
                val dexPokemon = assetLoader.dexPokemon(p.species.toId())
                val matchingAbility = dexPokemon?.matchingAbility(ability.toId(), "") ?: ""
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