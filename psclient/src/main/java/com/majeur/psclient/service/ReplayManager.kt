package com.majeur.psclient.service

import android.os.Handler
import android.os.Looper
import com.majeur.psclient.service.observer.BattleRoomMessageObserver
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException

class ReplayManager(private val showdownService: ShowdownService) {

    private lateinit var currentReplay : ReplayData
    private var uiHandler = Handler(Looper.getMainLooper())

    fun downloadAndStartReplay(replayId : String) {
        val url = HttpUrl.Builder().run {
            scheme("https")
            host("replay.pokemonshowdown.com")
            addPathSegment("$replayId.json")
            build()
        }

        val request = Request.Builder()
                .url(url)
                .build()

        showdownService.okHttpClient.newCall(request).enqueue(object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {

                val rawResponse = response.body()?.string()
                if (rawResponse?.isEmpty() != false) {
                    Timber.e("Replay download request responded with an empty body.")
                    notifyReplayDownloadFailure()
                    return
                }
                try {
                    currentReplay = ReplayData(JSONObject(rawResponse))
                    initReplayRoom()
                } catch (e: JSONException) {
                    Timber.e(e,"Error while parsing replay json.")
                    notifyReplayDownloadFailure()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e,"Replay download Call failed.")
                notifyReplayDownloadFailure()
            }
        })
    }

    private fun initReplayRoom() {
        showdownService.battleMessageObserver.onSetBattleType(BattleRoomMessageObserver.BattleType.REPLAY)

        var updateSearchMessage = MSG_FOUND.format(currentReplay.id, "Viewing Replay")
        processData(updateSearchMessage)

        var initBattleMessage = MSG_INIT_BATTLE.format(currentReplay.id)
        processData(initBattleMessage)

        sendAllLogLines()
    }

    private fun sendAllLogLines() {
        var battleLine = MSG_BATTLE_LINE_UPDATE.format(currentReplay.id, currentReplay.log)
        processData(battleLine)
    }

    private fun notifyReplayDownloadFailure() {
        processData(ERR_POPUP_REPLAY_DL_FAIL)
    }

    private fun processData(data: String) {
        uiHandler.post {
            showdownService.processServerData(data)
        }
    }


    fun goToNextTurn() {
        showdownService.battleMessageObserver.handleReplayAction(BattleRoomMessageObserver.ReplayAction.NEXT_TURN)
    }

    fun goToPreviousTurn() {
        TODO("Go to previous turn not yet implemented")
    }

    fun pause() {
        showdownService.battleMessageObserver.handleReplayAction(BattleRoomMessageObserver.ReplayAction.PAUSE)
    }

    fun play() {
        showdownService.battleMessageObserver.handleReplayAction(BattleRoomMessageObserver.ReplayAction.PLAY)
    }

    fun closeReplay() {
        showdownService.battleMessageObserver.handleReplayAction(BattleRoomMessageObserver.ReplayAction.CLOSE_REPLAY)
    }


    class ReplayData(replayData : JSONObject) {
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

    companion object {
        private var MSG_FOUND = """|updatesearch|{"searching":[],"games":{%s":"%s"}}" """
        private var MSG_INIT_BATTLE = ">%s \n|init|battle"
        private var MSG_BATTLE_LINE_UPDATE = ">%s \n%s"

        private var ERR_POPUP_REPLAY_DL_FAIL = "|popup|An error occurred when trying to retrieve the replay"
    }
}

