package com.majeur.psclientunitest

import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.model.battle.Move
import com.majeur.psclient.model.common.Item
import com.majeur.psclient.model.common.Stats
import com.majeur.psclient.model.pokemon.DexPokemon
import com.majeur.psclient.model.pokemon.TeamPokemon
import com.majeur.psclient.util.smogon.SmogonTeamBuilder
import com.majeur.psclient.util.smogon.SmogonTeamParser
import com.majeur.psclient.util.toId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

fun Stats.joinToString() = array.joinToString()

@RunWith(MockitoJUnitRunner::class)
class SmogonFormatTest {

    private val assetLoader: AssetLoader = Mockito.mock(AssetLoader::class.java)
    val crobat = DexPokemon().apply { // SmogonTeamParser only uses AssetLoader for abilities
        species = "Crobat"
        hiddenAbility = "Infiltrator"
        abilities = listOf("Inner Focus")
    }
    val flyiniumz = Item().apply { name = "Flyinium Z" }
    val bravebird = Move.Details().apply { name = "Brave Bird" }
    val superfang = Move.Details().apply { name = "Super Fang" }
    val taunt = Move.Details().apply { name = "Taunt" }
    val roost = Move.Details().apply { name = "Roost" }

    @Test
    fun `test_Parse a single pokemon`(): Unit = runBlocking(Dispatchers.Unconfined) {
        Mockito.`when`(assetLoader.dexPokemon("crobat")).thenReturn(crobat)

        val text = """
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost
        """.trimIndent()

        val pokemon = SmogonTeamParser.parsePokemon(text, assetLoader)!!
        pokemon.apply {
            assert(name == "The bat") { "Wrong name: $name" }
            assert(species.toId() == "crobat") { "Wrong species: $species" }
            assert(gender.toId() == "f") { "Wrong gender: $gender" }
            assert(item.toId() == "flyiniumz") { "Wrong item: $item" }
            assert(ability == "Infiltrator") { "Wrong ability: $ability" }
            assert(level == 58) { "Wrong level: $level" }
            assert(shiny == true) { "Wrong shinyness: $shiny" }
            assert(evs.atk == 252 && evs.def == 4 && evs.spe == 252) { "Wrong evs: ${evs.joinToString()}" }
            assert(evs.hp == 0 && evs.spa == 0 && evs.spd == 0) { "Wrong default evs" }
            assert(nature.toId() == "jolly") { "Wrong nature: $nature" }
            assert(ivs.spa == 15) { "Wrong ivs: ${ivs.joinToString()}" }
            assert(ivs.hp == 31 && ivs.atk == 31 && ivs.def == 31 && ivs.spd == 31 && ivs.spe == 31) { "Wrong default ivs" }
            assert(moves[0].toId() == "bravebird" && moves[1].toId() == "superfang" && moves[2].toId() == "taunt" &&
                    moves[3].toId() == "roost") { "Wrong moves: ${moves.joinToString()}" }
        }
        Unit
    }

    @Test
    fun `test_Parse multiple pokemons as a single team`(): Unit = runBlocking(Dispatchers.Unconfined) {
        Mockito.`when`(assetLoader.dexPokemon("crobat")).thenReturn(crobat)

        val text = """
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            

        """.trimIndent()

        val teams = SmogonTeamParser.parseTeams(text, assetLoader)
        assert(teams.size == 1) { "More than one team for a single team" }

        val team = teams.first()
        assert(team.pokemons.size == 6) { "Wrong pokemon count: ${team.pokemons.size}" }

        team.pokemons.forEach { pokemon ->
            pokemon.apply {
                assert(name == "The bat") { "Wrong name: $name" }
                assert(species.toId() == "crobat") { "Wrong species: $species" }
                assert(gender.toId() == "f") { "Wrong gender: $gender" }
                assert(item.toId() == "flyiniumz") { "Wrong item: $item" }
                assert(ability == "Infiltrator") { "Wrong ability: $ability" }
                assert(level == 58) { "Wrong level: $level" }
                assert(shiny == true) { "Wrong shinyness: $shiny" }
                assert(evs.atk == 252 && evs.def == 4 && evs.spe == 252) { "Wrong evs: ${evs.joinToString()}" }
                assert(evs.hp == 0 && evs.spa == 0 && evs.spd == 0) { "Wrong default evs" }
                assert(nature.toId() == "jolly") { "Wrong nature: $nature" }
                assert(ivs.spa == 15) { "Wrong ivs: ${ivs.joinToString()}" }
                assert(ivs.hp == 31 && ivs.atk == 31 && ivs.def == 31 && ivs.spd == 31 && ivs.spe == 31) { "Wrong default ivs" }
                assert(moves[0].toId() == "bravebird" && moves[1].toId() == "superfang" && moves[2].toId() == "taunt" &&
                        moves[3].toId() == "roost") { "Wrong moves: ${moves.joinToString()}" }
            }
        }
        Unit
    }

    @Test
    fun `test_Parse multiple teams`(): Unit = runBlocking(Dispatchers.Unconfined) {
        Mockito.`when`(assetLoader.dexPokemon("crobat")).thenReturn(crobat)

        val text = """
            === [gen7uu] test team 1 ===

            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            
            === [gen7doublesuu] Untitled 1 ===
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Level: 58  
            Shiny: Yes  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost  
            

        """.trimIndent()

        val teams = SmogonTeamParser.parseTeams(text, assetLoader)
        assert(teams.size == 2) { "Wrong team count: ${teams.size}" }

        val team1 = teams[0]
        assert(team1.label == "test team 1") { "Wrong label for first team: ${team1.label}" }
        assert(team1.format == "gen7uu") { "Wrong format for first team: ${team1.format}" }
        assert(team1.pokemons.size == 2) { "Wrong pokemon count for first team: ${team1.pokemons.size}" }

        val team2 = teams[1]
        assert(team2.label == "Untitled 1") { "Wrong label for second team: ${team2.label}" }
        assert(team2.format == "gen7doublesuu") { "Wrong format for second team: ${team2.format}" }
        assert(team2.pokemons.size == 4) { "Wrong pokemon count for second team: ${team2.pokemons.size}" }

        (team1.pokemons + team2.pokemons).forEach { pokemon ->
            pokemon.apply {
                assert(name == "The bat") { "Wrong name: $name" }
                assert(species.toId() == "crobat") { "Wrong species: $species" }
                assert(gender.toId() == "f") { "Wrong gender: $gender" }
                assert(item.toId() == "flyiniumz") { "Wrong item: $item" }
                assert(ability == "Infiltrator") { "Wrong ability: $ability" }
                assert(level == 58) { "Wrong level: $level" }
                assert(shiny == true) { "Wrong shinyness: $shiny" }
                assert(evs.atk == 252 && evs.def == 4 && evs.spe == 252) { "Wrong evs: ${evs.joinToString()}" }
                assert(evs.hp == 0 && evs.spa == 0 && evs.spd == 0) { "Wrong default evs" }
                assert(nature.toId() == "jolly") { "Wrong nature: $nature" }
                assert(ivs.spa == 15) { "Wrong ivs: ${ivs.joinToString()}" }
                assert(ivs.hp == 31 && ivs.atk == 31 && ivs.def == 31 && ivs.spd == 31 && ivs.spe == 31) { "Wrong default ivs" }
                assert(moves[0].toId() == "bravebird" && moves[1].toId() == "superfang" && moves[2].toId() == "taunt" &&
                        moves[3].toId() == "roost") { "Wrong moves: ${moves.joinToString()}" }
            }
        }
        Unit
    }

    @Test
    fun `test_Build a single pokemon`(): Unit = runBlocking(Dispatchers.Unconfined) {
        Mockito.`when`(assetLoader.dexPokemon("crobat")).thenReturn(crobat)
        Mockito.`when`(assetLoader.item("flyiniumz")).thenReturn(flyiniumz)
        Mockito.`when`(assetLoader.moveDetails("bravebird")).thenReturn(bravebird)
        Mockito.`when`(assetLoader.moveDetails("superfang")).thenReturn(superfang)
        Mockito.`when`(assetLoader.moveDetails("taunt")).thenReturn(taunt)
        Mockito.`when`(assetLoader.moveDetails("roost")).thenReturn(roost)

        val poke = TeamPokemon().apply {
            species = "crobat"
            name = "The bat"
            gender = "f"
            item = "flyiniumz"
            ability = "infiltrator"
            level = 58
            shiny = true
            evs.apply { atk = 252; def = 4; spe = 252 }
            nature = "jolly"
            ivs.apply { spa = 15 }
            moves = listOf("bravebird", "superfang", "taunt", "roost")
        }

        val result = SmogonTeamBuilder.buildPokemon(assetLoader, poke)

        val text = """
            The bat (Crobat) (F) @ Flyinium Z  
            Ability: Infiltrator  
            Shiny: Yes  
            Level: 58  
            EVs: 252 Atk / 4 Def / 252 Spe  
            Jolly Nature  
            IVs: 15 SpA  
            - Brave Bird  
            - Super Fang  
            - Taunt  
            - Roost
        """.trimIndent()

        val expectedLines = text.split("\n").filter { it.isNotBlank() }
        val resultLines = result.split("\n").filter { it.isNotBlank() }

        assert(expectedLines.size == resultLines.size) {
            "Result string has ${resultLines.size} lines instead of ${expectedLines.size}"
        }

        expectedLines.zip(resultLines).forEachIndexed { i, (expected, result) ->
            assert(expected.trim() == result.trim()) {
                "Line $i do not match with expected result:\n${result.trim()}\n${expected.trim()}-(Expected)"
            }
        }

        Unit
    }
}