package com.majeur.psclient.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BattleFormat implements Serializable {

    private static final int MASK_TEAM = 0x1;
    private static final int MASK_SEARCH_SHOW_BIT = 0x2;
    private static final int MASK_CHALLENGE_SHOW = 0x4;
    private static final int MASK_TOURNAMENT_SHOW = 0x8;

    private String mLabel;
    private int mFormatInt;

    public BattleFormat(String label, int type) {
        mLabel = label;
        mFormatInt = type;
//        label.setCanSearch(((formatInt & MASK_SEARCH_SHOW_BIT) > 0));
//        label.setCanChallenge(((formatInt & MASK_CHALLENGE_SHOW) > 0));
//        label.setCanTournament(((formatInt & MASK_TOURNAMENT_SHOW) > 0));

    }

    public String getLabel() {
        return mLabel;
    }

    public boolean isTeamNeeded() {
        return (mFormatInt & MASK_TEAM) == 0;
    }

    public static class Category implements Serializable {

        private String mLabel;
        private List<BattleFormat> mBattleFormats;

        public Category() {
            mBattleFormats = new ArrayList<>();
        }

        public void addBattleFormat(BattleFormat battleFormat) {
            mBattleFormats.add(battleFormat);
        }

        public void setLabel(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }

        public List<BattleFormat> getBattleFormats() {
            return mBattleFormats;
        }
    }
}
