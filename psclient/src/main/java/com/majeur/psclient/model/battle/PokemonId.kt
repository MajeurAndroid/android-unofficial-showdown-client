package com.majeur.psclient.model.battle


class PokemonId {

    val player: Player
    val position: Int
    val name: String

    val foe get() = player == Player.FOE
    val trainer get() = player == Player.TRAINER
    val isInBattle get() = position >= 0


    // |switch|p1a: Quagsire|Quagsire, L83, M|100/100
    constructor(player: Player, rawId: String) {
        this.player = player
        position = rawId[2] - 'a' // char to alphabet index, if pkmn not in battle ':' - 'a' gives negative position
        name = rawId.substringAfter(": ")
    }

    constructor(player: Player, position: Int) {
        this.player = player
        this.position = position
        name = ""
    }

    override fun equals(other: Any?) = other is PokemonId && foe == other.foe && position == other.position
    override fun hashCode() = 31 * position + name.hashCode()
}
