package com.majeur.psclient.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.majeur.psclient.util.S;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

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

public class ShowdownService extends Service {

    private static final String TAG = ShowdownService.class.getSimpleName();
    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private Binder mBinder;

    private OkHttpClient mOkHttpClient;
    private WebSocket mWebSocket;
    private Handler mUiHandler;
    private Queue<String> mMessageCache;

    private List<ShowdownMessageHandler> mShowdownMessageHandlers;
    private Map<String, Object> mHandlersSharedData;
    private String mChallengeString;

    @Override
    public void onCreate() {
        super.onCreate();
        mUiHandler = new Handler(Looper.getMainLooper());
        mBinder = new Binder();
        mMessageCache = new LinkedList<>();
        mShowdownMessageHandlers = new LinkedList<>();
        mHandlersSharedData = new HashMap<>();

        mOkHttpClient = new OkHttpClient();

        Request request = new Request.Builder().url("ws://sim.smogon.com:8000/showdown/websocket").build();
        mWebSocket = mOkHttpClient.newWebSocket(request, mWebSocketListener);
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
        mWebSocket.close(NORMAL_CLOSURE_STATUS, "User is leaving");
    }

    /* package */ void retryShowdownServerConnection() {
        if (mWebSocket != null)
            mWebSocket.close(NORMAL_CLOSURE_STATUS, "No internet");
        Request request = new Request.Builder().url("ws://sim.smogon.com:8000/showdown/websocket").build();
        mWebSocket = mOkHttpClient.newWebSocket(request, mWebSocketListener);
    }

    public void sendTrnMessage(String userName, String assertion) {
        sendGlobalCommand("trn", userName + ",0," + assertion);
    }

    public void logout() {
        sendGlobalCommand("logout", null);
    }

    public void sendGlobalCommand(String command, String args) {
        sendRoomCommand(null, command, args);
    }

    public void sendRoomCommand(@Nullable String roomId, String command, @Nullable String args) {
        sendRoomMessage(Objects.toString(roomId, ""), "/" + command + " " + Objects.toString(args, ""));
    }

    public void sendRoomMessage(@Nullable String roomId, String message) {
        sendMessage(Objects.toString(roomId, "") + "|" + message);
    }

    private void sendMessage(String message) {
        Log.d(TAG, "Sending message: " + message);
        mWebSocket.send(message);
    }

    public void registerMessageHandler(ShowdownMessageHandler messageHandler) {
        mShowdownMessageHandlers.add(messageHandler);
        Collections.sort(mShowdownMessageHandlers, ShowdownMessageHandler.COMPARATOR);
        messageHandler.attachService(this);
    }

    public void unregisterMessageHandler(ShowdownMessageHandler messageHandler) {
        mShowdownMessageHandlers.remove(messageHandler);
        messageHandler.detachService();
    }

    private void dispatchMessages(String messages) {
        for (ShowdownMessageHandler messageHandler : mShowdownMessageHandlers) {
            if (messageHandler.shouldHandleMessages(messages)) {
                messageHandler.postMessages(messages);
            }
        }
    }

    public void fakeBattle() {
        S.run = true;
        dispatchMessages(S.s);
    }

    private boolean canDispatchMessages() {
        return !mShowdownMessageHandlers.isEmpty();
    }

    private void dispatchNetworkError() {
        for (ShowdownMessageHandler messageHandler : mShowdownMessageHandlers) {
            messageHandler.onNetworkError();
        }
    }

    private final WebSocketListener mWebSocketListener = new WebSocketListener() {

        private boolean mPreviousMessagesDispatched;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG, "WebSocked Opened");
        }

        @Override
        public void onMessage(WebSocket webSocket, final String text) {
            Log.d(TAG, "Receiving message: " + text);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    boolean canDispatchMessages = canDispatchMessages();

                    if (canDispatchMessages) {
                        if (!mPreviousMessagesDispatched)
                            while (!mMessageCache.isEmpty())
                                dispatchMessages(mMessageCache.remove());

                        dispatchMessages(text);
                        mPreviousMessagesDispatched = true;
                    } else {
                        mMessageCache.add(text);
                        mPreviousMessagesDispatched = false;
                    }
                }
            });
        }


        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            Log.d(TAG, "WebSocket closed (" + reason + ")");
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.e(TAG, "WebSocket error: " + t.toString());
            if (t instanceof UnknownHostException || t instanceof SocketTimeoutException) {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dispatchNetworkError();
                    }
                });
            }
        }
    };

    public void setChallengeString(String challengeString) {
        mChallengeString = challengeString;
    }

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
            public void onResponse(Call call, Response response) throws IOException {
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
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {

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
            public void onResponse(Call call, Response response) throws IOException {
                final String rawResponse = response.body().string();

                final boolean errorInUsername = rawResponse.startsWith(";;");
                final boolean userRegistered = rawResponse.length() == 1 && rawResponse.charAt(0) == ';';
                if (!errorInUsername && !userRegistered) {
                    sendTrnMessage(username, rawResponse);
                    storeAuthCookieIfAny(response.headers("Set-Cookie"));
                }

                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (errorInUsername) {
                            callback.onSignInAttempted(false, false, rawResponse.substring(2));
                        } else if (userRegistered) {
                            callback.onSignInAttempted(false, true, "This name is registered");
                        } else {
                            callback.onSignInAttempted(true, false, null);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {

            }
        });
    }

    public void attemptSignIn(final String username, final String password, final AttemptSignInCallback callback) {
        HttpUrl dummyUrl = getActionServerUrl()
                .addQueryParameter("act", "login")
                .addQueryParameter("name", username)
                .addQueryParameter("pass", password)
                .addQueryParameter("challstr", mChallengeString)
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
            public void onResponse(Call call, Response response) throws IOException {
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
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                callback.onSignInAttempted(success, true, null);
                            } else {
                                callback.onSignInAttempted(false, true, null);
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {

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
                .addQueryParameter("challstr", mChallengeString);
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

    /* package */ void putSharedData(String key, Object data) {
        mHandlersSharedData.put(key, data);
    }

    @SuppressWarnings("unchecked")
    /* package */ <T> T getSharedData(String key) {
        return (T) mHandlersSharedData.get(key);
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
        public void onSignInAttempted(boolean success, boolean registeredUsername, String reason);
    }
}
