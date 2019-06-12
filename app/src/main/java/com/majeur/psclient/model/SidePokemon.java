package com.majeur.psclient.model;

import com.majeur.psclient.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class SidePokemon {

    public int index;
    public String name;
    public String gender = "";
    public String species;
    public boolean shiny;
    public int level = 100;
    public Condition condition;
    public boolean active;
    public Stats stats;
    public List<String> moves;
    public String baseAbility;
    public String item;
    public String pokeBall;
    public String ability;

    private StatModifiers statsModifiers;

    public SidePokemon(int index, JSONObject jsonObject) throws JSONException {
        statsModifiers = new StatModifiers();
        this.index = index;

        String ident = jsonObject.getString("ident");
        name = ident.substring(ident.indexOf(':') + 1);
        condition = new Condition(jsonObject.getString("condition"));
        active = jsonObject.getBoolean("active");
        stats = new Stats(jsonObject.getJSONObject("stats"));
        moves = Utils.getList(jsonObject.getJSONArray("moves"));
        baseAbility = jsonObject.getString("baseAbility");
        item = jsonObject.getString("item");
        pokeBall = jsonObject.getString("pokeball");
        ability = jsonObject.getString("ability");

        String[] detailsArray = jsonObject.getString("details").toLowerCase().split(", ");
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
    }
}