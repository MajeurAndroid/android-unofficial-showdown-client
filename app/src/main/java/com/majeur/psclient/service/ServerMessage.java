package com.majeur.psclient.service;

import android.text.TextUtils;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ServerMessage {

    private static final char SEPARATOR = '|';

    public String roomId;
    public String command;
    public List<String> args;
    private Iterator<String> mArgsIterator;
    public Map<String, String> kwargs;

    ServerMessage(String roomId, String data) {
        this.roomId = roomId;
        Log.w(getClass().getSimpleName(), "rommId: " + roomId + ", data: " + data);

        if (data.equals("|")) { // "|" type
            command = "break";
            args = Collections.emptyList();
            kwargs = Collections.emptyMap();
        } else if (data.charAt(0) != SEPARATOR || data.charAt(1) == SEPARATOR) { // "||MESSAGE" and "MESSAGE" type
            command = "raw";
            parseArguments(data, true);
        } else {
            int sepIndex = data.indexOf('|', 1);
            if (sepIndex == -1) {
                command = data.substring(1).toLowerCase();
                args = Collections.emptyList();
                kwargs = Collections.emptyMap();
            } else {
                command = data.substring(1, sepIndex).toLowerCase();
                parseArguments(data.substring(sepIndex + 1), command.equals("formats") || command.equals("c")
                        || command.equals("c:") || command.equals("tier") || command.equals("error"));
            }
        }
    }

    public boolean hasNextArg() {
        return mArgsIterator.hasNext();
    }

    public String nextArg() {
        return mArgsIterator.next();
    }

    public void resetArgsIteration() {
        mArgsIterator = args.iterator();
    }

    public String rawArgs() {
        if (args.size() < 1) return "";
        StringBuilder builder = new StringBuilder();
        while (hasNextArg()) builder.append(nextArg()).append(SEPARATOR);
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public String kwarg(String key) {
        return kwargs.get(key);
    }

    public boolean hasKwarg(String key) {
        return kwargs.containsKey(key);
    }

    private void parseArguments(String rawArgs, boolean escapeKwargs) {
        args = new LinkedList<>();
        kwargs = new HashMap<>();

        int sep = rawArgs.indexOf(SEPARATOR);
        if (sep == 0) rawArgs = rawArgs.substring(1);

        int separatorStart = 0;
        int separatorEnd = rawArgs.indexOf(SEPARATOR);
        boolean hasNext = true;
        while (hasNext) {
            String next;
            if (separatorEnd != -1) {
                next = rawArgs.substring(separatorStart, separatorEnd);
                separatorStart = separatorEnd + 1;
                separatorEnd = rawArgs.indexOf(SEPARATOR, separatorStart);
            } else {
                next = rawArgs.substring(separatorStart);
                hasNext = false;
            }

            if (TextUtils.isEmpty(next)) continue;

            if (!escapeKwargs && next.charAt(0) == '[' && next.contains("]")) {
                String key = next.substring(next.indexOf('[') + 1, next.indexOf(']'));
                String value = next.substring(key.length() + 2).trim();
                kwargs.put(key, value);
            } else {
                args.add(next.trim());
            }
        }
        mArgsIterator = args.iterator();
    }

    @Override
    public String toString() {
        return "ServerMessage{" +
                "roomId='" + roomId + '\'' +
                ", command='" + command + '\'' +
                ", args=" + args.size() +
                ", kwargs=" + kwargs.size() +
                '}';
    }
}
