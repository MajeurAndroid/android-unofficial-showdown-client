package com.majeur.psclient.model.common

import com.majeur.psclient.model.pokemon.TeamPokemon
import com.majeur.psclient.util.toId
import java.io.Serializable
import java.util.*


class Team private constructor(
        val uniqueId: Int,
       var label: String,
       val pokemons: List<TeamPokemon>,
       var format: String?)
    : Serializable, Comparable<Team> {

    class Group(val format: String) {

        val teams = mutableListOf<Team>()

    }

    constructor(label: String, pokemons: List<TeamPokemon>, format: String?) : this(sUniqueIdInc++, label, pokemons, format) {}

    constructor(source: Team) : this(copiedTeamLabel(source.label), source.pokemons, source.format) {}

    val isEmpty: Boolean
        get() = pokemons.isEmpty()

    override fun compareTo(other: Team) = label.compareTo(other.label, ignoreCase = true)

    fun pack(): String {
        val buf = StringBuilder()
        for (set in pokemons) {
            if (buf.isNotEmpty()) buf.append("]")
            // name
            buf.append(if (set.name != null) set.name else set.species)

            // species
            val blank = set.name == null || set.name?.toId() == set.species.toId()
            buf.append("|").append(if (blank) "" else set.species)

            // item
            buf.append("|").append(set.item?.toId() ?: "")

            // ability
            buf.append("|").append(set.ability?.toId() ?: "")

            // moves
            buf.append("|").append(set.moves.joinToString(","))

            // nature
            buf.append("|").append(set.nature)

            // evs
            buf.append("|")
            if (set.evs.hp != 0) buf.append(set.evs.hp)
            buf.append(",")
            if (set.evs.atk != 0) buf.append(set.evs.atk)
            buf.append(",")
            if (set.evs.def != 0) buf.append(set.evs.def)
            buf.append(",")
            if (set.evs.spa != 0) buf.append(set.evs.spa)
            buf.append(",")
            if (set.evs.spd != 0) buf.append(set.evs.spd)
            buf.append(",")
            if (set.evs.spe != 0) buf.append(set.evs.spe)

            // gender
            if (set.gender != null) {
                buf.append("|").append(set.gender)
            } else {
                buf.append("|")
            }

            // ivs
            buf.append("|")
            if (set.ivs.hp != 31) buf.append(set.ivs.hp)
            buf.append(",")
            if (set.ivs.atk != 31) buf.append(set.ivs.atk)
            buf.append(",")
            if (set.ivs.def != 31) buf.append(set.ivs.def)
            buf.append(",")
            if (set.ivs.spa != 31) buf.append(set.ivs.spa)
            buf.append(",")
            if (set.ivs.spd != 31) buf.append(set.ivs.spd)
            buf.append(",")
            if (set.ivs.spe != 31) buf.append(set.ivs.spe)

            // shiny
            if (set.shiny) {
                buf.append("|S")
            } else {
                buf.append("|")
            }

            // level
            if (set.level != 100) {
                buf.append("|").append(set.level)
            } else {
                buf.append("|")
            }

            // happiness
            if (set.happiness != 255) {
                buf.append("|").append(set.happiness)
            } else {
                buf.append("|")
            }
            if (set.hpType != null) buf.append(",").append(set.hpType)
            if (set.pokeball != null && set.pokeball != "pokeball")
                buf.append(",").append(set.pokeball?.toId() ?: "")
        }
        return buf.toString()
    }

    companion object {

        private var sUniqueIdInc = 1

        private fun copiedTeamLabel(label: String): String {
            return "$label (Copy)"
            //        TODO: Take in account other labels of the same group that could match
//        String newLabel = label;
//        Pattern pattern = Pattern.compile("\\((Copy)(\\d*)\\)$");
//        Matcher matcher = pattern.matcher(newLabel);
//        if (matcher.find()) {
//            String group2 = matcher.group(2);
//            if (TextUtils.isEmpty(group2)) {
//                newLabel = newLabel.substring(0, newLabel.length() - 1) + "2)";
//            } else {
//                int n = Integer.parseInt(group2);
//                newLabel = newLabel.substring(0, newLabel.lastIndexOf(str(n))) + ++n + ")";
//            }
//        } else {
//            newLabel = newLabel + " (Copy)";
//        }
//        return newLabel;
        }

        fun dummyTeam(label: String): Team {
            val pokemons = (1..6).map { TeamPokemon.dummyPokemon() }
            return Team(label, pokemons, null)
        }

        fun unpack(label: String, format: String?, buf: String?): Team? {
            if (buf == null || buf.isBlank()) return Team(label, emptyList(), format)
            if (buf[0] == '[' && buf[buf.length - 1] == ']') {
                // TODO buf = this.packTeam(JSON.parse(buf));
            }
            val team: MutableList<TeamPokemon> = LinkedList()
            var i = 0
            var j = 0

            // limit to 24
            for (count in 0..23) {

                // name
                j = buf.indexOf('|', i)
                if (j < 0) return null
                val name = buf.substring(i, j)
                i = j + 1

                // species
                j = buf.indexOf('|', i)
                if (j < 0) return null
                var species = buf.substring(i, j)
                if (species == "") species = name
                val pokemon = TeamPokemon(species)
                team.add(pokemon)
                i = j + 1

                // item
                j = buf.indexOf('|', i)
                if (j < 0) return null
                pokemon.item = buf.substring(i, j)
                i = j + 1

                // ability
                j = buf.indexOf('|', i)
                if (j < 0) return null
                pokemon.ability = buf.substring(i, j)
                i = j + 1

                // moves
                j = buf.indexOf('|', i)
                if (j < 0) return null
                pokemon.moves = buf.substring(i, j).split(",").take(24)
                i = j + 1

                // nature
                j = buf.indexOf('|', i)
                if (j < 0) return null
                pokemon.nature = buf.substring(i, j)
                i = j + 1

                // evs
                j = buf.indexOf('|', i)
                if (j < 0) return null
                if (j != i) {
                    val evs = buf.substring(i, j).split(",").take(6)
                    pokemon.evs.hp =  evs[0].toIntOrNull() ?: 0
                    pokemon.evs.atk = evs[1].toIntOrNull() ?: 0
                    pokemon.evs.def = evs[2].toIntOrNull() ?: 0
                    pokemon.evs.spa = evs[3].toIntOrNull() ?: 0
                    pokemon.evs.spd = evs[4].toIntOrNull() ?: 0
                    pokemon.evs.spe = evs[5].toIntOrNull() ?: 0
                }
                i = j + 1

                // gender
                j = buf.indexOf('|', i)
                if (j < 0) return null
                if (i != j) pokemon.gender = buf.substring(i, j)
                i = j + 1

                // ivs
                j = buf.indexOf('|', i)
                if (j < 0) return null
                if (j != i) {
                    val ivs = buf.substring(i, j).split(",").take(6)
                    pokemon.ivs.hp =  ivs[0].toIntOrNull() ?: 31
                    pokemon.ivs.atk = ivs[1].toIntOrNull() ?: 31
                    pokemon.ivs.def = ivs[2].toIntOrNull() ?: 31
                    pokemon.ivs.spa = ivs[3].toIntOrNull() ?: 31
                    pokemon.ivs.spd = ivs[4].toIntOrNull() ?: 31
                    pokemon.ivs.spe = ivs[5].toIntOrNull() ?: 31
                }
                i = j + 1

                // shiny
                j = buf.indexOf('|', i)
                if (j < 0) return null
                if (i != j) pokemon.shiny = true
                i = j + 1

                // level
                j = buf.indexOf('|', i)
                if (j < 0) return null
                if (i != j) pokemon.level = buf.substring(i, j).toInt()
                i = j + 1

                // happiness
                j = buf.indexOf(']', i)
                var misc: List<String> = emptyList()
                if (j < 0) {
                    if (i < buf.length) misc = buf.substring(i).split(",").take(3)
                } else {
                    if (i != j) misc = buf.substring(i, j).split(",").take(3)
                }
                if (misc.isNotEmpty()) {
                    pokemon.happiness = misc[0].toIntOrNull() ?: 255
                    // poke.hpType = misc[1];
                    // poke.pokeball = misc[2];TODO
                }
                if (j < 0) break
                i = j + 1
            }
            return Team(label, team, format)
        }
    }

}
