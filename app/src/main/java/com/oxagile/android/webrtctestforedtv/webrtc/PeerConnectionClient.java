package com.oxagile.android.webrtctestforedtv.webrtc;

import android.content.Context;
import android.util.Log;

import com.oxagile.android.webrtctestforedtv.webrtc.constants.ConnectionType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    private final EglBase rootEglBase;
    private final Context appContext;
    private PeerConnectionFactory peerConnectionFactory;

    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoSource localVideoSource;
    private VideoCapturer videoCapturer;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;

    private PeerConnection peerConnection;
    private ConnectionType connectionType;
    private String liveStreamId;

    private PeerConnectionEvents connectionEvents = PeerConnectionEvents.getInstance();

    private boolean frontCamera = true;

    private final MediaConstraints mediaConstraints = new MediaConstraints();

    public PeerConnectionClient(SurfaceViewRenderer localSurfaceViewRenderer,
                                SurfaceViewRenderer remoteSurfaceViewRenderer) {
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

        rootEglBase = EglBase.create();
        this.appContext = context.getApplicationContext();

        initPeerConnectionFactory();
        createPeerConnectionFactory();
        initSurfaceViews();

        connectionEvents.setListener(new ConnectionListener());
    }

    public void startLikePresenter() {
        connectionType = ConnectionType.PRESENTER;
        createLocalVideoTrack(localSurfaceViewRenderer);//here preview will be start
        createLocalAudioTrack();
    }

    public void startLikeViewer() {
        connectionType = ConnectionType.VIEWER;
        createLocalVideoTrack(localSurfaceViewRenderer);//here preview will be start
        createLocalAudioTrack();
    }

    public void changeCamera() {
        frontCamera = !frontCamera;
        disposeLocalVideoTrack();
        createLocalVideoTrack(localSurfaceViewRenderer);
    }

    public void connect(String liveStreamId) {
        this.liveStreamId = liveStreamId;
        createPeerConnection();
    }

    public void closeConnection() {
        peerConnection.close();
        peerConnection.dispose();
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
//        options.disableEncryption = false;
//        options.disableNetworkMonitor = false;

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
            Log.d("HMfilterOkHttp", "initSurfaceViews - localSurfaceViewRenderer");
        }

        if (remoteSurfaceViewRenderer != null) {
            remoteSurfaceViewRenderer.setEnableHardwareScaler(true);
            remoteSurfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null); //rendererEvents
            Log.d("HMfilterOkHttp", "initSurfaceViews - remoteSurfaceViewRenderer");
        }
    }

    private void createLocalVideoTrack(SurfaceViewRenderer surfaceViewRenderer) {
        localVideoSource = peerConnectionFactory.createVideoSource(false);//if true - video resolution will change dynamically
        String threadName = Thread.currentThread().getName();
        surfaceTextureHelper = SurfaceTextureHelper.create(threadName, rootEglBase.getEglBaseContext());

        videoCapturer = Utils.createVideoCapture(appContext, frontCamera);//choosing of front/back camera is here
        videoCapturer.initialize(surfaceTextureHelper, appContext, localVideoSource.getCapturerObserver());

//        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);//////////////////////////////////////////////////////////////////////////////////////////////////////
        videoCapturer.changeCaptureFormat(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);
        String localVideoTrackId = LOCAL_VIDEO_TRACK_ID + "_" + connectionType.name();
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
        String localVideoTrackId = LOCAL_AUDIO_TRACK_ID + "_" + connectionType.name();
        audioTrack = peerConnectionFactory.createAudioTrack(localVideoTrackId, audioSource);
    }

    private void createPeerConnection() {
        peerConnection = peerConnectionFactory.createPeerConnection(Utils.getIceServers(), new PeerConnectionObserver(){
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceCandidate");
                connectionEvents.onIceCandidate(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                if (mediaStream != null && remoteSurfaceViewRenderer != null) {
                    Log.d("HMfilterOkHttp", "PeerConnectionObserver#onAddStream");
                    mediaStream.videoTracks.get(0).addSink(remoteSurfaceViewRenderer);
                }
            }
        });

        String labelOfLocalMediaStream = LABEL_OF_LOCAL_MEDIA_STREAM + "_" + connectionType.name();
        MediaStream localMediaStream = peerConnectionFactory.createLocalMediaStream(labelOfLocalMediaStream);

        localMediaStream.addTrack(videoTrack);
        localMediaStream.addTrack(audioTrack);

        if (peerConnection != null) {
            peerConnection.addStream(localMediaStream);
            connectionEvents.connect(null);
        }
    }

    private void createOffer() {
        peerConnection.createOffer(new SimpleSdpObserver(){
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                setLocalDescription(sessionDescription);
            }
        }, mediaConstraints);
    }

    private void setLocalDescription(SessionDescription sessionDescription) {
        peerConnection.setLocalDescription(new SimpleSdpObserver(){
            @Override
            public void onSetSuccess() {
                sendLocalDescription();
            }
        }, sessionDescription);
    }

    private void setRemoteDescription(String remoteSdp) {
        peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, remoteSdp));
        Log.d("HMfilterOkHttp", "Remote #SDP: " + remoteSdp);
    }

    private void sendLocalDescription() {
        if (connectionType == ConnectionType.PRESENTER) {
            connectionEvents.onPresenterSdp(peerConnection.getLocalDescription().description, liveStreamId);
        } else if (connectionType == ConnectionType.VIEWER) {
            connectionEvents.onViewerSdp(peerConnection.getLocalDescription().description, liveStreamId);
        }
        Log.d("HMfilterOkHttp", "Local #SDP: " + peerConnection.getLocalDescription().description);
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

    private class PeerConnectionObserver implements PeerConnection.Observer {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onSignalingChange");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceConnectionChange");
        }

        @Override
        public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onConnectionChange newState: " + newState.toString());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceConnectionReceivingChange");
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceGatheringChange");
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceCandidate");
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onIceCandidatesRemoved");
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onAddStream");
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
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
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onAddTrack");
        }

        @Override
        public void onTrack(RtpTransceiver transceiver) {
            Log.d("HMfilterOkHttp", "PeerConnectionObserver#onTrack");
        }
    }

    private class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
        }

        @Override
        public void onSetSuccess() {
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
            createOffer();
        }

        @Override
        public void onRemoteSdp(String remoteSdp) {
            setRemoteDescription(remoteSdp);
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

}
