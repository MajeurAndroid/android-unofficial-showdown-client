package com.majeur.psclient.model;

import android.graphics.Color;

public class Colors {

    public static final int BLACK   = Color.BLACK;
    public static final int WHITE   = Color.WHITE;
    public static final int GREEN   = 0xFF12D600;
    public static final int RED     = 0xFFD90000;
    public static final int BLUE    = Color.BLUE;
    public static final int YELLOW  = Color.YELLOW;
    public static final int GRAY    = 0xFF636363;

    public static final int STAT_BOOST          = BLUE;
    public static final int STAT_UNBOOST        = RED;
    public static final int VOLATILE_STATUS     = 0xFF6F35FC;
    public static final int VOLATILE_GOOD       = 0xFF33AA00;
    public static final int VOLATILE_NEUTRAL    = 0xFF555555;
    public static final int VOLATILE_BAD        = 0xFFFF4400;

    public static final int CATEGORY_PHYSICAL   = 0xFFEB5628;
    public static final int CATEGORY_PHY_INNER  = 0xFFFFF064;
    public static final int CATEGORY_SPECIAL    = 0xFF2260C2;
    public static final int CATEGORY_STATUS     = 0xFF9A9997;

    public static final int TYPE_NORMAL     = 0xFFA8A77A;
    public static final int TYPE_FIRE       = 0xFFEE8130;
    public static final int TYPE_WATER      = 0xFF6390F0;
    public static final int TYPE_ELECTRIC   = 0xFFF7D02C;
    public static final int TYPE_GRASS      = 0xFF7AC74C;
    // static final int TYPE_ICE      = 0xFF96D9D6;
    public static final int TYPE_ICE        = 0xFF7ac7c4;
    public static final int TYPE_FIGHTING   = 0xFFC22E28;
    public static final int TYPE_POISON     = 0xFFA33EA1;
    public static final int TYPE_GROUND     = 0xFFE2BF65;
    public static final int TYPE_FLYING     = 0xFFA98FF3;
    public static final int TYPE_PSYCHIC    = 0xFFF95587;
    public static final int TYPE_BUG        = 0xFFA6B91A;
    public static final int TYPE_ROCK       = 0xFFB6A136;
    public static final int TYPE_GHOST      = 0xFF735797;
    public static final int TYPE_DRAGON     = 0xFF6F35FC;
    public static final int TYPE_DARK       = 0xFF705746;
    public static final int TYPE_STEEL      = 0xFFB7B7CE;
    public static final int TYPE_FAIRY      = 0xFFD685AD;
    
    public static int typeColor(String type) {
        switch (type) {
            case "normal":
                return TYPE_NORMAL;
            case "fire":
                return TYPE_FIRE;
            case "water":
                return TYPE_WATER;
            case "electric":
                return TYPE_ELECTRIC;
            case "grass":
                return TYPE_GRASS;
            case "ice":
                return TYPE_ICE;
            case "fighting":
                return TYPE_FIGHTING;
            case "poison":
                return TYPE_POISON;
            case "ground":
                return TYPE_GROUND;
            case "flying":
                return TYPE_FLYING;
            case "psychic":
                return TYPE_PSYCHIC;
            case "bug":
                return TYPE_BUG;
            case "rock":
                return TYPE_ROCK;
            case "ghost":
                return TYPE_GHOST;
            case "dragon":
                return TYPE_DRAGON;
            case "dark":
                return TYPE_DARK;
            case "steel":
                return TYPE_STEEL;
            case "fairy":
                return TYPE_FAIRY;
            default:
                return 0;
        }
    }

    public static int statusColor(String status) {
        switch (status) {
            case "psn":
            case "tox":
                return TYPE_POISON;
            case "brn":
                return TYPE_FIRE;
            case "frz":
                return TYPE_ICE;
            case "par":
                return TYPE_ELECTRIC;
            case "slp":
                return TYPE_NORMAL;
            default:
                return 0;
        }
    }

    public static int sideColor(String side) {
        switch (side) {
            case "AV": // Aurora Veil
                return TYPE_ICE;
            case "LS": // Light Screen
                return TYPE_PSYCHIC;
            case "MI": // Mist
                return TYPE_ICE;
            case "RE": // Reflect
                return TYPE_PSYCHIC;
            case "SG": // Safe Guard
                return TYPE_NORMAL;
            case "SP": // Spikes
                return TYPE_GROUND;
            case "SR": // Stealth Rock
                return TYPE_ROCK;
            case "SW": // Sticky Web
                return TYPE_BUG;
            case "TA": // Tailwhind
                return TYPE_FLYING;
            case "TS": // Toxic Spikes
                return TYPE_POISON;
            default:
                return BLACK;
        }
    }

    public static int healthColor(float health) {
        if (health > 0.5f)
            return GREEN;
        else if (health > 0.2f)
            return YELLOW;
        else
            return RED;
    }

    public static int categoryColor(String cat) {
        switch (cat) {
            case "physical":
                return CATEGORY_PHYSICAL;
            case "special":
                return CATEGORY_SPECIAL;
            case "status":
                return CATEGORY_STATUS;
            default:
                return 0;
        }
    }
}
