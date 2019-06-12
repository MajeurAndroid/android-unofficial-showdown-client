package com.majeur.psclient.model;

import java.util.Arrays;

public class Poke {

    public static Poke dummyPokemon() {
        Poke poke = new Poke();
        poke.species = "missingno";
        return poke;
    }

    public String name;
    public String species;
    public String item;
    public String ability;
    public String abilitySlot; // TODO
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

    public Poke() {
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

    public static class Stats {
        public int hp;
        public int atk;
        public int def;
        public int spa;
        public int spd;
        public int spe;

        public Stats(int defaultValue) {
            this.hp = defaultValue;
            this.atk = defaultValue;
            this.def = defaultValue;
            this.spa = defaultValue;
            this.spd = defaultValue;
            this.spe = defaultValue;
        }

        @Override
        public String toString() {
            return "Stats{" +
                    "hp=" + hp +
                    ", atk=" + atk +
                    ", def=" + def +
                    ", spa=" + spa +
                    ", spd=" + spd +
                    ", spe=" + spe +
                    '}';
        }
    }
}
