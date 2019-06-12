package com.majeur.psclient.service;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import com.majeur.psclient.util.TextTagSpan;
import com.majeur.psclient.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RoomMessageHandler extends ShowdownMessageHandler {

    private static final String TAG = RoomMessageHandler.class.getSimpleName();

    protected String mRoomId;
    private List<String> mCurrentUsers;
    private Map<String, Integer> mUsernameColorCache;
    private boolean mRoomJoined;

    public RoomMessageHandler() {
        mCurrentUsers = new ArrayList<>();
        mUsernameColorCache = new HashMap<>();
    }

    public boolean roomJoined() {
        return mRoomJoined;
    }

    public void joinRoom(String roomId) {
        if (roomId == null) return;
        if (mRoomJoined) {
            Log.e(TAG, "Trying to join a new room without leaving current joined, ignoring request");
            return;
        }

        mRoomId = roomId;
        getShowdownService().sendGlobalCommand("join", roomId);
    }

    protected void setAutoJoinedRoom(String roomId) {
        leaveRoomNow();
        mRoomId = roomId;
    }

    public void leaveRoom() {
        getShowdownService().sendGlobalCommand("leave", mRoomId);
    }

    protected void leaveRoomNow() {
        if (mRoomId == null) return;
        leaveRoom();
        mRoomJoined = false;
        onRoomDeInit();
        mRoomId = null;
    }

    public void requestAvailableRoomsInfo() {
        getShowdownService().sendGlobalCommand("cmd", "rooms");
    }

    public void sendChatMessage(String message) {
        getShowdownService().sendRoomMessage(mRoomId, message);
    }

    @Override
    public boolean shouldHandleMessages(String messages) {
        return mRoomId != null && ((mRoomId.equals("lobby") && messages.charAt(0) != '>') || messages.startsWith(">" + mRoomId));
    }

    @Override
    protected void onHandleHeader(String header) {
        // Room id header, pass
    }

    @Override
    public void onHandleMessage(MessageIterator message) {
        processCommand(message);
    }

    public abstract void onPrintText(CharSequence text);

    public void onPrintText(String text) {
        onPrintText(new SpannedString(text));
    }

    private void processCommand(MessageIterator message) {
        String command = message.next().toLowerCase();
        switch (command) {
            case "init":
                mUsernameColorCache.clear();
                mRoomJoined = true;
                onRoomInit();
                break;
            case "title":
                onRoomTitleChanged(message.next());
                break;
            case "users":
                initializeUserList(message);
                break;
            case "j":
            case "join":
                String username = message.next();
                mCurrentUsers.add(username);
                onUpdateUsers(mCurrentUsers);
                printUserRelatedMessage(username + " joined");
                break;
            case "l":
            case "leave":
                username = message.next();
                mCurrentUsers.remove(username);
                onUpdateUsers(mCurrentUsers);
                printUserRelatedMessage(username + " leaved");
                break;
            case "html":
                // onPrintText("~html messages aren't supported yet~");
                break;
            case "uhtml":
                // onPrintText("~uhtml messages aren't supported yet~");
                break;
            case "uhtmlchange":
                // TODO
                break;
            case "n":
            case "name":
                handleNameChange(message);
                break;
            case "c":
            case "chat":
                handleChatMessage(message);
                break;
            case "c:":
                message.next(); // Skipping time stamp
                handleChatMessage(message);
                break;
            case ":":
                // Time stamp, we aren't using it yet
                break;
            case "b":
            case "battle":
                onPrintText("A battle started between XXX and YYY");
                break;
            case "error":
                printErrorMessage(message.next());
                break;
            case "raw":
                String s = message.next();
                if (s.contains("http")) break; // skipping complex Html formatted messages
                onPrintText(Html.fromHtml(s));
                break;
            case "deinit":
                mRoomJoined = false;
                onRoomDeInit();
                mRoomId = null;
                break;
            case "noinit":
                //TODO
                break;
        }
    }

    private void initializeUserList(MessageIterator message) {
        String rawUsers = message.next();
        int separator = rawUsers.indexOf(',');
        int count = Integer.parseInt(rawUsers.substring(0, separator));
        mCurrentUsers = new ArrayList<>(count);
        rawUsers = rawUsers.substring(separator + 1);
        // We substring from 1 to avoid username prefixes
        while ((separator = rawUsers.indexOf(',')) != -1) {
            mCurrentUsers.add(rawUsers.substring(1, separator));
            rawUsers = rawUsers.substring(separator + 1);
        }
        mCurrentUsers.add(rawUsers.substring(1));
        onUpdateUsers(mCurrentUsers);
    }

    private void handleNameChange(MessageIterator message) {
        String user = message.next();
        String oldName = message.next();
        printUserRelatedMessage("User " + oldName + " changed its name and is now " + user);
    }

    private void handleChatMessage(MessageIterator message) {
        String user = message.next().substring(1);
        String userMessage = message.nextTillEnd();

        Spannable spannable = new SpannableString(user + ": " + userMessage);
        int textColor = obtainUsernameColor(user);
        Object span = new TextTagSpan(Utils.getTagColor(textColor), textColor);
        spannable.setSpan(span, 0, user.length() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        onPrintText(spannable);
    }

    private int obtainUsernameColor(String username) {
        Integer usernameColor = mUsernameColorCache.get(username);
        if (usernameColor == null) {
            usernameColor = Utils.hashColor(username);
            mUsernameColorCache.put(username, usernameColor);
        }
        return usernameColor;
    }

    private void printUserRelatedMessage(String message) {
        if (mCurrentUsers.size() >= 8)
            return;

        Spannable spannable = new SpannableString(message);
        spannable.setSpan(new StyleSpan(Typeface.ITALIC), 0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(0xFF424242), 0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(0.8f), 0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        onPrintText(spannable);
    }

    private void printErrorMessage(String message) {
        Spannable spannable = new SpannableString(message);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        onPrintText(spannable);
    }

    public abstract void onRoomInit();

    public abstract void onRoomTitleChanged(String title);

    public abstract void onUpdateUsers(List<String> users);

    public abstract void onRoomDeInit();
}
