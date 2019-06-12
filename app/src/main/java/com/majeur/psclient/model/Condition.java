package com.majeur.psclient.model;

import static com.majeur.psclient.model.Colors.statusColor;

public class Condition {

    public int color;
    public String status;
    public final int maxHp;
    public final int hp;
    public float health;

    public Condition(String rawCondition) {
        int separator = rawCondition.indexOf('/');
        if (separator == -1) {
            hp = 0;
            maxHp = 100;
            status = "fnt";
            health = 0f;
            return;
        }
        hp = Integer.parseInt(rawCondition.substring(0, separator));
        int sep = rawCondition.indexOf(' ');
        if (sep == -1) {
            maxHp = Integer.parseInt(rawCondition.substring(separator + 1));
            status = null;
        } else {
            maxHp = Integer.parseInt(rawCondition.substring(separator + 1, sep));
            status = rawCondition.substring(sep + 1);
        }
        health = (float) hp / (float) maxHp;

        if (status == null) return;
        color = Colors.statusColor(status);
    }
}
