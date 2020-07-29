package com.majeur.psclient.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.majeur.psclient.service.observer.BattleRoomMessageObserver
import com.majeur.psclient.service.observer.ChatRoomMessageObserver
import com.majeur.psclient.service.observer.GlobalMessageObserver
import com.majeur.psclient.util.S
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class ShowdownService : Service() {

    companion object {
        private const val WS_CLOSE_NORMAL = 1000
        private const val WS_CLOSE_GOING_AWAY = 1001
        private const val WS_CLOSE_NETWORK_ERROR = 4001
        private const val SHOWDOWN_SOCKET_URL = "wss://sim3.psim.us/showdown/websocket"
    }

    lateinit var okHttpClient: OkHttpClient
        private set

    private lateinit var binder: Binder
    private lateinit var uiHandler: Handler

    val globalMessageObserver by lazy { GlobalMessageObserver(this) }
    val chatMessageObserver by lazy { ChatRoomMessageObserver(this) }
    val battleMessageObserver by lazy { BattleRoomMessageObserver(this) }
    private val messageObservers get() = listOf(globalMessageObserver, chatMessageObserver, battleMessageObserver)
    private var previousChatRoomId: String? = null
    private var previousBattleRoomId: String? = null

    val replayManager by lazy {
        ReplayManager(okHttpClient, object : ReplayManager.ReplayCallback {
            override fun init() {
                battleMessageObserver.onSetBattleType(BattleRoomMessageObserver.BattleType.REPLAY)
            }
            override fun onMessage(msg: String) {
                processServerData(msg)
            }

            override fun onAction(replayAction: BattleRoomMessageObserver.ReplayAction) {
                battleMessageObserver.handleReplayAction(replayAction)
            }
        })
    }

    private val sharedData = mutableMapOf<String, Any?>()
    private var webSocket: WebSocket? = null
    private var _connected = AtomicBoolean(false)

    @Suppress("ObjectLiteralToLambda")
    private val stopSelfRunnable = object : Runnable {
        override fun run() = stopSelf()
    }

    var isConnected: Boolean
        private set(value) = _connected.set(value)
        get() = _connected.get()

    override fun onCreate() {
        Timber.d("(${hashCode()}) Lifecycle: onCreate")
        super.onCreate()
        uiHandler = Handler(Looper.getMainLooper())
        binder = Binder()
        okHttpClient = OkHttpClient.Builder()
                .build()
    }

    override fun onBind(intent: Intent): Binder {
        Timber.d("(${hashCode()}) Lifecycle: onBind")
        uiHandler.removeCallbacks(stopSelfRunnable)
        return binder
    }

    override fun onRebind(intent: Intent?) {
        Timber.d("(${hashCode()}) Lifecycle: onRebind")
        super.onRebind(intent)
        // We try to rejoin previously leaved rooms
        if (previousBattleRoomId != null)
            sendGlobalCommand("join", previousBattleRoomId!!)
        if (previousChatRoomId != null)
            sendGlobalCommand("join", previousChatRoomId!!)
        uiHandler.removeCallbacks(stopSelfRunnable)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Timber.d("(${hashCode()}) Lifecycle: onUnbind")
        // If no activity is bound we leave every room we were into and keep their ids to rejoin them on next bind
        previousBattleRoomId = battleMessageObserver.observedRoomId
        if (previousBattleRoomId != null) sendRoomCommand(previousBattleRoomId, "leave")
        previousChatRoomId = chatMessageObserver.observedRoomId
        if (previousChatRoomId != null) sendRoomCommand(previousChatRoomId, "leave")
        // We stop our service (and close our WS connection) after 30 seconds with no activity bound
        uiHandler.postDelayed(stopSelfRunnable, 30000)
        return true
    }

    override fun onDestroy() {
        Timber.d("(${hashCode()}) Lifecycle: onDestroy")
        super.onDestroy()
        if (isConnected) webSocket?.close(WS_CLOSE_GOING_AWAY, null)
    }

    fun connectToServer() {
        if (isConnected) return
        Timber.d("Attempting to open WS connection.")
        val request = Request.Builder().url(SHOWDOWN_SOCKET_URL).build()
        webSocket = okHttpClient.newWebSocket(request, webSocketListener)
    }

    fun reconnectToServer() {
        if (isConnected) return
        connectToServer()
    }

    fun disconnectFromServer() {
        if (!isConnected) return
        Timber.d("Attempting to close WS connection.")
        webSocket?.close(WS_CLOSE_NORMAL, "Normal closure")
        sharedData.clear()
    }

    fun sendTrnMessage(userName: String, assertion: String) = sendGlobalCommand("trn", userName, "0", assertion)

    fun sendPrivateMessage(to: String, content: String) = sendGlobalCommand("pm", to, content)

    fun sendGlobalCommand(command: String, vararg args: Any) =
            sendRoomMessage(null, "/$command ${args.joinToString(",")}")

    fun sendRoomCommand(roomId: String?, command: String, vararg args: Any?) =
            sendRoomMessage(roomId, "/$command ${args.joinToString("|")}")

    fun sendRoomMessage(roomId: String?, message: String) = sendMessage("${roomId ?: ""}|$message")

    private fun sendMessage(message: String) {
        if (isConnected) {
            Timber.tag("WebSocket[SEND]").i(message)
            webSocket?.send(message)
        } else {
            Timber.w("WebSocket not opened. Ignoring message: $message")
        }
    }

    private fun processServerData(data: String) {
        if (data[0] == '>') dispatchServerData(data.removePrefix(">").substringBefore("\n"),
                data.substringAfter("\n"))
        else dispatchServerData(null, data)
    }

    private fun dispatchServerData(roomId: String?, data: String) {
        data.split("\n")
                .filter { it.isNotBlank() }
                .forEach { dispatchMessage(ServerMessage(roomId ?: "lobby", it)) }
    }

    private fun dispatchMessage(msg: ServerMessage) {
        val observers = messageObservers
        val observersInterceptingBefore = observers
                .filter { it.interceptCommandBefore.contains(msg.command) }
        val observersInterceptingAfter = observers
                .filter { it.interceptCommandAfter.contains(msg.command) }

        observersInterceptingBefore
                .forEach { it.postMessage(msg, forcePost = true) }
        observers
                .minus(observersInterceptingBefore)
                .forEach { it.postMessage(msg) }
        observersInterceptingAfter
                .minus(observersInterceptingBefore)
                .forEach { it.postMessage(msg, forcePost = true) }
    }

    fun fakeBattle() {
        S.run = true
        processServerData(S.s)
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Timber.tag("WebSocket[OPEN]").i("Host: ${response.request().url().host()}")
            isConnected = true
            uiHandler.post {
                dispatchMessage(ServerMessage("lobby", "|connected|"))
            }
        }

        override fun onMessage(webSocket: WebSocket, data: String) {
            Timber.tag("WebSocket[RECEIVE]").i(data)
            uiHandler.post {
                processServerData(data)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Timber.tag("WebSocket[CLOSING]").i(reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Timber.tag("WebSocket[ERR]").w(t)
            isConnected = false
            this@ShowdownService.webSocket = null
            uiHandler.post {
                dispatchMessage(ServerMessage("lobby", "|networkerror|"))
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Timber.tag("WebSocket[CLOSED]").i(reason)
            isConnected = false
            this@ShowdownService.webSocket = null
        }
    }

    fun tryCookieSignIn() {
        retrieveAuthCookieIfAny()?.let { cookie ->
            val url = actionServerUrlWithChallenge
                    .addQueryParameter("act", "upkeep")
                    .build()
            val request = Request.Builder()
                    .url(url)
                    .addHeader("cookie", cookie)
                    .build()
            okHttpClient.newCall(request).enqueue(object : Callback {
                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val rawResponse = response.body()?.string()
                    if (rawResponse?.isEmpty() != false) {
                        Timber.e("Assertion request responded with an empty body.")
                        return
                    }
                    try {
                        val resultJson = JSONObject(rawResponse.removePrefix("]"))
                        if (resultJson.optBoolean("loggedin"))
                            sendTrnMessage(resultJson.getString("username"),
                                    resultJson.getString("assertion"))
                    } catch (e: JSONException) {
                        Timber.e(e,"Error while parsing assertion json.")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Timber.e(e,"Call failed.")
                }
            })
        }
    }

    fun attemptSignIn(username: String, callback: AttemptSignInCallback) {
        val url: HttpUrl = actionServerUrlWithChallenge
                .addQueryParameter("act", "getassertion")
                .addQueryParameter("userid", username)
                .build()
        val request: Request = Request.Builder()
                .url(url)
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                var rawResponse = response.body()?.string()
                if (rawResponse?.isNotBlank() != true) {
                    uiHandler.post { callback.onError("Something is interfering with our connection to the login server. Most likely, your internet provider needs you to re-log-in, or your internet provider is blocking Pokémon Showdown.") }
                    return
                }
                if (rawResponse.startsWith("<!doctype html", true)) {
                    // some sort of MitM proxy; ignore it
                    rawResponse = rawResponse.substringAfter('>')
                }
                rawResponse = rawResponse.removePrefix("\r").removePrefix("\n")
                if (rawResponse.contains('<')) {
                    uiHandler.post { callback.onError("Something is interfering with our connection to the login server. Most likely, your internet provider needs you to re-log-in, or your internet provider is blocking Pokémon Showdown.") }
                    return
                }
                when {
                    rawResponse == ";" -> {
                        uiHandler.post { callback.onAuthenticationRequired() }
                    }
                    rawResponse == ";;@gmail" -> {
                        uiHandler.post { callback.onError("Google log-in is not supported in this client, please use another account.") }
                    }
                    rawResponse.substring(0, 2) == ";;" -> {
                        val errorReason: String = rawResponse.substring(2)
                        uiHandler.post { callback.onError(errorReason) }
                    }
                    rawResponse.indexOf('\n') >= 0 -> {
                        uiHandler.post { callback.onError("Something is interfering with our connection to the login server.") }
                    }
                    else -> {
                        sendTrnMessage(username, rawResponse)
                        storeAuthCookieIfAny(response.headers("Set-Cookie"))
                        uiHandler.post { callback.onSuccess() }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                uiHandler.post { callback.onError("An error occurred with your internet connection.") }
                Timber.e(e,"Call failed.")
            }
        })
    }

    fun attemptSignIn(username: String, password: String, callback: AttemptSignInCallback) {
        val dummyUrl: HttpUrl = actionServerUrl
                .addQueryParameter("act", "login")
                .addQueryParameter("name", username)
                .addQueryParameter("pass", password)
                .addQueryParameter("challstr", getSharedData<String>("challenge"))
                .build()
        val mediaType = MediaType.parse("application/x-www-form-urlencoded;")
        val request = Request.Builder()
                .url(actionServerUrl.build())
                .post(RequestBody.create(mediaType, dummyUrl.query()!!))
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                var rawResponse = response.body()?.string()
                if (rawResponse?.isNotEmpty() != true) {
                    uiHandler.post { callback.onError("Something is interfering with our connection to the login server. Most likely, your internet provider needs you to re-log-in, or your internet provider is blocking Pokémon Showdown.") }
                    return
                }
                try {
                    val json = JSONObject(rawResponse.removePrefix("]"))
                    if (json.optJSONObject("curuser")?.optBoolean("loggedin") == true) {
                        // success!
                        storeAuthCookieIfAny(response.headers("Set-Cookie"))
                        sendTrnMessage(username, json.getString("assertion"))
                        uiHandler.post { callback.onSuccess() }
                        return
                    }
                } catch (e: JSONException) {
                    Timber.e(e,"Error while parsing connection result json.")
                }
                uiHandler.post { callback.onError("Wrong password, please try again.") }
            }

            override fun onFailure(call: Call, e: IOException) {
                uiHandler.post { callback.onError("An error occurred with your internet connection.") }
                Timber.e(e,"Call failed.")
            }
        })
    }

    private val actionServerUrl: HttpUrl.Builder
        get() = HttpUrl.Builder()
                .scheme("https")
                .host("play.pokemonshowdown.com")
                .addPathSegment("action.php")

    private val actionServerUrlWithChallenge: HttpUrl.Builder
        get() = actionServerUrl.addQueryParameter("challstr", getSharedData<String>("challenge"))


    private fun storeAuthCookieIfAny(cookies: List<String>) {
        cookies.first { it.startsWith("sid") }.let { cookie ->
            val encodedCookie = Base64.encode(cookie.substringBefore(';').toByteArray(), Base64.DEFAULT)
            getSharedPreferences("user", Context.MODE_PRIVATE).edit()
                    .putString("token", String(encodedCookie))
                    .apply()
        }
    }

    private fun retrieveAuthCookieIfAny() = getSharedPreferences("user", Context.MODE_PRIVATE)
                .getString("token", null)?.let {
                    String(Base64.decode(it, Base64.DEFAULT))
                }

    fun forgetUserLoginInfos() = getSharedPreferences("user", Context.MODE_PRIVATE).edit().clear().apply()

    fun putSharedData(key: String, data: Any?) {
        sharedData[key] = data
    }

    fun <T> getSharedData(key: String): T? {
        return sharedData[key] as T?
    }

    inner class Binder : android.os.Binder() {
        val service: ShowdownService
            get() = this@ShowdownService
    }

    interface AttemptSignInCallback {
        fun onSuccess()
        fun onError(reason: String)
        fun onAuthenticationRequired()
    }
}