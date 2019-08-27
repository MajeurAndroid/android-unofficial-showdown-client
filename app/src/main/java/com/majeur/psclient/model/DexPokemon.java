package com.majeur.psclient.model;

import java.util.List;

public class DexPokemon {

    public int num;
    public String species;
    public List<String> types = null;
    public float[] genderRatio;
    public Stats baseStats;
    public List<String> abilities;
    public String hiddenAbility;
    public float heightm;
    public float weightkg;
    public String color;
    public List<String> evos = null;
    public List<String> eggGroups = null;
    public String tier;
    public String firstType;
    public String secondType;
    public String gender = "MF";

    public DexPokemon(){

    }
}