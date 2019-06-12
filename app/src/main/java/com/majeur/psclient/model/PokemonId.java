package com.majeur.psclient.model;

public class PokemonId {

    public static PokemonId fromRawId(Player player, String rawId) {
        return new PokemonId(player, rawId);
    }

    public static PokemonId mockNameOnly(String name) {
        return new PokemonId(name);
    }

    public final Player player;
    public final int position;
    public final String name;
    public final boolean foe;
    public final boolean isInBattle;

    PokemonId(Player player, String rawId) {
        this.player = player;
        position = rawId.charAt(2) - 'a';
        name = rawId.substring(rawId.indexOf(":") + 2);
        foe = player == Player.FOE;
        isInBattle = position >= 0;
    }

    PokemonId(String name) {
        this.player = null;
        this.name = name;
        position = -1;
        foe = false;
        isInBattle = false;
    }
}
