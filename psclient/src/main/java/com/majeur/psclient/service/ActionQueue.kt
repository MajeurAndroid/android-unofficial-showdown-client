package com.majeur.psclient.service

import android.os.Handler
import android.os.Looper
import com.majeur.psclient.service.observer.BattleRoomMessageObserver

class ActionQueue(looper: Looper, var listener: QueueListener) {

    private data class Entry (val action: Action, val delay: Long)

    private val handler = Handler(looper)
    private val actions = mutableListOf<Entry>()

    private var lastAction: (()->Unit)? = null
    private var isLooping = false
    private var turnActionInQueue = false

    private var battleType = BattleRoomMessageObserver.BattleType.LIVE

    fun setBattleType(battleType: BattleRoomMessageObserver.BattleType) {
        this.battleType = battleType
    }

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
        enqueueAction(Action(action, ActionType.START_OF_TURN))

        // Only skip to the latest turn if we are watching a live battle.
        // Otherwise, do each turn in the queue one at a time
        if (turnActionInQueue && battleType == BattleRoomMessageObserver.BattleType.LIVE) {
            loopTo(action)
            turnActionInQueue = true
        }
    }

    fun enqueueAction(action: ()->Unit) {
        enqueue(Action(action), 0)
    }

    fun enqueueMajorAction(action: ()->Unit) {
        enqueue(Action(action), 1500)
    }

    fun enqueueMinorAction(action: ()->Unit) {
        enqueue(Action(action), 750)
    }

    private fun enqueueAction(action: Action) {
        enqueue(action, 0)
    }

    private fun enqueue(action: Action, delay: Long) {
        actions.add(Entry(action, delay))
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
                listener.onQueueEmpty()
                return
            }
            var thisAction = actions.first().action

            actions.removeAt(0).action.actionUnit.invoke()
        } while (thisAction.actionType !== ActionType.START_OF_TURN)
    }

    private fun loopTo(action: ()->Unit) {
        stopLoop()
        while (actions.first().action.actionUnit !== action)
            actions.removeAt(0).action.actionUnit.invoke()
        startLoop()
    }

    private val loopRunnable: Runnable = object : Runnable {

        override fun run() {
            val entry = actions.removeAt(0)
            entry.action.actionUnit.invoke()
            if (actions.any()) {
                handler.postDelayed(this, entry.delay)
            } else {
                stopLoop()
                turnActionInQueue = false
                lastAction?.invoke()
                lastAction = null
                listener.onQueueEmpty()
            }
        }
    }

}

interface QueueListener {
    fun onQueueEmpty()
}

enum class ActionType {
    START_OF_TURN,
    OTHER
}

class Action(_action: ()->Unit, _type: ActionType = ActionType.OTHER) {
    val actionUnit: ()->Unit = _action
    val actionType: ActionType = _type
}