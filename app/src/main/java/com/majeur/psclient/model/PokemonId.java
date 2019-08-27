package com.majeur.psclient.model;

public class PokemonId {

    public static PokemonId fromRawId(Player player, String rawId) {
        return new PokemonId(player, rawId);
    }

    public static PokemonId fromPosition(Player player, int position) {
        return new PokemonId(player, position);
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

    PokemonId(Player player, int position) {
        this.player = player;
        this.name = null;
        this.position = position;
        foe = player == Player.FOE;
        isInBattle = false;
    }

    @Override
    public String toString() {
        return "PokemonId{" +
                "player=" + player +
                ", position=" + position +
                ", name='" + name + '\'' +
                ", foe=" + foe +
                ", isInBattle=" + isInBattle +
                '}';
    }
}
