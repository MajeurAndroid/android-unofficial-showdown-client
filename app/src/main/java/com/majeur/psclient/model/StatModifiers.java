package com.majeur.psclient.model;

import static com.majeur.psclient.util.Utils.boldText;
import static com.majeur.psclient.util.Utils.str;

public class StatModifiers {

    public static final String[] STAT_KEYS = {"atk", "def", "spa", "spd", "spe", "evasion", "accuracy"};

    private static final float[] LEVELS = {1f / 4f, 2f / 7f, 1f / 3f, 2f / 5f, 1f / 2f, 2f / 3f, 1f, 3f / 2f, 2f, 5f / 2f, 3f, 7f / 2f, 4f};
    private static final float[] LEVELS_ALT = {3f / 9f, 3f / 8f, 3f / 7f, 3f / 6f, 3f / 5f, 3f / 4f, 3f / 3f, 4f / 3f, 5f / 3f, 6f / 3f, 7f / 3f, 8f / 3f, 9f / 3f};

    private int atk = 0;
    private int def = 0;
    private int spa = 0;
    private int spd = 0;
    private int spe = 0;
    private int eva = 0;
    private int acc = 0;

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
            case "evasion":
                return eva;
            case "accuracy":
                return acc;
            default:
                return 0;
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
            case "evasion":
                eva += val;
                break;
            case "accuracy":
                acc += val;
                break;
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
            case "evasion":
                eva = val;
                break;
            case "accuracy":
                acc = val;
                break;
        }
    }

    public void set(StatModifiers modifiers) {
        atk = modifiers.atk;
        def = modifiers.def;
        spa = modifiers.spa;
        spd = modifiers.spd;
        spe = modifiers.spe;
        eva = modifiers.eva;
        acc = modifiers.acc;
    }

    public void invert() {
        atk = -atk;
        def = -def;
        spa = -spa;
        spd = -spd;
        spe = -spe;
        eva = -eva;
        acc = -acc;
    }

    public void clear() {
        atk = def = spa = spd = spe = eva = acc = 0;
    }

    public void clearPositive() {
        if (atk > 0) atk = 0;
        if (def > 0) def = 0;
        if (spa > 0) spa = 0;
        if (spd > 0) spd = 0;
        if (spe > 0) spe = 0;
        if (eva > 0) eva = 0;
        if (acc > 0) acc = 0;
    }

    public void clearNegative() {
        if (atk < 0) atk = 0;
        if (def < 0) def = 0;
        if (spa < 0) spa = 0;
        if (spd < 0) spd = 0;
        if (spe < 0) spe = 0;
        if (eva < 0) eva = 0;
        if (acc < 0) acc = 0;
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
            case "evasion":
                return LEVELS_ALT[eva + 6];
            case "accuracy":
                return LEVELS_ALT[acc + 6];
            default:
                return 0;
        }
    }

    public CharSequence calcReadableStat(String stat, int baseStat) {
        float m = modifier(stat);
        if (m == 1f) return str(baseStat);
        int afterModifier = (int) (baseStat * m);
        return boldText(str(afterModifier));
    }
}
