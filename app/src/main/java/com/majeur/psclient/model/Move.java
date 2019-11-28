package com.majeur.psclient.model;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.Nullable;

import static com.majeur.psclient.model.Id.toId;

public class Move {

    public final String name;
    public final String id;
    public final int index;
    public final int pp;
    public final int ppMax;
    public final Target target;
    public final boolean disabled;
    public final String zName;
    public Details details;
    public Details zDetails;

    public String maxMoveId;
    public Target maxMoveTarget;
    public Details maxDetails;

    // Flag to know if this move should be read as
    // zmove or regular base one
    public boolean zflag;
    public boolean maxflag;

    public Move(int index, JSONObject jsonObject, @Nullable JSONObject zJsonObject, JSONObject maxJsonObject) throws JSONException {
        this.index = index;
        name = jsonObject.getString("move").replace("Hidden Power", "HP");
        id = jsonObject.getString("id");
        pp = jsonObject.optInt("pp", -1);
        ppMax = jsonObject.optInt("maxpp", -1);
        target = Target.parse(jsonObject.optString("target", null));
        disabled = jsonObject.optBoolean("disabled", false);
        zName = zJsonObject != null ? zJsonObject.optString("move", null) : null;
        maxMoveId = maxJsonObject != null ? maxJsonObject.optString("move", null) : null;
        maxMoveTarget = maxJsonObject != null ? Target.parse(maxJsonObject.optString("target", null)) : null;
    }

    public boolean canZMove() {
        return zName != null;
    }

    public String maxMoveName() {
        for (String name : MAX_MOVES)
            if (toId(name).equals(maxMoveId))
                return name;
        return maxMoveId;
//        switch (details.type) {
//                case "Poison":  return "Max Ooze";
//                case "Fighting": return "Max Knuckle";
//                case "Dark": return "Max Darkness";
//                case "Grass": return "Max Overgrowth";
//                case "Normal": return "Max Strike";
//                case "Rock": return "Max Rockfall";
//                case "Steel": return "Max Steelspike";
//                case "Dragon": return "Max Wyrmwind";
//                case "Electric": return "Max Lightning";
//                case "Water": return "Max Geyser";
//                case "Fire": return "Max Flare";
//                case "Ghost": return "Max Phantasm";
//                case "Bug": return "Max Flutterby";
//                case "Psychic": return "Max Mindstorm";
//                case "Ice": return "Max Hailstorm";
//                case "Flying": return "Max Airstream";
//                case "Ground": return "Max Quake";
//                case "Fairy": return "Max Starfall";
//                case "???": return "";
//                default: return null;
//        }
    }

    private static String[] MAX_MOVES = {"Max Guard", "Max Ooze", "Max Knuckle", "Max Darkness",
            "Max Overgrowth", "Max Strike", "Max Rockfall", "Max Steelspike", "Max Wyrmwind",
            "Max Lightning", "Max Geyser", "Max Flare", "Max Phantasm", "Max Flutterby", "Max Mindstorm",
            "Max Hailstorm", "Max Airstream", "Max Quake", "Max Starfall"};

    @Override
    public String toString() {
        return "Move{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", index=" + index +
                ", pp=" + pp +
                ", ppMax=" + ppMax +
                ", target='" + target + '\'' +
                ", disabled=" + disabled +
                ", zName='" + zName + '\'' +
                ", details=" + details +
                '}';
    }

    public static class Details {

        public final String name;
        public final int accuracy;
        public final int priority;
        public final int basePower;
        public final int zPower;
        public final int color;
        public final String category;
        public final String desc;
        public final String type;
        public final int pp;
        public final Target target;
        public final String zEffect;
        public final int maxPower;

        public Details(String name, int accuracy, int priority, int basePower, int zPower,
                       String category, String desc, String type, int pp, String target,
                       String zEffect, int maxPower) {
            this.name = name;
            this.accuracy = accuracy;
            this.priority = priority;
            this.basePower = basePower;
            this.zPower = zPower;
            this.category = category;
            this.desc = desc;
            this.type = type;
            this.color = Colors.typeColor(toId(type));
            this.pp = pp;
            this.target = Target.parse(target);
            this.zEffect = zEffect;
            this.maxPower = maxPower;
        }

        @Override
        public String toString() {
            return "Details{" +
                    "name='" + name + '\'' +
                    ", accuracy=" + accuracy +
                    ", priority=" + priority +
                    ", basePower=" + basePower +
                    ", zPower=" + zPower +
                    ", color=" + color +
                    ", category='" + category + '\'' +
                    ", desc='" + desc + '\'' +
                    ", type='" + type + '\'' +
                    ", pp=" + pp +
                    ", target=" + target +
                    ", zEffect='" + zEffect + '\'' +
                    '}';
        }
    }

    public enum Target {

        NORMAL,
        ALL_ADJACENT_FOES,
        SELF,
        ANY,
        ADJACENT_ALLY_OR_SELF,
        ALLY_TEAM,
        ADJACENT_ALLY,
        ALLY_SIDE,
        ALL_ADJACENT,
        SCRIPTED,
        ALL,
        ADJACENT_FOE,
        RANDOM_NORMAL,
        FOE_SIDE;

        public static Target parse(String target) {
            if (target == null) return Target.NORMAL;
            target = target.toLowerCase().trim();
            switch (target) {
                case "normal": return Target.NORMAL;
                case "alladjacentfoes": return Target.ALL_ADJACENT_FOES;
                case "self": return Target.SELF;
                case "any": return Target.ANY;
                case "adjacentallyorself": return Target.ADJACENT_ALLY_OR_SELF;
                case "allyteam": return Target.ALLY_TEAM;
                case "adjacentally": return Target.ADJACENT_ALLY;
                case "allyside": return Target.ALLY_SIDE;
                case "alladjacent": return Target.ALL_ADJACENT;
                case "scripted": return Target.SCRIPTED;
                case "all": return Target.ALL;
                case "adjacentfoe": return Target.ADJACENT_FOE;
                case "randomnormal": return Target.RANDOM_NORMAL;
                case "foeside": return Target.FOE_SIDE;
                default: return Target.NORMAL;
            }
        }


        //  Triples       Doubles     Singles
        //  3  2  1         2  1         1
        // -1 -2 -3        -1 -2        -1
        public static boolean[][] computeTargetAvailabilities(Target target, int position, int pokeCount) {
            boolean[][] availabilities = new boolean[2][pokeCount];
            for (int i = 0; i < 2; i++)
                for (int j = 0; j < pokeCount; j++) availabilities[i][j] = true;

            if (target == ADJACENT_FOE) {
                for (int i = 0; i < pokeCount; i++) {
                    if (position != i && position != i + 1 && position != i - 1) availabilities[0][i] = false;
                    availabilities[1][i] = false;
                }
            }
            if (target == ADJACENT_ALLY || target == ADJACENT_ALLY_OR_SELF) {
                for (int i = 0; i < pokeCount; i++) {
                    if (position != i && position != i + 1 && position != i - 1) availabilities[1][i] = false;
                    availabilities[0][i] = false;
                }
                if (target == ADJACENT_ALLY) availabilities[1][position] = false;
            }
            if (target == NORMAL) {
                availabilities[1][position] = false;
            }
            return availabilities;
        }

        public boolean isChosable() {
            return this == NORMAL || this == ANY || this == ADJACENT_ALLY
                    || this == ADJACENT_ALLY_OR_SELF || this == ADJACENT_FOE;
        }
    }
}
