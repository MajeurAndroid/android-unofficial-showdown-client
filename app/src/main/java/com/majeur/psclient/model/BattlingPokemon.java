package com.majeur.psclient.model;

import androidx.collection.ArraySet;

import java.util.Set;

public class BattlingPokemon extends BasePokemon {

    public static BattlingPokemon fromSwitchMessage(Player player, String message) {
        int separator = message.indexOf('|');
        int nextSeparator = message.indexOf('|', separator + 1);

        PokemonId id = new PokemonId(player, message.substring(0, separator));

        String details = nextSeparator == -1 ? message.substring(separator + 1)
                : message.substring(separator + 1, nextSeparator);
        String[] detailsArray = details.split(", ");
        String species = detailsArray[0];
        boolean shiny = false;
        String gender = "";
        int level = 100;
        for (int i = 1; i < detailsArray.length; i++) {
            char c = detailsArray[i].toLowerCase().charAt(0);
            switch (c) {
                case 's':
                    shiny = true;
                    break;
                case 'm':
                    gender = "♂";
                    break;
                case 'f':
                    gender = "♀";
                    break;
                case 'l':
                    level = Integer.parseInt(detailsArray[i].substring(1));
                    break;
            }
        }

        Condition condition = null;
        if (nextSeparator != -1) {
            separator = nextSeparator;
            nextSeparator = message.indexOf('|', separator + 1);

            condition = new Condition(nextSeparator == -1 ? message.substring(separator + 1)
                    : message.substring(separator + 1, nextSeparator));
        }

        return new BattlingPokemon(species, player, id, level, gender, shiny, condition);
    }

    public Player player;
    public PokemonId id;
    public String name;
    public int position;
    public boolean foe;
    public int level;
    public String gender;
    public boolean shiny;
    public Condition condition;
    public StatModifiers statModifiers;
    public String transformSpecies;
    public final Set<String> volatiles = new ArraySet<>();

    public BattlingPokemon(String rawSpecies, Player player, PokemonId id, int level, String gender, boolean shiny,
                           Condition condition) {
        super(rawSpecies);
        this.player = player;
        this.id = id;
        this.name = id.name;
        this.position = id.position;
        this.foe = id.foe;
        this.level = level;
        this.gender = gender;
        this.shiny = shiny;
        this.condition = condition;
        this.statModifiers = new StatModifiers();
    }


    /*
    copyAll = false means Baton Pass,
    copyAll = true means Illusion breaking
    TODO: check use for illusion breaking
     */
    public void copyVolatiles(BattlingPokemon pokemon, boolean copyAll) {
        statModifiers.set(pokemon.statModifiers);
        volatiles.addAll(pokemon.volatiles);
        if (!copyAll) {
            volatiles.remove("airballoon");
            volatiles.remove("attract");
            volatiles.remove("autotomize");
            volatiles.remove("disable");
            volatiles.remove("encore");
            volatiles.remove("foresight");
            volatiles.remove("imprison");
            volatiles.remove("laserfocus");
            volatiles.remove("mimic");
            volatiles.remove("miracleeye");
            volatiles.remove("nightmare");
            volatiles.remove("smackdown");
            volatiles.remove("stockpile1");
            volatiles.remove("stockpile2");
            volatiles.remove("stockpile3");
            volatiles.remove("torment");
            volatiles.remove("typeadd");
            volatiles.remove("typechange");
            volatiles.remove("yawn");
        }
        volatiles.remove("transform");
        volatiles.remove("formechange");
    }
}
