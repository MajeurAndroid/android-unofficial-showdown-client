package com.majeur.psclient.service;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.majeur.psclient.util.TextTagSpan;
import com.majeur.psclient.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RoomMessageObserver extends MessageObserver {

    private static final String TAG = RoomMessageObserver.class.getSimpleName();

    private List<String> mCurrentUsers;
    private Map<String, Integer> mUsernameColorCache;
    private boolean mRoomJoined;

    public RoomMessageObserver() {
        mCurrentUsers = new ArrayList<>();
        mUsernameColorCache = new HashMap<>();
    }

    public boolean roomJoined() {
        return mRoomJoined;
    }

    public void sendChatMessage(String message) {
        getService().sendRoomMessage(observedRoomId(), message);
    }

    @Override
    public boolean onMessage(ServerMessage message) {
        switch (message.command) {
            case "init":
                mUsernameColorCache.clear();
                mRoomJoined = true;
                onRoomInit();
                return true;
            case "title":
                onRoomTitleChanged(message.nextArg());
                return true;
            case "users":
                initializeUserList(message);
                return true;
            case "j":
            case "join":
                String username = message.nextArg();
                mCurrentUsers.add(username);
                onUpdateUsers(mCurrentUsers);
                printUserRelatedMessage(username + " joined");
                return true;
            case "l":
            case "leave":
                username = message.nextArg();
                mCurrentUsers.remove(username);
                onUpdateUsers(mCurrentUsers);
                printUserRelatedMessage(username + " left");
                return true;
            case "html":
                // printMessage("~html messages aren't supported yet~");
                return true;
            case "uhtml":
                // printMessage("~html messages aren't supported yet~");
                return true;
            case "uhtmlchange":
                // TODO
                return true;
            case "n":
            case "name":
                handleNameChange(message);
                return true;
            case "c":
            case "chat":
                handleChatMessage(message);
                return true;
            case "c:":
                message.nextArg(); // Skipping time stamp
                handleChatMessage(message);
                return true;
            case ":":
                // Time stamp, we aren't using it yet
                return true;
            case "b":
            case "battle":
                printMessage("A battle started between XXX and YYY");
                return true;
            case "error":
                printErrorMessage(message.nextArg());
                return true;
            case "raw":
                String s = message.rawArgs();
                if (s.contains("href")) return true; // skipping complex Html formatted messages
                printMessage(Html.fromHtml(s));
                return true;
            case "deinit":
                mRoomJoined = false;
                onRoomDeInit();
                return true;
            case "noinit":
                //TODO
                return true;
            default:
                return false;
        }
    }

    private void initializeUserList(ServerMessage args) {
        String rawUsers = args.nextArg();
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

    private void handleNameChange(ServerMessage args) {
        String user = args.nextArg();
        String oldName = args.nextArg();
        printUserRelatedMessage("User " + oldName + " changed its name and is now " + user);
    }

    private void handleChatMessage(ServerMessage args) {
        String user = args.nextArg().substring(1);
        String userMessage = args.rawArgs();

        Spannable spannable = new SpannableString(user + ": " + userMessage);
        int textColor = obtainUsernameColor(user);
        Object span = new TextTagSpan(Utils.getTagColor(textColor), textColor);
        spannable.setSpan(span, 0, user.length() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        printMessage(spannable);
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
        printMessage(spannable);
    }

    protected void printErrorMessage(String message) {
        Spannable spannable = new SpannableString(message);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        printMessage(spannable);
    }

    protected void printMessage(CharSequence text) {
        onPrintText(text);
    }

    protected abstract void onRoomInit();

    protected abstract void onPrintText(CharSequence text);

    protected abstract void onRoomTitleChanged(String title);

    protected abstract void onUpdateUsers(List<String> users);

    protected abstract void onRoomDeInit();
}
