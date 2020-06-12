package com.majeur.psclient.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.majeur.psclient.util.S
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class ShowdownService : Service() {

    companion object {
        private val TAG: String = ShowdownService::class.java.simpleName
        private const val WS_CLOSE_NORMAL: Int = 1000
        private const val WS_CLOSE_GOING_AWAY: Int = 1001
        private const val WS_CLOSE_NETWORK_ERROR: Int = 4001
        private const val SHOWDOWN_SOCKET_URL: String = "wss://sim3.psim.us/showdown/websocket"
    }

    lateinit var okHttpClient: OkHttpClient
        private set

    private lateinit var binder: Binder
    private lateinit var uiHandler: Handler

    private val messageObservers = mutableListOf<AbsMessageObserver>()
    private val sharedData = mutableMapOf<String, Any>()
    private var webSocket: WebSocket? = null
    private var messageCache = mutableListOf<String>()
    private var _connected = AtomicBoolean(false)

    var isConnected: Boolean
        private set(value) = _connected.set(value)
        get() = _connected.get()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Lifecycle: onCreate()")
        uiHandler = Handler(Looper.getMainLooper())
        binder = Binder()
        okHttpClient = OkHttpClient.Builder()
                .build()
    }

    override fun onBind(intent: Intent) = binder

    override fun onUnbind(intent: Intent): Boolean {
        disconnectFromServer()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isConnected) webSocket?.close(WS_CLOSE_GOING_AWAY, null)
    }

    fun connectToServer() {
        if (isConnected) return
        Log.d(TAG, "Attempting to open WS connection.")
        val request = Request.Builder().url(SHOWDOWN_SOCKET_URL).build()
        webSocket = okHttpClient.newWebSocket(request, webSocketListener)
    }

    fun reconnectToServer() {
        disconnectFromServer()
        connectToServer()
    }

    fun disconnectFromServer() {
        if (!isConnected) return
        Log.d(TAG, "Attempting to close WS connection.")
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
            Log.i("$TAG[SEND]", message)
            webSocket?.send(message)
        } else {
            Log.w(TAG, "WebSocket not opened. Ignoring message: $message")
        }
    }

    fun registerMessageObserver(observer: AbsMessageObserver) {
        messageObservers.add(observer)
        observer.service = this
    }

    fun unregisterMessageObserver(observer: AbsMessageObserver) {
        messageObservers.remove(observer)
        observer.service = null
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
        messageObservers
                .filter { it.interceptCommandBefore.contains(msg.command) }
                .forEach { it.postMessage(msg, forcePost = true) }
        messageObservers
                .forEach { it.postMessage(msg) }
        messageObservers
                .filter { it.interceptCommandAfter.contains(msg.command) }
                .forEach { it.postMessage(msg, forcePost = true) }
    }

    fun fakeBattle() {
        S.run = true
        processServerData(S.s)
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i("$TAG[OPEN]", response.request().url().host())
            isConnected = true
            uiHandler.post {
                dispatchMessage(ServerMessage("lobby", "|connected|"))
            }
        }

        override fun onMessage(webSocket: WebSocket, data: String) {
            Log.i("$TAG[RECEIVE]", data)
            uiHandler.post {
                if (messageObservers.isEmpty()) {
                    messageCache.add(data)
                } else {
                    while (messageCache.isNotEmpty()) processServerData(messageCache.removeAt(0))
                    processServerData(data)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.i("$TAG[CLOSING]", reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w("$TAG[ERR]", t)
            isConnected = false
            this@ShowdownService.webSocket = null
            uiHandler.post {
                dispatchMessage(ServerMessage("lobby", "|networkerror|"))
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i("$TAG[CLOSED]", reason)
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
                        Log.e(TAG, "Assertion request responded with an empty body.")
                        return
                    }
                    try {
                        val resultJson = JSONObject(rawResponse.removePrefix("]"))
                        if (resultJson.optBoolean("loggedin"))
                            sendTrnMessage(resultJson.getString("username"),
                                    resultJson.getString("assertion"))
                    } catch (e: JSONException) {
                        Log.e(TAG, "Error while parsing assertion json.", e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Call failed.", e)
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
                Log.e(TAG, "Call failed.", e)
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
                    Log.e(TAG, "Error while parsing connection result json.", e)
                }
                uiHandler.post { callback.onError("Wrong password, please try again.") }
            }

            override fun onFailure(call: Call, e: IOException) {
                uiHandler.post { callback.onError("An error occurred with your internet connection.") }
                Log.e(TAG, "Call failed.", e)
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

    fun putSharedData(key: String, data: Any) {
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