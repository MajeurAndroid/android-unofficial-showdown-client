package com.majeur.psclient.model;

import java.util.List;

public class DexPokemon extends BasePokemon {

    public int num;
    public String firstType;
    public String secondType;
    public Stats baseStats;
    public List<String> abilities;
    public String hiddenAbility;
    public float height;
    public float weight;
    public String color;
    public String gender;
    public String tier;

    public DexPokemon(String rawSpecies, int num, String firstType, String secondType,
                      Stats baseStats, List<String> abilities, String hiddenAbility,
                      float height, float weight, String color, String gender, String tier) {
        super(rawSpecies);
        this.num = num;
        this.firstType = firstType;
        this.secondType = secondType;
        this.baseStats = baseStats;
        this.abilities = abilities;
        this.hiddenAbility = hiddenAbility;
        this.height = height;
        this.weight = weight;
        this.color = color;
        this.gender = gender;
        this.tier = tier;
    }
}