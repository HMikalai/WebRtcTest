package com.oxagile.android.webrtctestforedtv.webrtc.utils;

import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

public class IceServersUtils {

    private IceServersUtils() {}

    public static List<PeerConnection.IceServer> getStunIceServers() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer());
        return iceServers;
    }
}
