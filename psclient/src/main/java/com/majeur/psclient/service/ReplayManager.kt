package com.majeur.psclient.service

import com.majeur.psclient.service.observer.BattleRoomMessageObserver
import com.majeur.psclient.ui.BaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ReplayManager(
        val httpClient: OkHttpClient,
        val replayCallback: ReplayCallback) {

    private val fragmentScope = BaseFragment.FragmentScope()
    private lateinit var currentReplay : ReplayData


    fun downloadAndStartReplay(uri : String) {
        val url = HttpUrl.get(uri)
        fragmentScope.launch {
            val request = Request.Builder()
                    .url(url)
                    .build()
            val replayResponse = withContext(Dispatchers.IO) {
                val response = httpClient.newCall(request).execute()
                response.body()?.string() ?: ""
            }

            currentReplay = ReplayData(JSONObject(replayResponse))

            initReplayRoom(currentReplay)
        }
    }

    private fun initReplayRoom(replayData: ReplayData) {
        replayCallback.init()

        var updateSearchMessage = MSG_FOUND.format(replayData.id, "Viewing Replay")
        replayCallback.onMessage(updateSearchMessage)

        var initBattleMessage = MSG_INIT_BATTLE.format(replayData.id)
        replayCallback.onMessage(initBattleMessage)

        sendAllLogLines()
    }

    private fun sendAllLogLines() {
        var battleLine = MSG_BATTLE_LINE_UPDATE.format(currentReplay.id, currentReplay.log)
        replayCallback.onMessage(battleLine)
    }

    fun goToNextTurn() {
        replayCallback.onAction(BattleRoomMessageObserver.ReplayAction.NEXT_TURN)
    }

    fun goToPreviousTurn() {
        TODO("Go to previous turn not yet implemented")
    }

    fun pause() {
        replayCallback.onAction(BattleRoomMessageObserver.ReplayAction.PAUSE)
    }

    fun play() {
        replayCallback.onAction(BattleRoomMessageObserver.ReplayAction.PLAY)
    }

    fun closeReplay() {
        replayCallback.onAction(BattleRoomMessageObserver.ReplayAction.CLOSE_REPLAY)
    }

    class ReplayData(replayData : JSONObject) {

        // ----------------------------
        // Raw data from replay JSON
        val id: String = replayData.getString("id")
        val p1: String = replayData.getString("p1")
        val p2: String = replayData.getString("p2")
        val format: String = replayData.getString("format")
        val log: String = replayData.getString("log")
        //  val inputLog: String = replayData.getString("inputlog")
        val uploadTime: Long = replayData.getLong("uploadtime")
        val views: Long = replayData.getLong("views")
        val p1Id: String = replayData.getString("p1id")
        val p2Id: String = replayData.getString("p2id")
        val formatId: String = replayData.getString("formatid")
        // rating
        // private
        // password
    }



    interface ReplayCallback {
        fun init()
        fun onMessage(msg: String)
        fun onAction(replayAction: BattleRoomMessageObserver.ReplayAction)
    }

    companion object {
        private var MSG_FOUND = """|updatesearch|{"searching":[],"games":{%s":"%s"}}" """
        private var MSG_INIT_BATTLE = ">%s \n|init|battle"
        private var TITLE = ">%s \n|title|%s \n|j|☆%s \n|j|☆%s"
        private var MSG_BATTLE_LINE_UPDATE = ">%s \n%s"
    }
}

