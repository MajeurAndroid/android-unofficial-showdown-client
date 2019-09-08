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

public abstract class GlobalMessageObserver extends MessageObserver {

    private static final String TAG = GlobalMessageObserver.class.getSimpleName();

    private String mUserName;
    private boolean mIsUserGuest;

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
                processChallengeString(message.args);
                return true;
            case "updateuser":
                processUpdateUser(message.args);
                return true;
            case "nametaken":
                message.args.next(); // Skipping name
                onShowPopup(message.args.next());
                return true;
            case "queryresponse":
                processQueryResponse(message.args);
                return true;
            case "formats":
                processAvailableFormats(message.args);
                return true;
            case "popup":
                handlePopup(message.args);
                return true;
            case "updatesearch":
                processUpdateSearch(message.args);

                //final String searchStatus = messageDetail.substring(messageDetail.indexOf('|') + 1);
                //BroadcastSender.get(this).sendBroadcastFromMyApplication(
                //        BroadcastSender.EXTRA_UPDATE_SEARCH, searchStatus);
                return true;

            case "pm":
            case "usercount":
            case "updatechallenges":
                //final String challengesStatus = messageDetail.substring(messageDetail.indexOf('|') + 1);
                //BroadcastSender.get(this).sendBroadcastFromMyApplication(
                //        BroadcastSender.EXTRA_UPDATE_CHALLENGE, challengesStatus);
                return true;
            case "error":
                if (message.args.next().equals("network"))
                    onNetworkError();
                return true;
            case "init":
                onRoomInit(message.roomId, message.args.next());
                return false; // Must not consume init/deinit commands !
            case "deinit":
                onRoomDeinit(message.roomId);
                return false; // Must not consume init/deinit commands !
            default:
                return false;
        }
    }

    private void processChallengeString(ServerMessage.Args args) {
        getService().setChallengeString(args.nextTillEnd());
        getService().tryCookieSignIn();
    }

    private void processUpdateUser(ServerMessage.Args args) {
        String username = args.next().trim();
        if (username.charAt(0) == '!' || username.charAt(0) == '@')
            username = username.substring(1);
        boolean isGuest = "0".equals(args.next());
        String avatar = args.next();
        avatar = ("000" + avatar).substring(avatar.length());

        mUserName = username;
        mIsUserGuest = isGuest;
        getService().putSharedData("username", username);
        onUserChanged(username, isGuest, avatar);
    }

    private void processQueryResponse(ServerMessage.Args args) {
        String query = args.next();
        String queryContent = args.next();
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
            getService().putSharedData("userCount", jsonObject.getInt("userCount"));
            getService().putSharedData("battleCount", jsonObject.getInt("battleCount"));

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
        // Unused for now
        try {
            JSONObject jsonObject = new JSONObject(queryContent);
            String userId = jsonObject.optString("userid");
            if (userId != null && getUserId().equals(userId)) {
                String avatarId = jsonObject.getString("avatar");
                avatarId = ("000" + avatarId).substring(avatarId.length());

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processAvailableFormats(ServerMessage.Args args) {
        String rawText = args.nextTillEnd();
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
                int formatInt = Integer.valueOf(rawText.substring(innerSeparator + 1, innerSeparator + 2), 16);
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

    private void handlePopup(ServerMessage.Args args) {
        StringBuilder text = new StringBuilder(args.next());
        while (args.hasNext()) {
            String next = args.next();
            if (!TextUtils.isEmpty(next))
                text.append("\n").append(next);
        }
        onShowPopup(text.toString());
    }

    private void processUpdateSearch(ServerMessage.Args args) {
        try {
            JSONObject jsonObject = new JSONObject(args.nextTillEnd());
            JSONArray searchingArray = jsonObject.getJSONArray("searching");
            JSONObject games = jsonObject.optJSONObject("games");
            if (games == null || games.length() == 0) return;
            String[] battleRoomIds = new String[games.length()];
            String[] battleRoomNames = new String[games.length()];
            Iterator<String> iterator = games.keys();
            int i = 0;
            while (iterator.hasNext()) {
                String key = iterator.next();
                battleRoomIds[i] = key;
                battleRoomNames[i] = games.getString(key);
                i++;
            }
            onBattlesFound(battleRoomIds, battleRoomNames);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected abstract void onUserChanged(String userName, boolean isGuest, String avatarId);

    protected abstract void onBattleFormatsChanged(List<BattleFormat.Category> battleFormats);

    protected abstract void onBattlesFound(String[] battleRoomIds, String[] battleRoomNames);

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
