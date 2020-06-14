package com.majeur.psclient.model

import java.io.Serializable

class ChatRoomInfo(val name: String, val description: String, val userCount: Int) : Comparable<ChatRoomInfo>, Serializable {

    override fun compareTo(other: ChatRoomInfo) = -userCount.compareTo(other.userCount)

}
