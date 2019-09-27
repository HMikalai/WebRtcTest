package com.oxagile.android.webrtctestforedtv.webrtc;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.oxagile.android.webrtctestforedtv.webrtc.constants.MessageDataType;
import com.oxagile.android.webrtctestforedtv.webrtc.model.Candidate;
import com.oxagile.android.webrtctestforedtv.webrtc.model.Message;
import com.oxagile.android.webrtctestforedtv.webrtc.websocket.WebSocketConnection;
import com.oxagile.android.webrtctestforedtv.webrtc.websocket.WebSocketConnectionListener;

import org.webrtc.IceCandidate;

public class PeerConnectionEvents {
    private static final String DEFAULT_WEB_SOCKET_URL = "wss://edtv-java-dev.oxagile.com:8443/stream"; //todo will be remove

    private static PeerConnectionEvents instance;
    private final Gson gson = new Gson();
    private WebSocketConnection webSocketConnection;
    private String webSocketUrl;
    private boolean needReconnect;
    private boolean isConnected;
    private PeerConnectionEventsListener listener;

    public static PeerConnectionEvents getInstance() {
        if (instance == null) {
            instance = new PeerConnectionEvents();
        }
        return instance;
    }

    private PeerConnectionEvents() {
    }

    public void setListener(PeerConnectionEventsListener listener) {
        this.listener = listener;
    }

    public void connect(String webSocketUrl) {
        this.webSocketUrl = TextUtils.isEmpty(webSocketUrl) ? DEFAULT_WEB_SOCKET_URL : webSocketUrl;
        if (webSocketConnection != null) {
            webSocketConnection.close();
            needReconnect = true;
        } else {
            connect();
        }
    }

    public void onPresenterSdp(String sdpOffer, String userId, String liveStreamId) {
        Message message = new Message(MessageDataType.PRESENTER, sdpOffer, userId, liveStreamId);
        send(message);
    }

    public void onViewerSdp(String sdpOffer, String userId, String liveStreamId) {
        Message message = new Message(MessageDataType.VIEWER, sdpOffer, userId, liveStreamId);
        send(message);
    }

    public void onIceCandidate(IceCandidate iceCandidate) {
        Message message = new Message(new Candidate(iceCandidate.sdp, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex));
        send(message);
    }

    public void closeConnection() {
        webSocketConnection.close();
    }

    private void send(Message message) {
        if (isConnected) {
            String json = gson.toJson(message);
            webSocketConnection.sendMessage(json);
        } else if (listener != null) {
            listener.notConnected();
        }
    }

    private void connect() {
        webSocketConnection = new WebSocketConnection(webSocketUrl, new ConnectionListener());
    }

    private class ConnectionListener implements WebSocketConnectionListener {
        @Override
        public void onOpen() {
            isConnected = true;
            if(listener != null) {
                listener.connected();
            }
        }

        @Override
        public void onMessage(String json) {
            Message message = gson.fromJson(json, Message.class);
            switch (message.getType()) {
                case MessageDataType.PRESENTER_RESPONSE:
                case MessageDataType.VIEWER_RESPONSE:
                    if(listener != null) {
                        listener.onRemoteSdp(message.getSdpAnswer());
                    }
                    break;
                case MessageDataType.ICE_CANDIDATE_FOR_RECEIVE:
                    if(listener != null) {
                        listener.onRemoteIceCandidate(message.getCandidate());
                    }
                    break;
                case MessageDataType.PRESENTER:
                case MessageDataType.VIEWER:
                case MessageDataType.STOP:
                    throw new IllegalStateException("Illegal message type: " + message.getType());
            }
        }

        @Override
        public void onRemoteClosing() {
            isConnected = false;
        }

        @Override
        public void onClosed() {
            isConnected = false;
            if (needReconnect) {
                needReconnect = false;
                connect();
            }
        }

        @Override
        public void onErrorClosed(Throwable t) {
            isConnected = false;
        }
    }

    public interface PeerConnectionEventsListener {
        void connected();

        void onRemoteSdp(String sdp);

        void onRemoteIceCandidate(Candidate candidate);

        void notConnected();
    }
}
