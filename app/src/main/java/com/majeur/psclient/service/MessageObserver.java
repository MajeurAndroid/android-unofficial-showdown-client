package com.majeur.psclient.service;

import android.text.TextUtils;

public abstract class MessageObserver {

    private ShowdownService mService;
    private String mRoomId;
    private boolean mObserveAll;


    public void observeForRoomId(String roomId) {
        mRoomId = roomId;
    }

    public void setObserveAll(boolean observeAll) {
        mObserveAll = observeAll;
    }

    protected String observedRoomId() {
        return mRoomId;
    }

    protected ShowdownService getService() {
        return mService;
    }

    /* package */ void attachService(ShowdownService service) {
        mService = service;
    }

    /* package */ void detachService() {
        mService = null;
    }

    /* package */ boolean postMessage(ServerMessage message) {
        if (mObserveAll || TextUtils.equals(mRoomId, message.roomId))
            return onMessage(message);
        return false;
    }

    abstract boolean onMessage(ServerMessage message);

}
