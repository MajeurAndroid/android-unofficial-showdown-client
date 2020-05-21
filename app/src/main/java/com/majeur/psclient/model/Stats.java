package com.majeur.psclient.model;

import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Stats implements Serializable {

    public int hp;
    public int atk;
    public int def;
    public int spa;
    public int spd;
    public int spe;

    public Stats(JSONObject jsonObject) throws JSONException {
        hp = jsonObject.optInt("hp", 0);
        atk = jsonObject.getInt("atk");
        def = jsonObject.getInt("def");
        spa = jsonObject.getInt("spa");
        spd = jsonObject.getInt("spd");
        spe = jsonObject.getInt("spe");
    }

    public Stats(int hp, int atk, int def, int spa, int spd, int spe) {
        this.hp = hp;
        this.atk = atk;
        this.def = def;
        this.spa = spa;
        this.spd = spd;
        this.spe = spe;
    }

    public Stats(int defaultValue) {
        this.hp = defaultValue;
        this.atk = defaultValue;
        this.def = defaultValue;
        this.spa = defaultValue;
        this.spd = defaultValue;
        this.spe = defaultValue;
    }

    public void set(int index, int value) {
        switch (index) {
            case 0:
                hp = value;
                break;
            case 1:
                atk = value;
                break;
            case 2:
                def = value;
                break;
            case 3:
                spa = value;
                break;
            case 4:
                spd = value;
                break;
            case 5:
                spe = value;
                break;
        }
    }

    public int get(int index) {
        return toArray()[index];
    }

    public int[] toArray() {
        return new int[]{hp, atk, def, spa, spd, spe};
    }

    public static String getName(int index) {
        switch (index) {
            case 0: return "HP";
            case 1: return "Atk";
            case 2: return "Def";
            case 3: return "SpA";
            case 4: return "Spd";
            case 5: return "Spe";
            default: return null;
        }
    }

    @Override
    public String toString() {
        return "Stats{" +
                "hp=" + hp +
                ", atk=" + atk +
                ", def=" + def +
                ", spa=" + spa +
                ", spd=" + spd +
                ", spe=" + spe +
                '}';
    }

    public int sum() {
        return hp + atk + def + spa + spd + spe;
    }

    public String hpType() {
        int a = hp % 2 == 0 ? 0 : 1;
        int b = atk % 2 == 0 ? 0 : 2;
        int c = def % 2 == 0 ? 0 : 4;
        int d = spe % 2 == 0 ? 0 : 8;
        int e = spa % 2 == 0 ? 0 : 16;
        int f = spd % 2 == 0 ? 0 : 32;
        int t = (a + b + c + d + e + f) * 15 / 63;
        switch (t) {
            case 0: return "Fighting";
            case 1: return "Flying";
            case 2: return "Poison";
            case 3: return "Ground";
            case 4: return "Rock";
            case 5: return "Bug";
            case 6: return "Ghost";
            case 7: return "Steel";
            case 8: return "Fire";
            case 9: return "Water";
            case 10: return "Grass";
            case 11: return "Electric";
            case 12: return "Psychic";
            case 13: return "Ice";
            case 14: return "Dragon";
            case 15: return "Dark";
            default: return null;
        }
    }

    public void setForHpType(@Nullable String type) {
        if (type == null) type = "Dark"; // Default all 31 iv spread
        switch (type) {
            case "Bug":			hp = 31; atk = 31; def = 31; spa = 31; spd = 30; spe = 30; break;
            case "Dark":		hp = 31; atk = 31; def = 31; spa = 31; spd = 31; spe = 31; break;
            case "Dragon":		hp = 30; atk = 31; def = 31; spa = 31; spd = 31; spe = 31; break;
            case "Electric":	hp = 31; atk = 31; def = 31; spa = 30; spd = 31; spe = 31; break;
            case "Fighting":	hp = 31; atk = 31; def = 30; spa = 30; spd = 30; spe = 30; break;
            case "Fire":		hp = 31; atk = 30; def = 31; spa = 30; spd = 31; spe = 30; break;
            case "Flying":		hp = 31; atk = 31; def = 31; spa = 30; spd = 30; spe = 30; break;
            case "Ghost":		hp = 31; atk = 30; def = 31; spa = 31; spd = 30; spe = 31; break;
            case "Grass":		hp = 30; atk = 31; def = 31; spa = 30; spd = 31; spe = 31; break;
            case "Ground":		hp = 31; atk = 31; def = 31; spa = 30; spd = 30; spe = 31; break;
            case "Ice":			hp = 31; atk = 31; def = 31; spa = 31; spd = 31; spe = 30; break;
            case "Poison":		hp = 31; atk = 31; def = 30; spa = 30; spd = 30; spe = 31; break;
            case "Psychic":		hp = 30; atk = 31; def = 31; spa = 31; spd = 31; spe = 30; break;
            case "Rock":		hp = 31; atk = 31; def = 30; spa = 31; spd = 30; spe = 30; break;
            case "Steel":		hp = 31; atk = 31; def = 31; spa = 31; spd = 30; spe = 31; break;
            case "Water":		hp = 31; atk = 31; def = 31; spa = 30; spd = 31; spe = 30; break;
        }
    }

    public static boolean checkHpType(String type) {
        if (type == null) return false;
        type = type.toLowerCase().trim();
        switch (type) {
            case "bug":			return true;
            case "dark":		return true;
            case "dragon":		return true;
            case "electric":	return true;
            case "fighting":	return true;
            case "fire":		return true;
            case "flying":		return true;
            case "ghost":		return true;
            case "grass":		return true;
            case "ground":		return true;
            case "ice":			return true;
            case "poison":		return true;
            case "psychic":		return true;
            case "rock":		return true;
            case "steel":		return true;
            case "water":		return true;
            default:            return false;
        }
    }

    public static int[] calculateSpeedRange(int level, int baseSpe, String tier, int gen) {
        boolean isRandomBattle = tier.contains("Random Battle") || (tier.contains("Random") && tier.contains("Battle") && gen >= 6);

        float minNature = (isRandomBattle || gen < 3) ? 1f : 0.9f;
        float maxNature = (isRandomBattle || gen < 3) ? 1f : 1.1f;
        int maxIv = (gen < 3) ? 30 : 31;

        int min;
        int max;
        if (tier.contains("Let's Go")) {
            min = tr(tr(tr(2 * baseSpe * level / 100f + 5) * minNature) * tr((70f / 255f / 10f + 1) * 100) / 100f);
            max = tr(tr(tr((2 * baseSpe + maxIv) * level / 100f + 5) * maxNature) * tr((70f / 255f / 10f + 1) * 100) / 100f);
            if (tier.contains("No Restrictions")) max += 200;
            else if (tier.contains("Random")) max += 20;
        } else {
            float maxIvEvOffset = maxIv + ((isRandomBattle && gen >= 3) ? 21 : 63);
            min = tr(tr(2 * baseSpe * level / 100f + 5) * minNature);
            max = tr(tr((2 * baseSpe + maxIvEvOffset) * level / 100f + 5) * maxNature);
        }
        return new int[]{min, max};
    }

    private static int tr(float value) {
        return (int) value;
    }


    public static int calculateStat(int base, int iv, int ev, int niv, float nat) {
        return (int) ((((2 * base + iv + ev / 4) * niv) / 100 + 5) * nat);
    }

    public static int calculateHp(int base, int iv, int ev, int niv) {
        return ((2 * base + iv + ev / 4) * niv) / 100 + niv + 10;
    }

    public static int toIndex(String name) {
        name = name.trim().toLowerCase();
        if (name.contains("atk") || name.contains("attack")) {
            if (name.contains("sp")) return 3;
            else return 1;
        } else if (name.contains("def")) {
            if (name.contains("sp")) return 4;
            else return 2;
        } else if (name.contains("sp")) {
            return 5;
        } else if (name.contains("h")) {
            return 0;
        }
        return -1;
    }
}
