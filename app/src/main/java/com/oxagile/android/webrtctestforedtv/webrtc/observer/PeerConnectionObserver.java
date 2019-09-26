package com.oxagile.android.webrtctestforedtv.webrtc.observer;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;

public class PeerConnectionObserver implements PeerConnection.Observer {
    @Override
    public void onSignalingChange(PeerConnection.SignalingState newState) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onSignalingChange");
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceConnectionChange");
    }

    @Override
    public void onStandardizedIceConnectionChange(PeerConnection.IceConnectionState newState) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onStandardizedIceConnectionChange");
    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onConnectionChange");
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceConnectionReceivingChange");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceGatheringChange");
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceCandidate");
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceCandidatesRemoved");
    }

    @Override
    public void onAddStream(MediaStream stream) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onAddStream");
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onRemoveStream");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onDataChannel");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onRenegotiationNeeded");
    }

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
        Log.d("HMfilterOkHttp", "onAddTrack() called with: rtpReceiver = [" + receiver + "], mediaStreams = [" + mediaStreams + "]");
        try {

            Log.d("HMfilterOkHttp", "VideoTrack  mediaStreams[0].videoTracks.get(0).kind(): " + mediaStreams[0].videoTracks.get(0).kind());
            Log.d("HMfilterOkHttp", "VideoTrack  mediaStreams[0].videoTracks.get(0).id(): " + mediaStreams[0].videoTracks.get(0).id());
        } catch (Exception e) {
            Log.d("HMfilterOkHttp", "VideoTrack  mediaStreams[0].videoTracks.get(0).kind(): bad");
            Log.d("HMfilterOkHttp", "VideoTrack  mediaStreams[0].videoTracks.get(0).id(): bad");
        }
    }

    @Override
    public void onTrack(RtpTransceiver transceiver) {
        Log.d("HMfilterOkHttp", "PeerConnectionObserver#onTrack");
    }
}