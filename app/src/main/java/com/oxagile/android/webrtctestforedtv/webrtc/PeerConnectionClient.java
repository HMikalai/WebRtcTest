package com.oxagile.android.webrtctestforedtv.webrtc;

import android.content.Context;
import android.util.Log;

import androidx.annotation.IntDef;

import com.oxagile.android.webrtctestforedtv.webrtc.model.Candidate;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CapturerObserver;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.webrtc.SessionDescription.Type.ANSWER;

public class PeerConnectionClient {
    private static final int VIDEO_RESOLUTION_WIDTH = 1920;
    private static final int VIDEO_RESOLUTION_HEIGHT = 1080;
    private static final int FPS = 30;
    private static final String LOCAL_VIDEO_TRACK_ID = "localVideoTrackId";
    private static final String LOCAL_AUDIO_TRACK_ID = "localAudioId";
    private static final String LABEL_OF_LOCAL_MEDIA_STREAM = "mediaStream";

    private final SurfaceViewRenderer localSurfaceViewRenderer;
    private final SurfaceViewRenderer remoteSurfaceViewRenderer;
    private final PeerConnectionClientListener peerConnectionClientListener;

    private final EglBase rootEglBase;
    private final Context appContext;
    private PeerConnectionFactory peerConnectionFactory;

    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoSource localVideoSource;
    private VideoCapturer videoCapturer;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;

    private PeerConnection peerConnection;
    private int connectionType;
    private String liveStreamId;

    private PeerConnectionEvents connectionEvents = PeerConnectionEvents.getInstance();

    private boolean frontCamera = true;

    private final MediaConstraints mediaConstraints = new MediaConstraints();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PeerConnectionClient(SurfaceViewRenderer localSurfaceViewRenderer, SurfaceViewRenderer remoteSurfaceViewRenderer, PeerConnectionClientListener peerConnectionClientListener) {
        Context context = null;
        if (localSurfaceViewRenderer != null && localSurfaceViewRenderer.getContext() != null) {
            context = localSurfaceViewRenderer.getContext();
        } else if (remoteSurfaceViewRenderer != null && remoteSurfaceViewRenderer.getContext() != null) {
            context = remoteSurfaceViewRenderer.getContext();
        }
        if (context == null) {
            throw new NullPointerException("The context is null");
        }

        this.localSurfaceViewRenderer = localSurfaceViewRenderer;
        this.remoteSurfaceViewRenderer = remoteSurfaceViewRenderer;

        this.peerConnectionClientListener = peerConnectionClientListener;
        rootEglBase = EglBase.create();
        this.appContext = context.getApplicationContext();

        initPeerConnectionFactory();
        createPeerConnectionFactory();
        initSurfaceViews();

        connectionEvents.setListener(new ConnectionListener());
    }

    public void startPreviewFromLocalCamera() {
        createLocalVideoTrack(localSurfaceViewRenderer);//here preview will be start
        createLocalAudioTrack();
    }

    public void changeCamera() {
        frontCamera = !frontCamera;
        disposeLocalVideoTrack();
        createLocalVideoTrack(localSurfaceViewRenderer);
    }

    public void startStream(String liveStreamId) {
        createPeerConnection(ConnectionType.PRESENTER, liveStreamId);
    }

    public void connectToStream(String liveStreamId) {
        createPeerConnection(ConnectionType.VIEWER, liveStreamId);
    }

    public void closeConnection() {
        connectionEvents.closeConnection();
    }

    //
    private void initPeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
    }

    private void createPeerConnectionFactory() {
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableEncryption = false;
        options.disableNetworkMonitor = false;

        //specify the video codecs
        this.peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true, true))
                .setOptions(options)
                .createPeerConnectionFactory();
    }

    private void initSurfaceViews() {
        if (localSurfaceViewRenderer != null) {
            localSurfaceViewRenderer.setMirror(true);
            localSurfaceViewRenderer.setEnableHardwareScaler(true);
            localSurfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null); //rendererEvents
        }

        if (remoteSurfaceViewRenderer != null) {
            remoteSurfaceViewRenderer.setMirror(true);
            remoteSurfaceViewRenderer.setEnableHardwareScaler(true);
            remoteSurfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null); //rendererEvents
        }
    }

    private void createLocalVideoTrack(SurfaceViewRenderer surfaceViewRenderer) {
        localVideoSource = peerConnectionFactory.createVideoSource(false);//if true - video resolution will change dynamically
        String threadName = Thread.currentThread().getName();
        surfaceTextureHelper = SurfaceTextureHelper.create(threadName, rootEglBase.getEglBaseContext());

        videoCapturer = Utils.createVideoCapture(appContext, frontCamera);//choosing of front/back camera is here
        videoCapturer.initialize(surfaceTextureHelper, appContext, new VideoCaptureObserver(localVideoSource.getCapturerObserver()));

//        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);//////////////////////////////////////////////////////////////////////////////////////////////////////
        videoCapturer.changeCaptureFormat(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);
        String localVideoTrackId = LOCAL_VIDEO_TRACK_ID + UUID.randomUUID().toString();
        videoTrack = peerConnectionFactory.createVideoTrack(localVideoTrackId, localVideoSource);
        videoTrack.addSink(surfaceViewRenderer);
    }

    private void disposeLocalVideoTrack() {
        if (videoTrack != null) {
            videoTrack.dispose();
        }
        if (videoCapturer != null) {
            videoCapturer.dispose();
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
        }
        if (localVideoSource != null) {
            localVideoSource.dispose();
        }
    }

    private void createLocalAudioTrack() {
        AudioSource audioSource = peerConnectionFactory.createAudioSource(mediaConstraints);
        String localVideoTrackId = LOCAL_AUDIO_TRACK_ID + UUID.randomUUID().toString();
        audioTrack = peerConnectionFactory.createAudioTrack(localVideoTrackId, audioSource);
    }

    private void createPeerConnection(@ConnectionType int type, String liveStreamId) {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(Utils.getIceServers());
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnectionObserver());

        String labelOfLocalMediaStream = LABEL_OF_LOCAL_MEDIA_STREAM + UUID.randomUUID().toString();
        MediaStream localMediaStream = peerConnectionFactory.createLocalMediaStream(labelOfLocalMediaStream);

        localMediaStream.addTrack(videoTrack);
        localMediaStream.addTrack(audioTrack);

        if (peerConnection != null) {
            peerConnection.addStream(localMediaStream);
            this.connectionType = type;
            this.liveStreamId = liveStreamId;
            connectionEvents.connect(null);
        }
    }

    private static class Utils {
        private Utils() {
        }

        private static VideoCapturer createVideoCapture(Context appContext, boolean fromFrontCamera) {
            boolean isSupportedCamera2 = Camera2Enumerator.isSupported(appContext);
            VideoCapturer videoCapturer;
            if (isSupportedCamera2) {
                videoCapturer = createCameraCapture(new Camera2Enumerator(appContext), fromFrontCamera);
            } else {
                videoCapturer = createCameraCapture(new Camera1Enumerator(true), fromFrontCamera);
            }
            return videoCapturer;
        }

        private static VideoCapturer createCameraCapture(CameraEnumerator enumerator, boolean fromFrontCamera) {
            final String[] deviceNames = enumerator.getDeviceNames();

            if (fromFrontCamera) {
                for (String deviceName : deviceNames) {
                    if (enumerator.isFrontFacing(deviceName)) {
                        VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                        if (videoCapturer != null) {
                            return videoCapturer;
                        }
                    }
                }
            } else {
                for (String deviceName : deviceNames) {
                    if (enumerator.isBackFacing(deviceName)) {
                        VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                        if (videoCapturer != null) {
                            return videoCapturer;
                        }
                    }
                }
            }
            return null;
        }

        private static List<PeerConnection.IceServer> getIceServers() {
            List<PeerConnection.IceServer> iceServers = new ArrayList<>();
            iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
            iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
            iceServers.add(PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer());
            iceServers.add(PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer());
            iceServers.add(PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer());
            return iceServers;
        }
    }

    private class VideoCaptureObserver implements CapturerObserver {
        private final CapturerObserver capturerObserver;

        VideoCaptureObserver(CapturerObserver capturerObserver) {
            this.capturerObserver = capturerObserver;
        }

        @Override
        public void onCapturerStarted(boolean success) {
            if (success && peerConnectionClientListener != null) {
                peerConnectionClientListener.onLocalVideoCapturerStarted();
            }
            capturerObserver.onCapturerStarted(success);
        }

        @Override
        public void onCapturerStopped() {
            if (peerConnectionClientListener != null) {
                peerConnectionClientListener.onLocalVideoCapturerStopped();
            }
            capturerObserver.onCapturerStopped();
        }

        @Override
        public void onFrameCaptured(VideoFrame frame) {
            capturerObserver.onFrameCaptured(frame);
        }
    }

    private class PeerConnectionObserver implements PeerConnection.Observer {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        }

        @Override
        public void onConnectionChange(PeerConnection.PeerConnectionState newState) {

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d("HMfilterOkHttp", "send iceCandidate");
            connectionEvents.onIceCandidate(iceCandidate);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            if (remoteSurfaceViewRenderer != null) {
                mediaStream.videoTracks.get(0).addSink(remoteSurfaceViewRenderer);
            }
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }

        @Override
        public void onTrack(RtpTransceiver transceiver) {

        }
    }

    private class SDPObserver implements SdpObserver {
        private final int type;
        private final String liveStreamId;

        SDPObserver(@ConnectionType int type, String liveStreamId) {
            this.type = type;
            this.liveStreamId = liveStreamId;
        }

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d("HMfilterOkHttp", "SDPObserver#onCreateSuccess: " + sessionDescription.description);
            peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
            if (type == ConnectionType.PRESENTER) {
                connectionEvents.onPresenterSdp(peerConnection.getLocalDescription().description, liveStreamId);
            } else if (type == ConnectionType.VIEWER) {
                connectionEvents.onViewerSdp(peerConnection.getLocalDescription().description, liveStreamId);
            }
        }

        @Override
        public void onSetSuccess() {
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d("HMfilterOkHttp", "SDPObserver#onCreateFailure s: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.d("HMfilterOkHttp", "SDPObserver#onSetFailure s: " + s);
        }
    }

    private class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d("HMfilterOkHttp", "SimpleSdpObserver#onCreateSuccess");
        }

        @Override
        public void onSetSuccess() {
            Log.d("HMfilterOkHttp", "SimpleSdpObserver#onSetSuccess");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d("HMfilterOkHttp", "SimpleSdpObserver#onCreateFailure s: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.d("HMfilterOkHttp", "SimpleSdpObserver#onSetFailure s: " + s);
        }
    }

    private class ConnectionListener implements PeerConnectionEvents.PeerConnectionEventsListener {
        @Override
        public void connected() {
//            Log.d("HMfilterOkHttp", "ConnectionListener#connected");
//
//            MediaConstraints sdpMediaConstraints = new MediaConstraints();
//            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
//            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
//
//            peerConnection.createOffer(new SDPObserver(connectionType, liveStreamId), sdpMediaConstraints);
            peerConnection.createOffer(new SDPObserver(connectionType, liveStreamId), mediaConstraints);
        }

        @Override
        public void onRemoteSdp(String sdp) {
            Log.d("HMfilterOkHttp", "ConnectionListener#onRemoteSdp: " + sdp);
            peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, sdp));
        }

        @Override
        public void onRemoteIceCandidate(Candidate candidate) {
            Log.d("HMfilterOkHttp", "ConnectionListener#onRemoteIceCandidate");
            IceCandidate iceCandidate = new IceCandidate(candidate.getSdpMid(), candidate.getSdpMLineIndex(), candidate.getCandidate());
            peerConnection.addIceCandidate(iceCandidate);
        }

        @Override
        public void notConnected() {
            Log.d("HMfilterOkHttp", "ConnectionListener#notConnected");
        }
    }

    public interface PeerConnectionClientListener {
        void onLocalVideoCapturerStarted();

        void onLocalVideoCapturerStopped();
    }

    @IntDef({ConnectionType.PRESENTER, ConnectionType.VIEWER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionType {
        int PRESENTER = 1000;
        int VIEWER = 1001;
    }
}
