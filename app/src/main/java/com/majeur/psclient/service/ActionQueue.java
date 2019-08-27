package com.majeur.psclient.service;

import android.os.Handler;
import android.os.Looper;

import java.util.LinkedList;
import java.util.Queue;


public class ActionQueue {

    public static final Runnable EMPTY_ACTION = new Runnable() {
        @Override
        public void run() {
        }
    };

    private static class Entry {

        Entry(Runnable action, long delay) {
            this.action = action;
            this.delay = delay;
        }

        private Runnable action;
        private long delay;
    }

    private Handler mHandler;

    private Queue<Entry> mQueue;
    private Runnable mLastAction;
    private boolean mIsLooping;

    private boolean mTurnActionInQueue;

    public ActionQueue(Looper looper) {
        mHandler = new Handler(looper);
        mQueue = new LinkedList<>();
        mIsLooping = false;
    }

    public void clear() {
        stopLoop();
        mTurnActionInQueue = false;
        mLastAction = null;
        mQueue.clear();
    }

    public void setLastAction(Runnable action) {
        mLastAction = action;
    }

    public void enqueueTurnAction(Runnable action) {
        enqueueAction(action);
        if (mTurnActionInQueue)
            loopTo(action);

        mTurnActionInQueue = true;
    }

    public void enqueueAction(Runnable action) {
        enqueue(action, 0);
    }

    public void enqueueMajorAction(Runnable action) {
        enqueue(action, 1500);
    }

    public void enqueueMinorAction(Runnable action) {
        enqueue(action, 750);
    }

    private void enqueue(Runnable action, long delay) {
        mQueue.add(new Entry(action, delay));

        if (!mIsLooping)
            startLoop();
    }

    private void startLoop() {
        mIsLooping = true;
        mHandler.post(mLoopRunnable);
    }

    private void stopLoop() {
        mIsLooping = false;
        mHandler.removeCallbacks(mLoopRunnable);
    }

    private void loopTo(Runnable action) {
        stopLoop();
        while (mQueue.element().action != action)
            mQueue.poll().action.run();
        startLoop();
    }

    private final Runnable mLoopRunnable = new Runnable() {
        @Override
        public void run() {
            Entry entry = mQueue.poll();
            entry.action.run();

            if (!mQueue.isEmpty()) {
                mHandler.postDelayed(this, entry.delay);
            } else {
                mTurnActionInQueue = false;
                mIsLooping = false;
                if (mLastAction != null) {
                    mLastAction.run();
                    mLastAction = null;
                }
            }
        }
    };
}
