package com.majeur.psclient.service.observer

import com.majeur.psclient.service.ServerMessage
import com.majeur.psclient.service.ShowdownService

abstract class AbsMessageObserver<C : AbsMessageObserver.UiCallbacks>(
        val service: ShowdownService
) {

    var uiCallbacks: C? = null
        set(value) {
            field = value
            if (value != null) onUiCallbacksAttached()
        }

    protected abstract fun onUiCallbacksAttached()

    open var observedRoomId: String? = null

    open val interceptCommandBefore = emptySet<String>()

    open val interceptCommandAfter = emptySet<String>()

    fun postMessage(message: ServerMessage, forcePost: Boolean = false) {
        if (forcePost || observedRoomId == message.roomId) onMessage(message)
    }

    protected abstract fun onMessage(message: ServerMessage)

    interface UiCallbacks {

    }
}