package com.majeur.psclient.service

import android.os.Handler
import android.os.Looper

class ActionQueue(looper: Looper) {

    private data class Entry (val action:()->Unit, val delay: Long, val isEndOfTurn: Boolean)

    private val handler = Handler(looper)
    private val actions = mutableListOf<Entry>()

    private var lastAction: (()->Unit)? = null
    private var isLooping = false
    private var turnActionInQueue = false

    var shouldLoopToLastTurn = true

    fun clear() {
        stopLoop()
        turnActionInQueue = false
        lastAction = null
        actions.clear()
    }

    fun setLastAction(action: (()->Unit)?) {
        lastAction = action
    }

    fun enqueueTurnAction(action: ()->Unit) {
        enqueue(action, 0, isEndOfTurn = true)

        // Only skip to the latest turn if we are watching a live battle.
        // Otherwise, do each turn in the queue one at a time
        if (turnActionInQueue && shouldLoopToLastTurn) {
            loopTo(action)
            turnActionInQueue = true
        }
    }

    fun enqueueAction(action: ()->Unit) {
        enqueue(action, 0, isEndOfTurn = false)
    }

    fun enqueueMajorAction(action: ()->Unit) {
        enqueue(action, 1500, isEndOfTurn = false)
    }

    fun enqueueMinorAction(action: ()->Unit) {
        enqueue(action, 750, isEndOfTurn = false)
    }

    private fun enqueue(action: ()->Unit, delay: Long, isEndOfTurn: Boolean) {
        actions.add(Entry(action, delay, isEndOfTurn))
        if (!isLooping) startLoop()
    }

    fun startLoop() {
        if (actions.isEmpty()) return

        isLooping = true
        handler.post(loopRunnable)
    }

    fun stopLoop() {
        isLooping = false
        handler.removeCallbacks(loopRunnable)
    }

    fun skipToNext() {
        do {
            if (actions.isEmpty()) {
                return
            }
            var thisAction = actions.first()

            actions.removeAt(0).action.invoke()
        } while (! thisAction.isEndOfTurn)
    }

    private fun loopTo(action: ()->Unit) {
        stopLoop()
        while (actions.first().action !== action)
            actions.removeAt(0).action.invoke()
        startLoop()
    }

    private val loopRunnable: Runnable = object : Runnable {

        override fun run() {
            val entry = actions.removeAt(0)
            entry.action.invoke()
            if (actions.any()) {
                handler.postDelayed(this, entry.delay)
            } else {
                stopLoop()
                turnActionInQueue = false
                lastAction?.invoke()
                lastAction = null
            }
        }
    }

}
