package com.majeur.psclient.service;

import android.util.Log;

import com.majeur.psclient.model.AvailableBattleRoomsInfo;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.RoomInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class GlobalMessageHandler extends ShowdownMessageHandler {

    private static final String TAG = GlobalMessageHandler.class.getSimpleName();

    private String mUserName;

    private String getUserId() {
        return mUserName.toLowerCase().replace("[^a-z0-9]", "");
    }

    public boolean isUserGuest() {
        return getUserId().startsWith("guest");
    }

    public void retryServerConnection() {
        getShowdownService().retryShowdownServerConnection();
    }

    public void searchForBattle() {
        getShowdownService().sendGlobalCommand("utm", "null");
        getShowdownService().sendGlobalCommand("search", "gen7randombattle");
    }

    @Override
    protected int getPriority() {
        return 1;
    }

    @Override
    public boolean shouldHandleMessages(String messages) {
        return messages.charAt(0) != '>';
    }

    @Override
    public void onHandleMessage(MessageIterator message) {
        String command = message.next();
        switch (command) {
            case "challstr":
                processChallengeString(message);
                break;
            case "updateuser":
                processUpdateUser(message);
                break;
            case "queryresponse":
                processQueryResponse(message);
                break;
            case "formats":
                processAvailableFormats(message);
                break;
            case "popup":
                onShowPopup(message.next());
                break;
            case "updatesearch":
                processUpdateSearch(message);

                //final String searchStatus = messageDetail.substring(messageDetail.indexOf('|') + 1);
                //BroadcastSender.get(this).sendBroadcastFromMyApplication(
                //        BroadcastSender.EXTRA_UPDATE_SEARCH, searchStatus);
                break;

            case "pm":
            case "usercount":
            case "updatechallenges":
                //final String challengesStatus = messageDetail.substring(messageDetail.indexOf('|') + 1);
                //BroadcastSender.get(this).sendBroadcastFromMyApplication(
                //        BroadcastSender.EXTRA_UPDATE_CHALLENGE, challengesStatus);
                break;
        }
    }

    private void processChallengeString(MessageIterator message) {
        getShowdownService().setChallengeString(message.nextTillEnd());
        getShowdownService().tryCookieSignIn();
    }

    private void processUpdateUser(MessageIterator message) {
        String username = message.next();
        boolean isGuest = "0".equals(message.next());
        String avatar = message.next();
        avatar = ("000" + avatar).substring(avatar.length());

        mUserName = username;
        getShowdownService().putSharedData("username", username);
        onUserChanged(username, isGuest, avatar);
    }

    private void processQueryResponse(MessageIterator message) {
        String query = message.next();
        String queryContent = message.next();
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
            getShowdownService().putSharedData("userCount", jsonObject.getInt("userCount"));
            getShowdownService().putSharedData("battleCount", jsonObject.getInt("battleCount"));

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
            if (userId != null && getUserId().equals(userId)) {
                String avatarId = jsonObject.getString("avatar");
                avatarId = ("000" + avatarId).substring(avatarId.length());
                onUserChanged(mUserName, userId.contains("guest"), avatarId);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processAvailableFormats(MessageIterator message) {
        String rawText = message.nextTillEnd();
        List<BattleFormat.Category> battleFormatCategories = new LinkedList<>();
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

        getShowdownService().putSharedData("formats", battleFormatCategories);

        onBattleFormatsChanged(battleFormatCategories);
    }

    private void processUpdateSearch(MessageIterator message) {
        try {
            JSONObject jsonObject = new JSONObject(message.nextTillEnd());
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

    public void fakeBattle() {
        getShowdownService().fakeBattle();
    }
}
