package com.majeur.psclient.model;

import java.util.Arrays;

public class TeamPokemon extends BasePokemon {

    public static TeamPokemon dummyPokemon() {
        return new TeamPokemon("MissingNo");
    }

    public String name;
    public String item;
    public String ability;
    public String[] moves;
    public String nature;
    public Stats evs;
    public String gender;
    public Stats ivs;
    public boolean shiny;
    public int level;
    public int happiness;
    public String hpType;
    public String pokeball;

    public TeamPokemon(String species) {
        super(species);
        ivs = new Stats(31);
        evs = new Stats(0);
        level = 100;
        happiness = 255;
    }

    @Override
    public String toString() {
        return "Poke{" +
                "name='" + name + '\'' +
                ", species='" + species + '\'' +
                ", item='" + item + '\'' +
                ", ability='" + ability + '\'' +
                ", moves=" + Arrays.toString(moves) +
                ", nature='" + nature + '\'' +
                ", evs=" + evs +
                ", gender='" + gender + '\'' +
                ", ivs=" + ivs +
                ", shiny=" + shiny +
                ", level=" + level +
                ", happiness=" + happiness +
                ", hpType='" + hpType + '\'' +
                ", pokeball='" + pokeball + '\'' +
                '}';
    }
}
