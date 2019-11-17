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
    public final String target;
    public final boolean disabled;
    public final String zName;
    public Details details;
    public Details zDetails;

    // Flag to know if this move should be read as
    // zmove or regular base one
    public boolean zflag;

    public Move(int index, JSONObject jsonObject, @Nullable JSONObject zJsonObject) throws JSONException {
        this.index = index;
        name = jsonObject.getString("move").replace("Hidden Power", "HP");
        id = jsonObject.getString("id");
        pp = jsonObject.optInt("pp", -1);
        ppMax = jsonObject.optInt("maxpp", -1);
        target = jsonObject.optString("target", null);
        disabled = jsonObject.optBoolean("disabled", false);
        zName = zJsonObject != null ? zJsonObject.optString("move", null) : null;
    }

    public boolean canZMove() {
        return zName != null;
    }

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

        public Details(String name, int accuracy, int priority, int basePower, int zPower,
                       String category, String desc, String type, int pp, String target,
                       String zEffect) {
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
            this.target = parse(target);
            this.zEffect = zEffect;
        }

        private Target parse(String target) {
            if (target == null) return Target.NORMAL;
            target = target.toLowerCase().trim();
            switch (target) {
                case "normal": return Target.NORMAL;
                case "allyside": return Target.ALLYSIDE;
                case "self": return Target.SELF;
                default: return Target.NORMAL;
            }
        }
    }

    public enum Target {
        NORMAL, ALLYSIDE, SELF
    }
}
