package com.majeur.psclient.service;

import java.util.Iterator;
import java.util.NoSuchElementException;

import androidx.annotation.NonNull;

public class MessageIterator implements Iterator<String> {

    private static final char SEPARATOR = '|';

    private String mMessage;
    private int mSeparatorStart;
    private int mSeparatorEnd;

    private boolean hasNext;

    public MessageIterator(String message) {
        int sep = message.indexOf(SEPARATOR);
        if (sep == 0) message = message.substring(1);
        mMessage = message;

        mSeparatorStart = 0;
        mSeparatorEnd = message.indexOf(SEPARATOR);
        hasNext = true;
    }

    public int getIndex() {
        return mSeparatorStart;
    }

    public void moveTo(int index) {
        if (index < 0 || index >= mMessage.length())
            throw new IllegalArgumentException("index must be in [0;" + mMessage.length() + "[");
        mSeparatorStart = index;
        mSeparatorEnd = mMessage.indexOf(SEPARATOR, mSeparatorStart + 1);
        hasNext = true;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    public String nextTillEnd() {
        if (!hasNext())
            throw new NoSuchElementException();
        String next = mMessage.substring(mSeparatorStart);
        hasNext = false;
        return next;
    }

    @Override
    public String next() {
        if (!hasNext())
            throw new NoSuchElementException();
        String next;
        if (mSeparatorEnd != -1) {
            next = mMessage.substring(mSeparatorStart, mSeparatorEnd);
            mSeparatorStart = mSeparatorEnd + 1;
            mSeparatorEnd = mMessage.indexOf(SEPARATOR, mSeparatorStart);
        } else {
            next = mMessage.substring(mSeparatorStart);
            hasNext = false;
        }
        return next;
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + " {" + mMessage + "}";
    }
}
