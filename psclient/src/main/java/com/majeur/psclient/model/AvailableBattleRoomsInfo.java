package com.majeur.psclient.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

public class AvailableBattleRoomsInfo {

    public static class RoomInfo {

        final String roomId;
        final String p1;
        final String p2;
        final int minLadder;

        public RoomInfo(String roomId, String p1, String p2, int minLadder) {
            this.roomId = roomId;
            this.p1 = p1;
            this.p2 = p2;
            this.minLadder = minLadder;
        }
    }

    private List<RoomInfo> mRooms;

    public AvailableBattleRoomsInfo(JSONObject jsonObject) throws JSONException {
        Iterator<String> iterator = jsonObject.keys();
        String key;
        while ((key = iterator.next()) != null) {
            JSONObject roomJson = jsonObject.getJSONObject(key);
            RoomInfo roomInfo = new RoomInfo(key, roomJson.getString("p1"),
                    roomJson.getString("p2"), roomJson.optInt("minElo", -1));
            mRooms.add(roomInfo);
        }
    }

    public RoomInfo[] getRooms() {
        return mRooms.toArray(new RoomInfo[0]);
    }
}
