package com.majeur.psclient.service;

import android.util.Log;

import java.util.Iterator;
import java.util.NoSuchElementException;

import androidx.annotation.NonNull;

public class ServerMessage {

    private static final char SEPARATOR = '|';

    public String roomId;
    public String command;
    public Args args;

    ServerMessage(String roomId, String data) {
        this.roomId = roomId;
        Log.w(getClass().getSimpleName(), "rommId: " + roomId + ", data: " + data);

        if (data.equals("|")) { // "|" type
            command = "break";
            args = new Args("");
        } else if (data.charAt(0) != SEPARATOR || data.charAt(1) == SEPARATOR) { // "||MESSAGE" and "MESSAGE" type
            command = "raw";
            args = new Args(data);
        } else {
            int sepIndex = data.indexOf('|', 1);
            if (sepIndex == -1) {
                command = data.substring(1).toLowerCase();
                args = new Args("");
            } else {
                command = data.substring(1, sepIndex).toLowerCase();
                args = new Args(data.substring(sepIndex + 1));
            }
        }
    }

    @Override
    public String toString() {
        return "ServerMessage{" +
                "roomId='" + roomId + '\'' +
                ", command='" + command + '\'' +
                ", args=" + args.toString() +
                '}';
    }

    public static class Args implements Iterator<String> {

        private String mRawArgs;
        private int mSeparatorStart;
        private int mSeparatorEnd;

        private boolean mHasNext;

        Args(String rawArgs) {
            int sep = rawArgs.indexOf(SEPARATOR);
            if (sep == 0) rawArgs = rawArgs.substring(1);
            mRawArgs = rawArgs;

            mSeparatorStart = 0;
            mSeparatorEnd = rawArgs.indexOf(SEPARATOR);
            mHasNext = true;
        }

        public void reset() {
            mSeparatorStart = 0;
            mSeparatorEnd = mRawArgs.indexOf(SEPARATOR);
            mHasNext = true;
        }

        public int getIndex() {
            return mSeparatorStart;
        }

        public void moveTo(int index) {
            if (index < 0 || index >= mRawArgs.length())
                throw new IllegalArgumentException("index must be in [0;" + mRawArgs.length() + "[");
            if (index != 0 && mRawArgs.charAt(index) != SEPARATOR)
                throw new IllegalArgumentException("invalid index, use only values from getIndex()");
            mSeparatorStart = index;
            mSeparatorEnd = mRawArgs.indexOf(SEPARATOR, mSeparatorStart + 1);
            mHasNext = true;
        }

        @Override
        public boolean hasNext() {
            return mHasNext;
        }

        public String nextTillEnd() {
            if (!mHasNext)
                throw new NoSuchElementException();
            String next = mRawArgs.substring(mSeparatorStart);
            mHasNext = false;
            return next;
        }

        @Override
        public String next() {
            if (!mHasNext)
                throw new NoSuchElementException();
            String next;
            if (mSeparatorEnd != -1) {
                next = mRawArgs.substring(mSeparatorStart, mSeparatorEnd);
                mSeparatorStart = mSeparatorEnd + 1;
                mSeparatorEnd = mRawArgs.indexOf(SEPARATOR, mSeparatorStart);
            } else {
                next = mRawArgs.substring(mSeparatorStart);
                mHasNext = false;
            }
            return next;
        }

        @NonNull
        @Override
        public String toString() {
            return getClass().getSimpleName() + " {" + mRawArgs + "}";
        }
    }
}
