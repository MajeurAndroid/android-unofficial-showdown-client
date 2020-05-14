package com.majeur.psclient.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.majeur.psclient.util.S;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShowdownService extends Service {

    private static final String TAG = ShowdownService.class.getSimpleName();
    private static final int WS_CLOSE_NORMAL = 1000;
    private static final int WS_CLOSE_GOING_AWAY = 1001;
    private static final int WS_CLOSE_NETWORK_ERROR = 4001;
    private static final String SHOWDOWN_SOCKET_URL = "wss://sim3.psim.us/showdown/websocket";

    private Binder mBinder;

    private OkHttpClient mOkHttpClient;
    private WebSocket mWebSocket;
    private Handler mUiHandler;
    private Queue<String> mMessageCache;

    private AtomicBoolean mConnected;

    private AbsMessageObserver mGlobalMessageObserver;
    private List<AbsMessageObserver> mMessageObservers;
    private Map<String, Object> mSharedData;

    @Override
    public void onCreate() {
        super.onCreate();
        mUiHandler = new Handler(Looper.getMainLooper());
        mBinder = new Binder();
        mMessageCache = new LinkedList<>();
        mConnected = new AtomicBoolean(false);
        mMessageObservers = new LinkedList<>();
        mSharedData = new HashMap<>();

        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy()");
        if (isConnected())
            mWebSocket.close(WS_CLOSE_GOING_AWAY, null);
    }

    public boolean isConnected() {
        return mConnected.get();
    }

    public void connectToServer() {
        if (isConnected())
            return;
        Log.d(TAG, "Attempting to open WS connection.");
        Request request = new Request.Builder().url(SHOWDOWN_SOCKET_URL).build();
        mWebSocket = mOkHttpClient.newWebSocket(request, mWebSocketListener);
    }

    public void reconnectToServer() {
        disconnectFromServer();
        connectToServer();
    }

    public void disconnectFromServer() {
        if (isConnected())
            mWebSocket.close(WS_CLOSE_NORMAL, null);
    }

    public void sendTrnMessage(String userName, String assertion) {
        sendGlobalCommand("trn", userName + ",0," + assertion);
    }

    public void sendGlobalCommand(String command, Object... args) {
        sendRoomCommand(null, command, args);
    }

    public void sendRoomCommand(@Nullable String roomId, String command, Object... args) {
        StringBuilder argsBuilder = new StringBuilder();
        if (args.length > 0) argsBuilder.append(args[0]);
        for (int i = 1; i < args.length; i++)
            argsBuilder.append('|').append(args[i]);

        sendRoomMessage(roomId, "/" + command + " " + argsBuilder.toString());
    }

    public void sendRoomMessage(@Nullable String roomId, String message) {
        sendMessage(Objects.toString(roomId, "") + "|" + message);
    }

    private void sendMessage(String message) {
        if (isConnected()) {
            Log.i(TAG + "[SEND]", message);
            mWebSocket.send(message);
        } else {
            Log.e(TAG, "Error: WebSocket not connected. Ignoring message: " + message);
        }
    }

    public void registerMessageObserver(AbsMessageObserver observer, boolean asGlobal) {
        if (asGlobal) {
            if (mGlobalMessageObserver != null)
                unregisterMessageObserver(mGlobalMessageObserver);
            mGlobalMessageObserver = observer;
        } else {
            mMessageObservers.add(observer);
        }
        observer.attachService(this);
    }

    public void unregisterMessageObserver(AbsMessageObserver observer) {
        if (observer == mGlobalMessageObserver)
            mGlobalMessageObserver = null;
        else
            mMessageObservers.remove(observer);
        observer.detachService();
    }

    private void processServerData(String data) {
        String roomId = null;
        if (data.charAt(0) == '>') {
            int lfIndex = data.indexOf('\n');
            if (lfIndex == -1)
                return;
            roomId = data.substring(1, lfIndex);
            data = data.substring(lfIndex + 1);
        }
        dispatchServerData(roomId, data);
    }

    private void dispatchServerData(String roomId, String data) {
        String[] lines = data.split("\n");
        for (String line : lines) {
            if (TextUtils.isEmpty(line))
                continue;

            ServerMessage message = new ServerMessage(roomId, line);

            //Log.i(message.command, message.args.toString());
            if (roomId == null) {
                if (!message.command.equals("init") && !message.command.equals("deinit")) {
                    boolean consumed = mGlobalMessageObserver != null && mGlobalMessageObserver.postMessage(message);
                    if (consumed) continue;
                }
                message.roomId = "lobby";
            }

            switch (message.command) {
                case "init":
                    if (mGlobalMessageObserver != null) mGlobalMessageObserver.postMessage(message);
                    dispatchMessage(message);
                    break;
                case "deinit":
                    dispatchMessage(message);
                    if (mGlobalMessageObserver != null) mGlobalMessageObserver.postMessage(message);
                    break;
                case "noinit":
                    if (mGlobalMessageObserver != null) mGlobalMessageObserver.postMessage(message);
                default:
                    dispatchMessage(message);
                    break;
            }
        }
    }

    private void dispatchMessage(ServerMessage message) {
        for (AbsMessageObserver observer : mMessageObservers)
            observer.postMessage(message);
    }

    public void fakeBattle() {
        S.run = true;
        processServerData(S.s);
    }

    private final WebSocketListener mWebSocketListener = new WebSocketListener() {

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            mConnected.set(true);
            Log.i(TAG + "[OPEN]", "");
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull final String data) {
            Log.i(TAG + "[RECEIVE]", data);
            mUiHandler.post(() -> {
                if (mGlobalMessageObserver == null && mMessageObservers.isEmpty()) {
                    mMessageCache.add(data);
                } else {
                    while (!mMessageCache.isEmpty())
                        processServerData(mMessageCache.remove());
                    processServerData(data);
                }
            });
        }


        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.i(TAG + "[CLOSING]", reason);
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, Throwable t, Response response) {
            Log.i(TAG + "[ERR]", t.toString());
            mConnected.set(false);
            mWebSocket = null;

            mUiHandler.post(() -> {
                if (mGlobalMessageObserver != null)
                    mGlobalMessageObserver.postMessage(new ServerMessage(null, "|error|network"));
            });

        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.i(TAG + "[CLOSED]", reason);
            mConnected.set(false);
            mWebSocket = null;
        }
    };

    public void tryCookieSignIn() {
        String cookie = retrieveAuthCookieIfAny();
        if (cookie == null)
            return;

        HttpUrl url = getActionServerUrlWithChallenge()
                .addQueryParameter("act", "upkeep")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("cookie", cookie)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                String rawResponse = response.body().string();
                if (rawResponse.charAt(0) != ']')
                    return;
                try {
                    JSONObject resultJson = new JSONObject(rawResponse.substring(1));
                    boolean userLogged = resultJson.getBoolean("loggedin");
                    if (userLogged)
                        sendTrnMessage(resultJson.getString("username"),
                                resultJson.getString("assertion"));

                } catch (JSONException e) {
                    Log.e(TAG, "Error while parsing assertion json.", e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Call failed.", e);
            }
        });
    }

    public void attemptSignIn(final String username, final AttemptSignInCallback callback) {
        HttpUrl url = getActionServerUrlWithChallenge()
                .addQueryParameter("act", "getassertion")
                .addQueryParameter("userid", username)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                final String rawResponse = response.body().string();

                final boolean errorInUsername = rawResponse.startsWith(";;");
                final boolean userRegistered = rawResponse.length() == 1 && rawResponse.charAt(0) == ';';
                if (!errorInUsername && !userRegistered) {
                    sendTrnMessage(username, rawResponse);
                    storeAuthCookieIfAny(response.headers("Set-Cookie"));
                }

                mUiHandler.post(() -> {
                    if (errorInUsername) {
                        callback.onSignInAttempted(false, false, rawResponse.substring(2));
                    } else if (userRegistered) {
                        callback.onSignInAttempted(false, true, "This name is registered");
                    } else {
                        callback.onSignInAttempted(true, false, null);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Call failed.", e);
            }
        });
    }

    public void attemptSignIn(final String username, final String password, final AttemptSignInCallback callback) {
        HttpUrl dummyUrl = getActionServerUrl()
                .addQueryParameter("act", "login")
                .addQueryParameter("name", username)
                .addQueryParameter("pass", password)
                .addQueryParameter("challstr", getSharedData("challenge"))
                .build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded;");
        RequestBody requestBody = RequestBody.create(mediaType, dummyUrl.query());

        HttpUrl url = getActionServerUrl().build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                final String rawResponse = response.body().string();
                if (rawResponse.charAt(0) != ']')
                    return;
                try {
                    final JSONObject jsonObject = new JSONObject(rawResponse.substring(1));
                    final boolean success = jsonObject.getBoolean("actionsuccess");
                    final String assertion = jsonObject.getString("assertion");
                    if (success) {
                        storeAuthCookieIfAny(response.headers("Set-Cookie"));
                        sendTrnMessage(username, assertion);
                    }
                    mUiHandler.post(() -> callback.onSignInAttempted(success, true, null));
                } catch (JSONException e) {
                    Log.e(TAG, "Error while parsing connection result json.", e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Call failed.", e);
            }
        });
    }

    private HttpUrl.Builder getActionServerUrl() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("play.pokemonshowdown.com")
                .addPathSegment("action.php");
    }

    private HttpUrl.Builder getActionServerUrlWithChallenge() {
        return getActionServerUrl()
                .addQueryParameter("challstr", getSharedData("challenge"));
    }

    private void storeAuthCookieIfAny(List<String> cookies) {
        String authCookie = null;
        for (String cookie : cookies)
            if ((authCookie = cookie).startsWith("sid"))
                break;
        if (authCookie == null)
            return;

        int endChar = authCookie.indexOf(';');
        authCookie = authCookie.substring(0, endChar + 1);
        byte[] encodedCookie = Base64.encode(authCookie.getBytes(), Base64.DEFAULT);
        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        sharedPreferences.edit().putString("token", new String(encodedCookie)).apply();
    }

    private String retrieveAuthCookieIfAny() {
        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        String encodedCookie = sharedPreferences.getString("token", null);
        if (encodedCookie == null)
            return null;
        return new String(Base64.decode(encodedCookie, Base64.DEFAULT));
    }

    public void forgetUserLoginInfos() {
        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    /* package */ void putSharedData(String key, Object data) {
        mSharedData.put(key, data);
    }

    @SuppressWarnings("unchecked")
    /* package */ <T> T getSharedData(String key) {
        return (T) mSharedData.get(key);
    }


    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public class Binder extends android.os.Binder {
        public ShowdownService getService() {
            return ShowdownService.this;
        }
    }

    public interface AttemptSignInCallback {
        void onSignInAttempted(boolean success, boolean registeredUsername, String reason);
    }
}
