package com.majeur.psclient.model;

import java.io.Serializable;
import java.util.Comparator;

public class RoomInfo implements Serializable {

    public static final Comparator<RoomInfo> COMPARATOR = new Comparator<RoomInfo>() {
        @Override
        public int compare(RoomInfo ri1, RoomInfo ri2) {
            return -Integer.compare(ri1.userCount, ri2.userCount);
        }
    };

    public final String name;
    public final String description;
    public final int userCount;

    public RoomInfo(String name, String descr, int userCount) {
        this.name = name;
        this.userCount = userCount;
        description = descr;
    }
}
