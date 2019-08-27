package com.majeur.psclient.model;

import androidx.annotation.NonNull;

public class Nature {

    public static final Nature Adamant = new Nature("Adamant", "atk", "spa");
    public static final Nature Bashful = new Nature("Bashful", null, null);
    public static final Nature Bold = new Nature("Bold", "def", "atk");
    public static final Nature Brave = new Nature("Brave", "atk", "spe");
    public static final Nature Calm = new Nature("Calm", "spd", "atk");
    public static final Nature Careful = new Nature("Careful", "spd", "spa");
    public static final Nature Docile = new Nature("Docile", null, null);
    public static final Nature Gentle = new Nature("Gentle", "spd", "def");
    public static final Nature Hardy = new Nature("Hardy", null, null);
    public static final Nature Hasty = new Nature("Hasty", "spe", "def");
    public static final Nature Impish = new Nature("Impish", "def", "spa");
    public static final Nature Jolly = new Nature("Jolly", "spe", "spa");
    public static final Nature Lax = new Nature("Lax", "def", "spd");
    public static final Nature Lonely = new Nature("Lonely", "atk", "def");
    public static final Nature Mild = new Nature("Mild", "spa", "def");
    public static final Nature Modest = new Nature("Modest", "spa", "atk");
    public static final Nature Naive = new Nature("Naive", "spe", "spd");
    public static final Nature Naughty = new Nature("Naughty", "atk", "spd");
    public static final Nature Quiet = new Nature("Quiet", "spa", "spe");
    public static final Nature Quirky = new Nature("Quirky", null, null);
    public static final Nature Rash = new Nature("Rash", "spa", "spd");
    public static final Nature Relaxed = new Nature("Relaxed", "def", "spe");
    public static final Nature Sassy = new Nature("Sassy", "spd", "spe");
    public static final Nature Serious = new Nature("Serious", null, null);
    public static final Nature Timid = new Nature("Timid", "spe", "atk");
    public static final Nature[] ALL = new Nature[25];

    static {
        ALL[0] = Adamant;
        ALL[5] = Careful;
        ALL[10] = Impish;
        ALL[15] = Modest;
        ALL[20] = Rash;
        ALL[1] = Bashful;
        ALL[6] = Docile;
        ALL[11] = Jolly;
        ALL[16] = Naive;
        ALL[21] = Relaxed;
        ALL[2] = Bold;
        ALL[7] = Gentle;
        ALL[12] = Lax;
        ALL[17] = Naughty;
        ALL[22] = Sassy;
        ALL[3] = Brave;
        ALL[8] = Hardy;
        ALL[13] = Lonely;
        ALL[18] = Quiet;
        ALL[23] = Serious;
        ALL[4] = Calm;
        ALL[9] = Hasty;
        ALL[14] = Mild;
        ALL[19] = Quirky;
        ALL[24] = Timid;
    }

    public final String name, plus, minus;
    public float atk = 1f;
    public float def = 1f;
    public float spa = 1f;
    public float spd = 1f;
    public float spe = 1f;

    private Nature(String name, String plus, String minus) {
        this.name = name;
        this.plus = plus;
        this.minus = minus;
        if (plus == null || minus == null) return;
        switch (plus) {
            case "atk":
                atk += 0.1f;
                break;
            case "def":
                def += 0.1f;
                break;
            case "spa":
                spa += 0.1f;
                break;
            case "spd":
                spd += 0.1f;
                break;
            case "spe":
                spe += 0.1f;
                break;
        }
        switch (minus) {
            case "atk":
                atk -= 0.1f;
                break;
            case "def":
                def -= 0.1f;
                break;
            case "spa":
                spa -= 0.1f;
                break;
            case "spd":
                spd -= 0.1f;
                break;
            case "spe":
                spe -= 0.1f;
                break;
        }
    }

    public float get(int index) {
        switch (index) {
            case 1:
                return atk;
            case 2:
                return def;
            case 3:
                return spa;
            case 4:
                return spd;
            case 5:
                return spe;
            default:
                return 0;
        }
    }

    @NonNull
    @Override
    public String toString() {
        if (plus != null && minus != null)
            return name + " (+" + plus + "/-" + minus + ")";
        return name;
    }
}
