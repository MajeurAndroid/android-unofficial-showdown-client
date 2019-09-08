package com.majeur.psclient.model;

import static com.majeur.psclient.util.Utils.boldText;
import static com.majeur.psclient.util.Utils.str;

public class StatModifiers {

    public static final String[] STAT_KEYS = {"atk", "def", "spa", "spd", "spe", "eva"};

    private static final float[] LEVELS = {1f / 4f, 2f / 7f, 1f / 3f, 2f / 5f, 1f / 2f, 2f / 3f, 1f, 3f / 2f, 2f, 5f / 2f, 3f, 7f / 2f, 4f};
    private static final float[] LEVELS_ALT = {3f / 9f, 3f / 8f, 3f / 7f, 3f / 6f, 3f / 5f, 3f / 4f, 3f / 3f, 4f / 3f, 5f / 3f, 6f / 3f, 7f / 3f, 8f / 3f, 9f / 3f};

    int atk = 0;
    int def = 0;
    int spa = 0;
    int spd = 0;
    int spe = 0;
    int eva = 0;


    public int get(String stat) {
        switch (stat) {
            case "atk":
                return atk;
            case "def":
                return def;
            case "spa":
                return spa;
            case "spd":
                return spd;
            case "spe":
                return spe;
            default:
                return eva;
        }
    }

    public void inc(String stat, int val) {
        switch (stat) {
            case "atk":
                atk += val;
                break;
            case "def":
                def += val;
                break;
            case "spa":
                spa += val;
                break;
            case "spd":
                spd += val;
                break;
            case "spe":
                spe += val;
                break;
            default:
                eva += val;
        }
    }

    public void set(String stat, int val) {
        switch (stat) {
            case "atk":
                atk = val;
                break;
            case "def":
                def = val;
                break;
            case "spa":
                spa = val;
                break;
            case "spd":
                spd = val;
                break;
            case "spe":
                spe = val;
                break;
            default:
                eva = val;
        }
    }

    public float modifier(String stat) {
        switch (stat) {
            case "atk":
                return LEVELS[atk + 6];
            case "def":
                return LEVELS[def + 6];
            case "spa":
                return LEVELS[spa + 6];
            case "spd":
                return LEVELS[spd + 6];
            case "spe":
                return LEVELS[spe + 6];
            default:
                return LEVELS_ALT[eva + 6];
        }
    }

    public CharSequence calcReadableStat(String stat, int baseStat) {
        float m = modifier(stat);
        if (m == 1f) return str(baseStat);
        int afterModifier = (int) (baseStat * m);
        return boldText(str(afterModifier));
    }
}
