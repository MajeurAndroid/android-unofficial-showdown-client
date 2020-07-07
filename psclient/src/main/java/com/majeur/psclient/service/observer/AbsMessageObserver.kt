package com.majeur.psclient.service.observer

import com.majeur.psclient.service.ServerMessage
import com.majeur.psclient.service.ShowdownService

abstract class AbsMessageObserver<C : AbsMessageObserver.UiCallbacks>(
        val service: ShowdownService
) {

    open var uiCallbacks: C? = null

    open var observedRoomId: String? = null

    var interceptCommandBefore = setOf<String>()
        protected set

    var interceptCommandAfter = setOf<String>()
        protected set

    fun postMessage(message: ServerMessage, forcePost: Boolean = false) {
        if (forcePost || observedRoomId == message.roomId) onMessage(message)
    }

    protected abstract fun onMessage(message: ServerMessage)

    interface UiCallbacks {

    }
}