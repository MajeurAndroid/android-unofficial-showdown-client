package com.majeur.psclient.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Stats {

    public final int hp;
    public final int atk;
    public final int def;
    public final int spa;
    public final int spd;
    public final int spe;

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

    public static final int[] calculateSpeedRange(int level, int baseSpe, String tier, int gen) {
        boolean isRandomBattle = tier.contains("Random Battle") || (tier.contains("Random") && tier.contains("Battle") && gen >= 6);

        float minNature = (isRandomBattle || gen < 3) ? 1f : 0.9f;
        float maxNature = (isRandomBattle || gen < 3) ? 1f : 1.1f;
        int maxIv = (gen < 3) ? 30 : 31;

        int min;
        int max;
        if (tier.contains("Let's Go")) {
            min = tr(tr(tr(2 * baseSpe * level / 100 + 5) * minNature) * tr((70 / 255 / 10 + 1) * 100) / 100);
            max = tr(tr(tr((2 * baseSpe + maxIv) * level / 100 + 5) * maxNature) * tr((70 / 255 / 10 + 1) * 100) / 100);
            if (tier.contains("No Restrictions")) max += 200;
            else if (tier.contains("Random")) max += 20;
        } else {
            float maxIvEvOffset = maxIv + ((isRandomBattle && gen >= 3) ? 21 : 63);
            min = tr(tr(2 * baseSpe * level / 100 + 5) * minNature);
            max = tr(tr((2 * baseSpe + maxIvEvOffset) * level / 100 + 5) * maxNature);
        }
        return new int[]{(int) min, (int) max};
    }

    private static int tr(float value) {
        return (int) value;
    }
}
