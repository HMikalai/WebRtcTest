package com.oxagile.android.webrtctestforedtv.webrtc.websocket;


import android.text.TextUtils;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;

public class WebSocketConnection {

    private final WebSocket ws;

    public WebSocketConnection(String webSocketUrl, WebSocketConnectionListener listener) {
        OkHttpClient client = getClient();

        Request request = new Request.Builder().url(webSocketUrl).build();
        this.ws = client.newWebSocket(request, new EchoWebSocketListener(listener));

        client.dispatcher().executorService().shutdown();
    }

    public void sendMessage(String message) {
        if (ws != null && !TextUtils.isEmpty(message)) {
            ws.send(message);
        }
    }

    public void close() {
        ws.close(1000, null);
    }

    private OkHttpClient getClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cache(null);
        builder.addInterceptor(getHttpLoggingInterceptor(true));
        return builder.build();
    }

    private static HttpLoggingInterceptor getHttpLoggingInterceptor(boolean showLog) {
        return new HttpLoggingInterceptor().setLevel((showLog ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE));
    }

    private class EchoWebSocketListener extends WebSocketListener {

        private final WebSocketConnectionListener listener;

        EchoWebSocketListener(WebSocketConnectionListener listener) {
            this.listener = listener;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            if (listener != null) {
                listener.onOpen();
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if (listener != null) {
                listener.onMessage(text);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            if (listener != null) {
                listener.onRemoteClosing();
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            if (listener != null) {
                listener.onClosed();
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            if (listener != null) {
                listener.onErrorClosed(t);
            }
        }
    }
}
