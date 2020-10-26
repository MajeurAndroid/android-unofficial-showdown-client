package com.majeur.psclient.service.observer

import com.majeur.psclient.model.BattleRoomInfo
import com.majeur.psclient.model.ChatRoomInfo
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.service.ServerMessage
import com.majeur.psclient.service.ShowdownService
import com.majeur.psclient.util.Utils
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.*

class GlobalMessageObserver(service: ShowdownService)
    : AbsMessageObserver<GlobalMessageObserver.UiCallbacks>(service) {

    override var observedRoomId: String? = "lobby"
    override val interceptCommandBefore = setOf("init", "noinit")
    override val interceptCommandAfter = setOf("deinit")

    val myUsername get() = service.getSharedData<String>("myusername")?.drop(1)
    var isUserGuest: Boolean = true
        private set

    private var requestServerCountsOnly = false
    private val privateMessages = mutableMapOf<String, MutableList<String>>()

    override fun onUiCallbacksAttached() {
        // If we did not stored at least username, we will not have anything else
        val username = service.getSharedData<String>("myusername") ?: return

        onUserChanged(username, isUserGuest, /* TODO */ "000")
        onUpdateCounts(service.getSharedData("users") ?: -1,
                service.getSharedData("battles") ?: -1)

        onBattleFormatsChanged(service.getSharedData("formats") ?: emptyList())

        onSearchBattlesChanged(service.getSharedData("searching") ?: emptyList(),
                service.getSharedData("games") ?: emptyMap())

        onChallengesChange(service.getSharedData("challenge_to"),
                service.getSharedData("challenge_to_format"),
                service.getSharedData("challenge_from") ?: emptyMap())
    }

    public override fun onMessage(message: ServerMessage) {
        message.newArgsIteration()
        when (message.command) {
            "connected" -> onConnectedToServer()
            "challstr" -> processChallengeString(message)
            "updateuser" -> processUpdateUser(message)
            "queryresponse" -> processQueryResponse(message)
            "formats" -> processAvailableFormats(message)
            "popup" -> handlePopup(message)
            "updatesearch" -> handleUpdateSearch(message)
            "pm" -> handlePm(message)
            "updatechallenges" -> handleChallenges(message)
            "networkerror" -> onNetworkError()
            "init" -> onRoomInit(message.roomId, message.nextArg)
            "deinit" -> onRoomDeinit(message.roomId)
            "noinit" -> {
                if (message.hasNextArg && "nonexistent" == message.nextArg && message.hasNextArg)
                    onShowPopup(message.nextArg)
            }
            "nametaken" -> {
                message.nextArg // Skipping name
                onShowPopup(message.nextArg)
            }
            "usercount" -> {
            }
        }
    }

    private fun processChallengeString(msg: ServerMessage) {
        service.putSharedData("challenge", msg.remainingArgsRaw)
        service.tryCookieSignIn()
    }

    private fun processUpdateUser(msg: ServerMessage) {
        var username = msg.nextArg
        service.putSharedData("myusername", username)
        val userType = username.substring(0, 1)
        username = username.substring(1)
        val isGuest = "0" == msg.nextArg
        var avatar = msg.nextArg
        avatar = "000$avatar".substring(avatar.length)
        isUserGuest = isGuest
        onUserChanged(username, isGuest, avatar)

        // Update server counts (active battle and active users)
        requestServerCountsOnly = true
        service.sendGlobalCommand("cmd", "rooms")

        // onSearchBattlesChanged(new String[0], new String[0], new String[0]); TODO Wtf was this call ?
    }

    private fun processQueryResponse(msg: ServerMessage) {
        val query = msg.nextArg
        val queryResponse = msg.remainingArgsRaw
        when (query) {
            "rooms" -> processRoomsQueryResponse(queryResponse)
            "roomlist" -> processRoomListQueryResponse(queryResponse)
            "savereplay" -> processSaveReplayQueryResponse(queryResponse)
            "userdetails" -> processUserDetailsQueryResponse(queryResponse)
            else -> Timber.w("Command |queryresponse| not handled, type=$query")
        }
    }

    private fun processRoomsQueryResponse(response: String) {
        if (response == "null") return
        try {
            val jsonObject = JSONObject(response)
            val userCount = jsonObject.getInt("userCount")
            service.putSharedData("users", userCount)
            val battleCount = jsonObject.getInt("battleCount")
            service.putSharedData("battles", battleCount)
            onUpdateCounts(userCount, battleCount)
            if (requestServerCountsOnly) {
                requestServerCountsOnly = false
                return
            }
            var jsonArray = jsonObject.getJSONArray("official")
            val officialRooms = mutableListOf<ChatRoomInfo>()
            for (i in 0 until jsonArray.length()) {
                val roomJson = jsonArray.getJSONObject(i)
                officialRooms.add(
                        ChatRoomInfo(roomJson.getString("title"),
                                roomJson.getString("desc"),
                                roomJson.getInt("userCount")))
            }
            jsonArray = jsonObject.getJSONArray("chat")
            val chatRooms = mutableListOf<ChatRoomInfo>()
            for (i in 0 until jsonArray.length()) {
                val roomJson = jsonArray.getJSONObject(i)
                chatRooms.add(
                        ChatRoomInfo(roomJson.getString("title"),
                                roomJson.getString("desc"),
                                roomJson.getInt("userCount")))
            }
            onAvailableRoomsChanged(officialRooms, chatRooms)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun processRoomListQueryResponse(response: String) {
        try {
            val battleRooms = mutableListOf<BattleRoomInfo>()
            val jsonObject = JSONObject(response).getJSONObject("rooms")
            val iterator = jsonObject.keys()
            while (iterator.hasNext()) {
                val roomId = iterator.next()
                val roomJson = jsonObject.getJSONObject(roomId)
                val roomInfo = BattleRoomInfo(roomId, roomJson.getString("p1"),
                        roomJson.getString("p2"), roomJson.optInt("minElo", 0))
                battleRooms.add(roomInfo)
            }
            onAvailableBattleRoomsChanged(battleRooms)
        } catch (e: JSONException) {
            e.printStackTrace()
            onAvailableBattleRoomsChanged(emptyList())
        }
    }

    private fun processSaveReplayQueryResponse(response: String) {
        try {
            val jsonObject = JSONObject(response)
            val replayId = jsonObject.optString("id")
            // val battleLog = jsonObject.optString("log")
            onReplaySaved(replayId, "https://replay.pokemonshowdown.com/$replayId")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun processUserDetailsQueryResponse(response: String) {
        try {
            val jsonObject = JSONObject(response)
            val userId = jsonObject.optString("userid")
            if (userId.isBlank()) return
            val name = jsonObject.optString("name")
            val online = jsonObject.has("status")
            val group = jsonObject.optString("group")
            val chatRooms = mutableListOf<String>()
            val battles = mutableListOf<String>()
            (jsonObject.opt("rooms") as? JSONObject)?.keys()?.forEach {
                if (it.startsWith("battle-") || it.drop(1).startsWith("battle-"))
                    battles.add(it)
                else
                    chatRooms.add(it)
            }
            onUserDetails(userId, name, online, group, chatRooms, battles)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun processAvailableFormats(msg: ServerMessage) {
        val rawText: String = msg.remainingArgsRaw
        val categories: MutableList<BattleFormat.Category> = LinkedList() // /!\ needs to impl Serializable

        rawText.split("|,").forEach { rawCategory ->
            val catName = rawCategory.substringAfter("|").substringBefore("|")
            val formats = rawCategory.substringAfter(catName).split("|")
                    .filter { s -> s.isNotBlank() }
                    .map { s ->
                        BattleFormat(s.substringBefore(","), s.substringAfter(",").toInt(16))
                    }
            BattleFormat.Category().apply {
                this.formats.addAll(formats)
                label = catName
            }.also {
                categories.add(it)
            }
        }
        service.putSharedData("formats", categories)
        onBattleFormatsChanged(categories)
    }

    private fun handlePopup(msg: ServerMessage) = onShowPopup(msg.args.joinToString("\n"))

    private fun handleUpdateSearch(msg: ServerMessage) {
        val jsonObject = Utils.jsonObject(msg.remainingArgsRaw) ?: return

        val searching = mutableListOf<String>()
        val searchingJson = jsonObject.optJSONArray("searching")
        searchingJson?.let {
            for (i in 0 until searchingJson.length())
                searching.add(searchingJson.getString(i))
        }

        val games = mutableMapOf<String, String>()
        val gamesJson = jsonObject.optJSONObject("games")
        gamesJson?.let {
            gamesJson.keys().forEach { key -> games[key] = gamesJson.getString(key) }
        }
        service.putSharedData("searching", searching)
        service.putSharedData("games", games)
        onSearchBattlesChanged(searching, games)
    }

    private fun handlePm(msg: ServerMessage) {
        val from = msg.nextArg.substring(1)
        val to = msg.nextArg.substring(1)
        val myUsername = service.getSharedData<String>("myusername")?.drop(1)
        val with = if (myUsername == from) to else from
        var content = msg.nextArgSafe
        if (content != null && (content.startsWith("/raw") || content.startsWith("/html") || content.startsWith("/uhtml")))
            content = "Html messages not supported in pm."
        val message = "$from: $content"
        val messages = privateMessages.getOrPut(with, { mutableListOf<String>() })
        messages.add(message)
        onNewPrivateMessage(with, message)
    }

    private fun handleChallenges(message: ServerMessage) {
        val rawJson: String = message.remainingArgsRaw
        var to: String? = null
        var format: String? = null
        val from = mutableMapOf<String, String>()

        val jsonObject = Utils.jsonObject(rawJson)
        val challengeTo = jsonObject.optJSONObject("challengeTo")
        challengeTo?.let {
            to = it.getString("to")
            format = it.getString("format")
        }
        val challengesFrom = jsonObject.optJSONObject("challengesFrom")
        challengesFrom?.let {
            it.keys().forEach { key ->
                from[key] = challengesFrom.getString(key)
            }
        }
        service.putSharedData("challenge_to", to)
        service.putSharedData("challenge_to_format", format)
        service.putSharedData("challenge_from", from)
        onChallengesChange(to, format, from)
    }

    fun getPrivateMessages(with: String): List<String>? {
        return privateMessages[with]
    }

    fun onConnectedToServer() = uiCallbacks?.onConnectedToServer()
    fun onUserChanged(userName: String, isGuest: Boolean, avatarId: String) = uiCallbacks?.onUserChanged(userName, isGuest, avatarId)
    fun onUpdateCounts(userCount: Int, battleCount: Int) = uiCallbacks?.onUpdateCounts(userCount, battleCount)
    fun onBattleFormatsChanged(battleFormats: List<BattleFormat.Category>) = uiCallbacks?.onBattleFormatsChanged(battleFormats)
    fun onSearchBattlesChanged(searching: List<String>, games: Map<String, String>) = uiCallbacks?.onSearchBattlesChanged(searching, games)
    fun onReplaySaved(replayId: String, url: String) = uiCallbacks?.onReplaySaved(replayId, url)
    fun onUserDetails(id: String, name: String, online: Boolean, group: String, rooms: List<String>, battles: List<String>) = uiCallbacks?.onUserDetails(id, name, online, group, rooms, battles)
    fun onShowPopup(message: String) = uiCallbacks?.onShowPopup(message)
    fun onAvailableRoomsChanged(officialRooms: List<ChatRoomInfo>, chatRooms: List<ChatRoomInfo>) = uiCallbacks?.onAvailableRoomsChanged(officialRooms, chatRooms)
    fun onAvailableBattleRoomsChanged(battleRooms: List<BattleRoomInfo>) = uiCallbacks?.onAvailableBattleRoomsChanged(battleRooms)
    fun onNewPrivateMessage(with: String, message: String) = uiCallbacks?.onNewPrivateMessage(with, message)
    fun onChallengesChange(to: String?, format: String?, from: Map<String, String>) = uiCallbacks?.onChallengesChange(to, format, from)
    fun onRoomInit(roomId: String, type: String) = uiCallbacks?.onRoomInit(roomId, type)
    fun onRoomDeinit(roomId: String) = uiCallbacks?.onRoomDeinit(roomId)
    fun onNetworkError() = uiCallbacks?.onNetworkError()

    interface UiCallbacks : AbsMessageObserver.UiCallbacks {
        fun onConnectedToServer()
        fun onUserChanged(userName: String, isGuest: Boolean, avatarId: String)
        fun onUpdateCounts(userCount: Int, battleCount: Int)
        fun onBattleFormatsChanged(battleFormats: List<@JvmSuppressWildcards BattleFormat.Category>)
        fun onSearchBattlesChanged(searching: List<String>, games: Map<String, String>)
        fun onReplaySaved(replayId: String, url: String)
        fun onUserDetails(id: String, name: String, online: Boolean, group: String, rooms: List<String>, battles: List<String>)
        fun onShowPopup(message: String)
        fun onAvailableRoomsChanged(officialRooms: List<ChatRoomInfo>, chatRooms: List<ChatRoomInfo>)
        fun onAvailableBattleRoomsChanged(battleRooms: List<BattleRoomInfo>)
        fun onNewPrivateMessage(with: String, message: String)
        fun onChallengesChange(to: String?, format: String?, from: Map<String, String>)
        fun onRoomInit(roomId: String, type: String)
        fun onRoomDeinit(roomId: String)
        fun onNetworkError()
    }
}

