package com.majeur.psclient.model;

import java.util.LinkedList;
import java.util.List;

public class BattleDecision {

    private static final String CMD_CHOOSE = "choose";
    private static final String CMD_TEAM = "team";
    private static final String ACTION_MOVE = "move";
    private static final String ACTION_SWITCH = "switch";
    private static final String ACTION_PASS = "pass";
    private static final String EXTRA_MEGA = "mega";
    private static final String EXTRA_ZMOVE = "zmove";
    private static final String EXTRA_DYNAMAX = "dynamax";

    private String mCommand;
    private List<Choice> mChoices = new LinkedList<>();

    private static class Choice {
        String action;
        int index;
        String extra;
        int target;
    }

    public void addSwitchChoice(int who) {
        mCommand = CMD_CHOOSE;
        Choice c = new Choice();
        c.action = ACTION_SWITCH;
        c.index = who;
        mChoices.add(c);
    }

    public void addMoveChoice(int which, boolean mega, boolean zmove, boolean dynamax) {
        mCommand = CMD_CHOOSE;
        Choice c = new Choice();
        c.action = ACTION_MOVE;
        c.index = which;
        if (mega) c.extra = EXTRA_MEGA;
        else if (zmove) c.extra = EXTRA_ZMOVE;
        else if (dynamax) c.extra = EXTRA_DYNAMAX;
        mChoices.add(c);
    }

    public void setLastMoveTarget(int target) {
        mChoices.get(mChoices.size() - 1).target = target;
    }

    public void addPassChoice() {
        mCommand = CMD_CHOOSE;
        Choice c = new Choice();
        c.action = ACTION_PASS;
        mChoices.add(c);
    }

    public void addTeamChoice(int first, int teamSize) {
        mCommand = CMD_TEAM;
        String teamOrder = "";
        for (int i = 1; i <= teamSize; i++) teamOrder += i;
        teamOrder = teamOrder.substring(first - 1) + teamOrder.substring(0, first - 1);
        Choice c = new Choice();
        c.action = teamOrder;
        mChoices.add(c);
    }

    public int switchChoicesCount() {
        int count = 0;
        for (Choice c : mChoices)
            if (c.action.equals(ACTION_SWITCH))
                count++;
        return count;
    }

    public boolean hasSwitchChoice(int which) {
        for (Choice c : mChoices)
            if (c.action.equals(ACTION_SWITCH) && c.index == which)
                return true;
        return false;
    }

    public String getCommand() {
        return mCommand;
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