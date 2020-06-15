package com.majeur.psclient.service

abstract class AbsMessageObserver {

    open var observedRoomId: String? = null

    var interceptCommandBefore = setOf<String>()
        protected set

    var interceptCommandAfter = setOf<String>()
        protected set

    var service: ShowdownService? = null

    fun postMessage(message: ServerMessage, forcePost: Boolean = false) {
        if (forcePost || observedRoomId == message.roomId) onMessage(message)
    }

    protected abstract fun onMessage(message: ServerMessage)
}