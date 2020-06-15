package com.majeur.psclient.model.battle


enum class Player {

    FOE, TRAINER;

    fun username(username1: String, username2: String, myUsername: String): String {
        return if (username1 == myUsername) {
            if (this == TRAINER) username1 else username2
        } else if (username2 == myUsername) {
            if (this == TRAINER) username2 else username1
        } else {
            if (this == TRAINER) username1 else username2
        }
    }

    companion object {
        operator fun get(playerId: String, username1: String?, username2: String?, myUsername: String): Player {
            val isOne = playerId.take(2).contains("p1", ignoreCase = true)
            val watching = username1 != myUsername && username2 != myUsername
            if (watching) return if (isOne) TRAINER else FOE
            return if (isOne) if (username1 == myUsername) TRAINER else FOE else if (username2 == myUsername) TRAINER else FOE
        }
    }
}
