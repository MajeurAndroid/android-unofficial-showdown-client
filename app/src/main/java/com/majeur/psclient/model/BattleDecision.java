package com.majeur.psclient.model;

import java.util.LinkedList;
import java.util.List;

import static com.majeur.psclient.util.Utils.str;

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
    private final List<Choice> mChoices = new LinkedList<>();

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

    public void addLeadChoice(int first, int teamSize) {
        mCommand = CMD_TEAM;
        Choice c = new Choice();
        c.index = first;
        c.target = teamSize;
        mChoices.add(c);
    }

    public int leadChoicesCount() {
        int count = 0;
        for (Choice c : mChoices)
            if (c.action == null)
                count++;
        return count;
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

    public boolean hasOnlyPassChoice() {
        for (Choice c : mChoices)
            if (!c.action.equals(ACTION_PASS))
                return false;
        return true;
    }

    public String getCommand() {
        return mCommand;
    }

    public String build() {
        if (mCommand.equals(CMD_TEAM)) return buildLeadString();
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

    private String buildLeadString() {
        StringBuilder builder = new StringBuilder();
        int teamSize = 0;
        for (Choice c : mChoices) {
            builder.append(c.index);
            teamSize = c.target;
        }
        for (int i = 1; i <= teamSize; i++)
            if (builder.indexOf(str(i)) < 0) builder.append(i);
        return builder.toString();
    }
}