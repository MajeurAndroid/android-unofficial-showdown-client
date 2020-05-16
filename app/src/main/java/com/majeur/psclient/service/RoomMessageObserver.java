package com.majeur.psclient.service;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import com.majeur.psclient.model.User;
import com.majeur.psclient.util.SpannableStringBuilder;
import com.majeur.psclient.util.TextTagSpan;
import com.majeur.psclient.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.graphics.Color.parseColor;
import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.util.Utils.applyStylingTags;
import static com.majeur.psclient.util.Utils.prepareForHtml;
import static com.majeur.psclient.util.Utils.specChars;

public abstract class RoomMessageObserver extends AbsMessageObserver {

    private static final String TAG = RoomMessageObserver.class.getSimpleName();

    private List<String> mCurrentUsers;
    private final Map<String, Integer> mUsernameColorCache;
    private boolean mRoomJoined;

    public RoomMessageObserver() {
        mCurrentUsers = new ArrayList<>();
        mUsernameColorCache = new HashMap<>();
    }

    @Override
    public void observeForRoomId(String roomId) {
        if (roomId == null && observedRoomId() != null) {
            mRoomJoined = false;
            mCurrentUsers.clear();
            mUsernameColorCache.clear();
            onRoomDeInit();
        }
        super.observeForRoomId(roomId);
    }

    public boolean roomJoined() {
        return mRoomJoined;
    }

    public List<String> getUsers() {
        List<String> users = mCurrentUsers.size() > 5 ? new ArrayList<>() : new LinkedList<>();
        users.addAll(mCurrentUsers);
        return Collections.unmodifiableList(users);
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
            case "J":
            case "j":
            case "join":
                String username = message.nextArg();
                mCurrentUsers.add(username);
                onUpdateUsers(mCurrentUsers);
                if (!message.command.equals("J"))
                    printUserRelatedMessage(username + " joined");
                return true;
            case "L":
            case "l":
            case "leave":
                username = message.nextArg();
                mCurrentUsers.remove(username);
                onUpdateUsers(mCurrentUsers);
                if (!message.command.equals("L"))
                    printUserRelatedMessage(username + " left");
                return true;
            case "html":
                //printMessage("~html messages aren't supported yet~");
                return true;
            case "uhtml":
                //printMessage("~html messages aren't supported yet~");
                return true;
            case "uhtmlchange":
                // TODO
                return true;
            case "N":
            case "n":
            case "name":
                if (!message.command.equals("N"))
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
            case "B":
            case "b":
            case "battle":
                String roomId = message.nextArg();
                String user1 = message.nextArg();
                String user2 = message.nextArg();
                if (!message.command.equals("B"))
                    printMessage("A battle started between " + user1 + " and " + user2 + " (in room " + roomId + ")");
                return true;
            case "error":
                printErrorMessage(message.nextArg());
                return true;
            case "raw":
                String html = message.rawArgs();
                printHtml(prepareForHtml(html));
                return true;
            case "deinit":
                mRoomJoined = false;
                mCurrentUsers.clear();
                mUsernameColorCache.clear();
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
        String user = args.nextArg().trim();
        String userMessage = args.rawArgs();
        String html = null;
        boolean announce = false;
        if (userMessage.startsWith("/raw")) { // Todo: support more commands and enhance code.
            html = userMessage.substring(5);
            userMessage = "";
        } else if (userMessage.startsWith("/uhtml")) {
            html = userMessage.substring(userMessage.indexOf(',')+1);
            userMessage = "";
        } else if (userMessage.startsWith("/announce")) {
            userMessage = userMessage.substring(10);
            announce = true;
        }

        SpannableStringBuilder spannable = new SpannableStringBuilder(user + ": " + specChars(userMessage));
        int textColor = obtainUsernameColor(user);
        Object span = new TextTagSpan(Utils.getTagColor(textColor), textColor);
        spannable.setSpan(span, 0, user.length() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        applyStylingTags(spannable);
        if (announce) {
            spannable.setSpan(new BackgroundColorSpan(parseColor("#678CB1")),
                    user.length()+2, spannable.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(Color.WHITE),
                    user.length()+2, spannable.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        printMessage(spannable);
        if (html != null) printHtml(prepareForHtml(html));
    }

    private int obtainUsernameColor(String username) {
        Integer usernameColor = mUsernameColorCache.get(username);
        if (usernameColor == null) {
            if (User.getGroup(username) != 0) username = username.substring(1);
            usernameColor = Utils.hashColor(toId(username));
            mUsernameColorCache.put(username, usernameColor);
        }
        return usernameColor;
    }

    private void printUserRelatedMessage(String message) {
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

    protected void printHtml(String html) {
        onPrintHtml(html);
    }

    protected abstract void onPrintHtml(String html);

    protected abstract void onRoomInit();

    protected abstract void onPrintText(CharSequence text);

    protected abstract void onRoomTitleChanged(String title);

    protected abstract void onUpdateUsers(List<String> users);

    protected abstract void onRoomDeInit();
}
