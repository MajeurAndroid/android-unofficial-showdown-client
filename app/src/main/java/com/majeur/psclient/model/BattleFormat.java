package com.majeur.psclient.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.model.Id.toIdSafe;

public class BattleFormat implements Serializable {

    public static final BattleFormat FORMAT_OTHER = new BattleFormat("[Other]", -1);

    public static int compare(List<Category> formats, String f1, String f2) {
        if (Objects.equals(f1, f2)) return 0;
        if (f1.contains("other")) return 1;
        if (f2.contains("other")) return -1;
        if (formats == null) return f1.compareTo(f2);
        int f1Index = -1;
        int f2Index = -1;
        int index = 0;
        loop: for (Category category : formats)
            for (BattleFormat format : category.getBattleFormats()) {
            String id = format.id();
            if (id.equals(f1)) f1Index = index;
            if (id.equals(f2)) f2Index = index;
            if (f1Index >= 0 && f2Index >= 0) break loop;
            index++;
        }
        return Integer.compare(f1Index, f2Index);
    }

    public static String resolveName(List<BattleFormat.Category> formats, String formatId) {
        if (formats == null) return formatId;
        if ("other".equals(formatId)) return "Other";
        for (BattleFormat.Category category : formats)
            for (BattleFormat format : category.getBattleFormats())
                if (format.id().contains(formatId)) return format.getLabel();
        return formatId;
    }

    private static final int MASK_TEAM = 0x1;
    private static final int MASK_SEARCH_SHOW = 0x2;
    private static final int MASK_CHALLENGE_SHOW = 0x4;
    private static final int MASK_TOURNAMENT_SHOW = 0x8;

    private final String mLabel;
    private final int mFormatInt;

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

    public boolean isSearchShow() {
        return (mFormatInt & MASK_SEARCH_SHOW) == 0;
    }

    public String id() {
        return toId(mLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BattleFormat that = (BattleFormat) o;
        return Objects.equals(toIdSafe(mLabel), toIdSafe(that.mLabel));
    }

    public static class Category implements Serializable {

        private String mLabel;

        public void setBattleFormats(List<BattleFormat> battleFormats) {
            mBattleFormats = battleFormats;
        }

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

        public List<BattleFormat> getSearchableBattleFormats() {
            List<BattleFormat> list = new ArrayList<>();
            for (BattleFormat format : mBattleFormats)
                if (!format.isSearchShow()) list.add(format);
            return list;
        }
    }
}
