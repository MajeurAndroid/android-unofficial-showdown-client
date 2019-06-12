package com.majeur.psclient.service;

import android.text.TextUtils;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;

public abstract class ShowdownMessageHandler {

    static final Comparator<ShowdownMessageHandler> COMPARATOR =
            Collections.reverseOrder(new Comparator<ShowdownMessageHandler>() {
                @Override
                public int compare(ShowdownMessageHandler s1, ShowdownMessageHandler s2) {
                    return Integer.compare(s1.getPriority(), s2.getPriority());
                }
            });

    private ShowdownService mShowdownService;

    public void release() {
        if (getShowdownService() != null)
            getShowdownService().unregisterMessageHandler(this);
    }

    /* package */ void attachService(ShowdownService showdownService) {
        mShowdownService = showdownService;
    }

    /* package */ void detachService() {
        mShowdownService = null;
    }

    protected ShowdownService getShowdownService() {
        return mShowdownService;
    }

    /* package */ void postMessages(String messages) {
        int separatorStart = 0;
        int separatorEnd = messages.indexOf('\n');
        if (separatorEnd == -1) {
            readMessage(messages);
            return;
        }

        while (true) {
            readMessage(messages.substring(separatorStart, separatorEnd));

            separatorStart = separatorEnd+1;
            separatorEnd = messages.indexOf('\n', separatorStart);

            if (separatorEnd == -1) {
                readMessage(messages.substring(separatorStart));
                break;
            }
        }
    }

    private void readMessage(String message) {
        if (message.length() == 0)
            return;

        switch (message.charAt(0)) {
            case '>':
                onHandleHeader(message);
                break;
            case '|':
                if (message.length() == 1)
                    message = "break|";
                else if (message.charAt(1) == '|')
                    message = "raw|" + message.substring(2);
                onHandleMessage(new MessageIterator(message));
        }
    }

    protected int getPriority() {
        return 0;
    }

    protected void onHandleHeader(String header) {

    }

    /* package */ abstract boolean shouldHandleMessages(String messages);

    protected abstract void onHandleMessage(MessageIterator message);

    /* package */ protected void onNetworkError() {

    }

}
