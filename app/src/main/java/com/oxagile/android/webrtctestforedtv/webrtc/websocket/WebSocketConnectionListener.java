package com.oxagile.android.webrtctestforedtv.webrtc.websocket;

public interface WebSocketConnectionListener {

    void onOpen();

    void onMessage(String text);

    void onRemoteClosing();

    void onClosed();

    void onErrorClosed(Throwable t);
}
