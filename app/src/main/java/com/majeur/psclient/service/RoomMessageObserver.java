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
                onRoomTitleChanged(message.args.next());
                return true;
            case "users":
                initializeUserList(message.args);
                return true;
            case "j":
            case "join":
                String username = message.args.next();
                mCurrentUsers.add(username);
                onUpdateUsers(mCurrentUsers);
                printUserRelatedMessage(username + " joined");
                return true;
            case "l":
            case "leave":
                username = message.args.next();
                mCurrentUsers.remove(username);
                onUpdateUsers(mCurrentUsers);
                printUserRelatedMessage(username + " leaved");
                return true;
            case "html":
                // onPrintText("~html messages aren't supported yet~");
                return true;
            case "uhtml":
                // onPrintText("~uhtml messages aren't supported yet~");
                return true;
            case "uhtmlchange":
                // TODO
                return true;
            case "n":
            case "name":
                handleNameChange(message.args);
                return true;
            case "c":
            case "chat":
                handleChatMessage(message.args);
                return true;
            case "c:":
                message.args.next(); // Skipping time stamp
                handleChatMessage(message.args);
                return true;
            case ":":
                // Time stamp, we aren't using it yet
                return true;
            case "b":
            case "battle":
                onPrintText("A battle started between XXX and YYY");
                return true;
            case "error":
                printErrorMessage(message.args.next());
                return true;
            case "raw":
                String s = message.args.nextTillEnd();
                if (s.contains("href")) return true; // skipping complex Html formatted messages
                onPrintText(Html.fromHtml(s));
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

    private void initializeUserList(ServerMessage.Args args) {
        String rawUsers = args.next();
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

    private void handleNameChange(ServerMessage.Args args) {
        String user = args.next();
        String oldName = args.next();
        printUserRelatedMessage("User " + oldName + " changed its name and is now " + user);
    }

    private void handleChatMessage(ServerMessage.Args args) {
        String user = args.next().substring(1);
        String userMessage = args.nextTillEnd();

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

    public abstract void onPrintText(CharSequence text);

    public void onPrintText(String text) {
        onPrintText(new SpannedString(text));
    }

    public abstract void onRoomTitleChanged(String title);

    public abstract void onUpdateUsers(List<String> users);

    public abstract void onRoomDeInit();
}
