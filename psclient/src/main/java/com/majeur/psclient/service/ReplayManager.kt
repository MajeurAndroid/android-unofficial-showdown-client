package com.majeur.psclient.service

import com.majeur.psclient.service.observer.AbsMessageObserver
import com.majeur.psclient.service.observer.BattleRoomMessageObserver
import com.majeur.psclient.ui.BaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ReplayManager(
        val showdownService: ShowdownService,
//        val replayCallbackConsumer: ReplayCallbackConsumer) {
        val replayCallback: ReplayCallback) {
//    val messageObservers: List<AbsMessageObserver<out AbsMessageObserver.UiCallbacks>>,
//        val okHttpClient: OkHttpClient) {

    private val fragmentScope = BaseFragment.FragmentScope()

//    private var battleMessageObserver;
    private lateinit var currentReplay : ReplayData


    fun downloadAndStartReplay(uri : String) {
        downloadJsonLogAndTriggerJoin(uri)
//        currentReplay = ReplayData(replayJson)


//        initReplayRoom();
    }

    private fun downloadJsonLogAndTriggerJoin(uri: String) {
//        val url = HttpUrl.Builder()
//                .scheme("https")
//                .host("pastebin.com")
//                .addPathSegment("raw")
//                .addPathSegment(rawPastebinKey)
//                .build()
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
//        var updateSearchMessage = FOUND.format(replayData.id, "REPLAY - " + replayData.format)
        var updateSearchMessage = FOUND.format(replayData.id, "Viewing Replay")
        Timber.tag("Replay [Update Search]").i(updateSearchMessage)
        replayCallback.onMessage(updateSearchMessage)


//        var initBattleMessage = INIT_BATTLE.format(replayData.id, replayData.p1, replayData.p2)
        var initBattleMessage = INIT_BATTLE.format(replayData.id)
        Timber.tag("Replay [Init]").i(initBattleMessage)
        replayCallback.onMessage(initBattleMessage)


        sendAllLogLines()


        // Temporary - TODO update to just print until team preview
//        (0..20).forEach { _ -> printNextLine() }
//        Thread(Runnable() {
//            replayData.logLines.forEach { _ ->
//                TimeUnit.MILLISECONDS.sleep(600)
//                fragmentScope.launch {
//                    printNextLine()
//                }
//            }
//
//        }).start()

    }

    private fun sendAllLogLines() {
        var battleLine = BATTLE_LINE_UPDATE.format(currentReplay.id, currentReplay.log)
        Timber.tag("Replay [DATA]").i(battleLine)
        replayCallback.onMessage(battleLine)
    }

//    private fun printNextLine() {
//        var line = currentReplay.logLines.get(currentReplay.currentLine)
//        var battleLine = BATTLE_LINE_UPDATE.format(currentReplay.id, line)
//        Timber.tag("Replay [DATA]").i(battleLine)
//        replayCallback.onMessage(battleLine)
//
//
//        currentReplay.currentLine++
//    }

    fun goToNextTurn() {
        // TODO
//        printNextLine()
    }

    fun goToPreviousTurn() {

    }

    fun pause() {

    }

    fun play() {

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

        // ----------------------------
        // Parsed data, for later use
        val logLines: List<String> = log.split("\n")

        var currentLine = 0

        var preTurnLogs = ArrayList<String>()
        var logsByTurn = HashMap<Integer, List<String>>()



//        fun linesUntilTeamPreview() :


        // Parse log data into useful categories for later processing
//        init {
//
//            var sectionToScan = 0
//            var turnNum = 0
//
//            logLines.forEach { line ->
//                if (sectionToScan == SECTION_INIT) {
//
//                    preTurnLogs.add(line)
//                    if (line.startsWith("|teampreview")) sectionToScan = SECTION_PRE_TURN
//
//                } else if (sectionToScan == SECTION_PREVIEW) {
//                    preTurnLogs.add(line)
//                    if (line.startsWith("|teampreview")) sectionToScan = SECTION_PRE_TURN
//
//                } else if (sectionToScan == SECTION_PRE_TURN) {
//                        preTurnLogs.add(line)
//
//                        if (line.startsWith("|teampreview")) sectionToScan++
//                } else {
//                    // Split turns up
//
//
//                }
//
//
//            }
//        }

        companion object {
            var SECTION_INIT = 0
            var SECTION_PREVIEW = 0
            var SECTION_PRE_TURN = 1
            var SECTION_GAME_START = 2
            var SECTION_TURNS = 2

            var LINE_TEAM_PREVIEW = "|teampreview"
            var LINE_GAME_START = "|start"
            var LINE_TURNS = "|turn"
        }
    }



    interface ReplayCallback {
        fun init()
        fun onMessage(msg: String)
        fun onAction(replayAction: BattleRoomMessageObserver.ReplayAction)
    }

    companion object {
        var FOUND = """|updatesearch|{"searching":[],"games":{%s":"%s"}}" """
//        var INIT_BATTLE = ">%s init|battle \n|title|%s \n|j|☆%s \n|j|☆%s"
//        var INIT_BATTLE = ">%s \n|init|battle \n|title|%s vs. %s"
        var INIT_BATTLE = ">%s \n|init|battle"
        var TITLE = ">%s \n|title|%s \n|j|☆%s \n|j|☆%s"
        var BATTLE_LINE_UPDATE = ">%s \n%s"
    }
}

class AutomaticTimedMessageSender(
        private val replayData: ReplayManager.ReplayData,
        private val replayCallback: ReplayManager.ReplayCallback) {

    fun printNextLine() {
        var line = replayData.logLines.get(replayData.currentLine)
        var battleLine = ReplayManager.BATTLE_LINE_UPDATE.format(replayData.id, line)
        Timber.tag("Replay [DATA]").i(battleLine)
        replayCallback.onMessage(battleLine)

        replayData.currentLine++
    }

}
