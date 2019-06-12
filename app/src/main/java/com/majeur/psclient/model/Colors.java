package com.majeur.psclient.model;

import android.graphics.Color;

public class Colors {

    public static final int BLACK = Color.BLACK;
    public static final int WHITE = Color.WHITE;

    public static final int STAT_BOOST      = Color.BLUE;
    public static final int STAT_UNBOOST    = Color.RED;
    public static final int VOLATILE_STATUS = 0xFF6F35FC;

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

    public static int healthColor(float health) {
        if (health > 0.5f)
            return Color.GREEN;
        else if (health > 0.2f)
            return Color.YELLOW;
        else
            return Color.RED;
    }
}
