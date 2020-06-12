package com.majeur.psclient.model;

import com.majeur.psclient.R;

public class Type {

    public static final String[] ALL = {
            "Bug",
            "Dark",
            "Dragon",
            "Electric",
            "Fighting",
            "Fire",
            "Flying",
            "Ghost",
            "Grass",
            "Ground",
            "Ice",
            "Poison",
            "Psychic",
            "Rock",
            "Steel",
            "Water",
            "Normal",
            "Fairy"
    };

    public static final String[] HP_TYPES = {
            "Dark",
            "Bug",
            "Dragon",
            "Electric",
            "Fighting",
            "Fire",
            "Flying",
            "Ghost",
            "Grass",
            "Ground",
            "Ice",
            "Poison",
            "Psychic",
            "Rock",
            "Steel",
            "Water",
    };

    public static int getResId(String rawType) {
        if (rawType == null) return 0;
        rawType = rawType.toLowerCase().trim();
        switch (rawType) {
            case "bug": return R.drawable.ic_type_bug;
            case "dark": return R.drawable.ic_type_dark;
            case "dragon": return R.drawable.ic_type_dragon;
            case "electric": return R.drawable.ic_type_electric;
            case "fighting": return R.drawable.ic_type_fighting;
            case "fire": return R.drawable.ic_type_fire;
            case "flying": return R.drawable.ic_type_flying;
            case "ghost": return R.drawable.ic_type_ghost;
            case "grass": return R.drawable.ic_type_grass;
            case "ground": return R.drawable.ic_type_ground;
            case "ice": return R.drawable.ic_type_ice;
            case "poison": return R.drawable.ic_type_poison;
            case "psychic": return R.drawable.ic_type_psychic;
            case "rock": return R.drawable.ic_type_rock;
            case "steel": return R.drawable.ic_type_steel;
            case "water": return R.drawable.ic_type_water;
            case "normal": return R.drawable.ic_type_normal;
            case "fairy": return R.drawable.ic_type_fairy;
            default: return R.drawable.ic_type_unknown;
        }
    }

}
