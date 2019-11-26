package com.majeur.psclient.model;

import java.util.LinkedList;
import java.util.List;

public class BattleDecision {

    private List<Choice> mChoices = new LinkedList<>();

    private static class Choice {
        String action;
        int index;
        String extra;
        int target;
    }

    public void addSwitchChoice(int who) {
        Choice c = new Choice();
        c.action = "switch";
        c.index = who;
        mChoices.add(c);
    }

    public void addMoveChoice(int which, boolean mega, boolean zmove, boolean dynamax) {
        Choice c = new Choice();
        c.action = "move";
        c.index = which;
        if (mega) c.extra = "mega";
        else if (zmove) c.extra = "zmove";
        else if (dynamax) c.extra = "dynamax";
        mChoices.add(c);
    }

    public void setLastMoveTarget(int target) {
        mChoices.get(mChoices.size() - 1).target = target;
    }

    public void addPassChoice() {
        Choice c = new Choice();
        c.action = "pass";
        mChoices.add(c);
    }

    public String build() {
        StringBuilder builder = new StringBuilder();
        int N = mChoices.size();
        for (int i = 0; i < N; i++) {
            Choice c = mChoices.get(i);
            builder.append(c.action);
            if (c.index != 0) builder.append(" ").append(c.index);
            if (c.extra != null) builder.append(" ").append(c.extra);
            if (c.target != 0) builder.append(" ").append(c.target);
            if (i < N -1) builder.append(",");
        }
        return builder.toString();
    }
}