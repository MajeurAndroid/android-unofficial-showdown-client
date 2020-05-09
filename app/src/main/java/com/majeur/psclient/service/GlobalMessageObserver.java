package com.majeur.psclient.service;

import android.text.TextUtils;
import android.util.Log;
import com.majeur.psclient.model.AvailableBattleRoomsInfo;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.RoomInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.majeur.psclient.util.Utils.jsonObject;

public abstract class GlobalMessageObserver extends AbsMessageObserver {

    private static final String TAG = GlobalMessageObserver.class.getSimpleName();

    private String mUserName;
    private boolean mIsUserGuest;
    private boolean mRequestServerCountsOnly;

    public GlobalMessageObserver() {
        setObserveAll(true);
        mIsUserGuest = true;
    }

    private String getUserId() {
        return mUserName.toLowerCase().replace("[^a-z0-9]", "");
    }

    public boolean isUserGuest() {
        return mIsUserGuest;
    }

    @Override
    public boolean onMessage(ServerMessage message) {
        switch (message.command) {
            case "challstr":
                processChallengeString(message);
                return true;
            case "updateuser":
                processUpdateUser(message);
                return true;
            case "nametaken":
                message.nextArg(); // Skipping name
                onShowPopup(message.nextArg());
                return true;
            case "queryresponse":
                processQueryResponse(message);
                return true;
            case "formats":
                processAvailableFormats(message);
                return true;
            case "popup":
                handlePopup(message);
                return true;
            case "updatesearch":
                handleUpdateSearch(message);
                return true;

            case "pm":
            case "usercount":
            case "updatechallenges":
                //final String challengesStatus = messageDetail.substring(messageDetail.indexOf('|') + 1);
                //BroadcastSender.get(this).sendBroadcastFromMyApplication(
                //        BroadcastSender.EXTRA_UPDATE_CHALLENGE, challengesStatus);
                return true;
            case "error":
                if (message.nextArg().equals("network"))
                    onNetworkError();
                return true;
            case "init":
                onRoomInit(message.roomId, message.nextArg());
                return false; // Must not consume init/deinit commands !
            case "deinit":
                onRoomDeinit(message.roomId);
                return false; // Must not consume init/deinit commands !
            case "noinit":
                if (message.hasNextArg() && "nonexistent".equals(message.nextArg()) && message.hasNextArg())
                    onShowPopup(message.nextArg());
                return true;
            default:
                return false;
        }
    }

    private void processChallengeString(ServerMessage msg) {
        getService().setChallengeString(msg.rawArgs());
        getService().tryCookieSignIn();
    }

    private void processUpdateUser(ServerMessage msg) {
        String username = msg.nextArg();
        String userType = username.substring(0, 1);
        username = username.substring(1);
        boolean isGuest = "0".equals(msg.nextArg());
        String avatar = msg.nextArg();
        avatar = ("000" + avatar).substring(avatar.length());

        mUserName = username;
        mIsUserGuest = isGuest;
        getService().putSharedData("username", username);
        onUserChanged(username, isGuest, avatar);

        // Update server counts (active battle and active users)
        mRequestServerCountsOnly = true;
        getService().sendGlobalCommand("cmd", "rooms");

        // onSearchBattlesChanged(new String[0], new String[0], new String[0]); TODO Wtf was this call ?
    }

    private void processQueryResponse(ServerMessage msg) {
        String query = msg.nextArg();
        String queryContent = msg.nextArg();
        switch (query) {
            case "rooms":
                processRoomsQueryResponse(queryContent);
                break;
            case "roomlist":
                processRoomListQueryResponse(queryContent);
                break;
            case "savereplay":
                // Not supported yet
                // queryContent is a JSON object formatted as follows:
                // {"log":"actual battle log","id":"gen7randombattle-858101987"}
                break;
            case "userdetails":
                processUserDetailsQueryResponse(queryContent);
                break;
            default:
                Log.w(TAG, "Command queryresponse not handled, type=" + query);
                break;
        }
    }

    private void processRoomsQueryResponse(String queryContent) {
        if (queryContent.equals("null"))
            return;

        try {
            JSONObject jsonObject = new JSONObject(queryContent);
            int userCount = jsonObject.getInt("userCount");
            int battleCount = jsonObject.getInt("battleCount");
            onUpdateCounts(userCount, battleCount);
            if (mRequestServerCountsOnly) {
                mRequestServerCountsOnly = false;
                return;
            }

            JSONArray jsonArray = jsonObject.getJSONArray("official");
            int N = jsonArray.length();
            RoomInfo[] officialRooms = new RoomInfo[N];
            for (int i = 0; i < N; i++) {
                JSONObject roomJson = jsonArray.getJSONObject(i);
                Log.e("s", roomJson.getString("title"));
                officialRooms[i] = new RoomInfo(roomJson.getString("title"),
                        roomJson.getString("desc"), roomJson.getInt("userCount"));
            }

            jsonArray = jsonObject.getJSONArray("chat");
            N = jsonArray.length();
            RoomInfo[] chatRooms = new RoomInfo[N];
            for (int i = 0; i < N; i++) {
                JSONObject roomJson = jsonArray.getJSONObject(i);
                chatRooms[i] = new RoomInfo(roomJson.getString("title"),
                        roomJson.getString("desc"), roomJson.getInt("userCount"));
            }

            onAvailableRoomsChanged(officialRooms, chatRooms);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processRoomListQueryResponse(String queryContent) {
        if (queryContent.equals("null"))
            return;

        try {

            new JSONObject(queryContent);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processUserDetailsQueryResponse(String queryContent) {
        try {
            JSONObject jsonObject = new JSONObject(queryContent);
            String userId = jsonObject.optString("userid");
            if (userId == null) return;

            String name = jsonObject.optString("name");
            boolean online = jsonObject.has("status");
            String group = jsonObject.optString("group");
            Object rooms = jsonObject.opt("rooms");
            List<String> chatRooms = new LinkedList<>();
            List<String> battles = new LinkedList<>();
            if (rooms instanceof JSONObject) {
                Iterator<String> iterator = ((JSONObject) rooms).keys();
                while (iterator.hasNext()) {
                    String roomId = iterator.next();
                    if (roomId.contains("battle-")) battles.add(roomId);
                    else chatRooms.add(roomId);
                }
            }

            onUserDetails(userId, name, online, group, chatRooms, battles);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processAvailableFormats(ServerMessage msg) {
        String rawText = msg.rawArgs();
        List<BattleFormat.Category> battleFormatCategories = new LinkedList<>(); // /!\ needs to impl Serializable
        BattleFormat.Category currentCategory = null;
        int separator;
        while (true) {
            separator = rawText.indexOf('|');
            if (rawText.charAt(0) == ',') {
                rawText = rawText.substring(separator + 1); // Ignoring ",1|" part
                separator = rawText.indexOf('|');
                currentCategory = new BattleFormat.Category();
                battleFormatCategories.add(currentCategory);
                String categoryLabel = rawText.substring(0, separator);
                currentCategory.setLabel(categoryLabel);

            } else {
                int innerSeparator = rawText.indexOf(',');
                String formatName = rawText.substring(0, innerSeparator);
                int innerEnd = separator == -1 ? rawText.length() : separator;
                int formatInt = Integer.valueOf(rawText.substring(innerSeparator + 1, innerEnd), 16);
                BattleFormat battleFormat = new BattleFormat(formatName, formatInt);
                currentCategory.addBattleFormat(battleFormat);

            }
            if (separator == -1)
                break;
            rawText = rawText.substring(separator + 1);
        }

        getService().putSharedData("formats", battleFormatCategories);
        onBattleFormatsChanged(battleFormatCategories);
    }

    private void handlePopup(ServerMessage msg) {
        StringBuilder text = new StringBuilder(msg.nextArg());
        while (msg.hasNextArg()) {
            String next = msg.nextArg();
            if (!TextUtils.isEmpty(next))
                text.append("\n").append(next);
        }
        onShowPopup(text.toString());
    }

    private void handleUpdateSearch(ServerMessage msg) {
        JSONObject jsonObject = jsonObject(msg.rawArgs());
        if (jsonObject == null) return;
        String[] searching;
        JSONArray jsonArray = jsonObject.optJSONArray("searching");
        if (jsonArray == null) {
            searching = new String[0];
        } else {
            searching = new String[jsonArray.length()];
            for (int i = 0; i < searching.length; i++)
                searching[i] = jsonArray.optString(i);
        }
        JSONObject games = jsonObject.optJSONObject("games");
        String[] battleRoomIds;
        String[] battleRoomNames;
        if (games == null) {
            battleRoomIds = new String[0];
            battleRoomNames = new String[0];
        } else {
            battleRoomIds = new String[games.length()];
            battleRoomNames = new String[games.length()];
            Iterator<String> iterator = games.keys();
            for (int i = 0; iterator.hasNext(); i++) {
                String key = iterator.next();
                battleRoomIds[i] = key;
                battleRoomNames[i] = games.optString(key);
            }
        }
        onSearchBattlesChanged(searching, battleRoomIds, battleRoomNames);
    }

    protected abstract void onUserChanged(String userName, boolean isGuest, String avatarId);

    protected abstract void onUpdateCounts(int userCount, int battleCount);

    protected abstract void onBattleFormatsChanged(List<BattleFormat.Category> battleFormats);

    protected abstract void onSearchBattlesChanged(String[] searching, String[] battleRoomIds, String[] battleRoomNames);

    protected abstract void onUserDetails(String id, String name, boolean online, String group, List<String> rooms, List<String> battles);

    protected abstract void onShowPopup(String message);

    protected abstract void onAvailableRoomsChanged(RoomInfo[] officialRooms, RoomInfo[] chatRooms);

    protected abstract void onAvailableBattleRoomsChanged(AvailableBattleRoomsInfo availableRoomsInfo);

    protected abstract void onRoomInit(String roomId, String type);

    protected abstract void onRoomDeinit(String roomId);

    protected abstract void onNetworkError();

    public void fakeBattle() {
        getService().fakeBattle();
    }
}
