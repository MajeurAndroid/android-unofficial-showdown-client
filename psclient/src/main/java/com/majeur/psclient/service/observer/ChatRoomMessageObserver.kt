package com.majeur.psclient.service.observer

import com.majeur.psclient.service.ShowdownService

class ChatRoomMessageObserver(service: ShowdownService)
    : RoomMessageObserver<ChatRoomMessageObserver.UiCallbacks>(service) {

    override var uiCallbacks: UiCallbacks? = null

    interface UiCallbacks : RoomMessageObserver.UiCallbacks {
    }
}