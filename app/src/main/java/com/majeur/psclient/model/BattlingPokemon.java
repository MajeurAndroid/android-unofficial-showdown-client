package com.majeur.psclient.model;

public class BattlingPokemon {

    public static BattlingPokemon fromSwitchMessage(Player player, String message) {
        return new BattlingPokemon(player, message);
    }

    public final String name;
    public final String species;
    public int level = 100;
    public String gender;
    public boolean shiny;

    public Condition condition;
    public StatModifiers statModifiers;

    public final Player player;
    public final int position;
    public final boolean foe;

    public final PokemonId id;

    BattlingPokemon(Player player, String message) {
        int separator = message.indexOf('|');
        int nextSeparator = message.indexOf('|', separator+1);

        id = new PokemonId(player, message.substring(0, separator));
        name = id.name;
        this.player = player;
        position = id.position;
        foe = id.foe;

        String details = message.substring(separator+1, nextSeparator);
        String[] detailsArray = details.toLowerCase().split(", ");
        species = detailsArray[0];
        for (int i = 1; i < detailsArray.length; i++) {
            switch (detailsArray[i].charAt(0)){
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
        separator = nextSeparator;
        nextSeparator = message.indexOf('|', separator+1);

        condition = new Condition(nextSeparator == -1 ? message.substring(separator + 1)
                : message.substring(separator+1, nextSeparator));

        statModifiers = new StatModifiers();
    }
}
