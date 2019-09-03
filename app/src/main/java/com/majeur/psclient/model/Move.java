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
    public ExtraInfo extraInfo;


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
                ", extraInfo=" + extraInfo +
                '}';
    }

    public static class ExtraInfo {
        public final int accuracy;
        public final int priority;
        public final int basePower;
        public final int color;
        public final String category;
        public final String desc;
        public final String type;

        public ExtraInfo(int accuracy, int priority, int basePower, String category, String desc, String type) {
            this.accuracy = accuracy;
            this.priority = priority;
            this.basePower = basePower;
            this.category = category;
            this.desc = desc;
            this.type = type;
            this.color = Colors.typeColor(toId(type));
        }

    }
}
