package com.majeur.psclient.service

import com.majeur.psclient.service.observer.BattleRoomMessageObserver
import com.majeur.psclient.ui.BaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

class ReplayManager(
        val showdownService: ShowdownService,
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
                val response = showdownService.okHttpClient.newCall(request).execute()
                response.body()?.string() ?: ""
            }
            print("Replay Data raw $replayResponse")

            currentReplay = ReplayData(JSONObject(replayResponse))
            initReplayRoom(currentReplay)
        }
    }

    private fun initReplayRoom(replayData: ReplayData) {
        replayCallback.init()

//        var updateSearchMessage = FOUND.format(replayData.id, "REPLAY - " + replayData.format)
        var updateSearchMessage = FOUND.format(replayData.id, "Viewing Replay")
        Timber.tag("Replay [Update Search]").i(updateSearchMessage)
        replayCallback.onMessage(updateSearchMessage)

        var initBattleMessage = INIT_BATTLE.format(replayData.id)
        Timber.tag("Replay [Init]").i(initBattleMessage)
        replayCallback.onMessage(initBattleMessage)

        sendAllLogLines()
    }

    private fun sendAllLogLines() {
        var battleLine = BATTLE_LINE_UPDATE.format(currentReplay.id, currentReplay.log)
        Timber.tag("Replay [DATA]").i(battleLine)
        replayCallback.onMessage(battleLine)
    }

    fun goToNextTurn() {
        replayCallback.onAction(BattleRoomMessageObserver.ReplayAction.NEXT_TURN)
    }

    fun goToPreviousTurn() {
        TODO("Go to next turn not yet implemented")
    }

    fun pause() {
        replayCallback.onAction(BattleRoomMessageObserver.ReplayAction.PAUSE)
    }

    fun play() {
        replayCallback.onAction(BattleRoomMessageObserver.ReplayAction.PLAY)
    }

    class ReplayData(replayData : JSONObject) {

        // ----------------------------
        // Raw data from replay JSON
        val id: String = replayData.getString("id")
        val p1: String = replayData.getString("p1")
        val p2: String = replayData.getString("p2")
        val format: String = replayData.getString("format")
        val log: String = replayData.getString("log")
//        val inputLog: String = replayData.getString("inputlog")
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
        var FOUND = """|updatesearch|{"searching":[],"games":{%s":"%s"}}" """
        var INIT_BATTLE = ">%s \n|init|battle"
        var TITLE = ">%s \n|title|%s \n|j|☆%s \n|j|☆%s"
        var BATTLE_LINE_UPDATE = ">%s \n%s"
    }
}

