package com.oxagile.android.webrtctestforedtv.webrtc;

import android.content.Context;
import android.util.Log;

import com.oxagile.android.webrtctestforedtv.webrtc.constants.ConnectionType;
import com.oxagile.android.webrtctestforedtv.webrtc.model.Candidate;
import com.oxagile.android.webrtctestforedtv.webrtc.observer.PeerConnectionObserver;
import com.oxagile.android.webrtctestforedtv.webrtc.observer.RendererObserver;
import com.oxagile.android.webrtctestforedtv.webrtc.observer.SimpleSdpObserver;
import com.oxagile.android.webrtctestforedtv.webrtc.utils.IceServersUtils;
import com.oxagile.android.webrtctestforedtv.webrtc.utils.VideoCapturerUtils;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.UUID;

import static org.webrtc.SessionDescription.Type.ANSWER;

public class PeerConnectionClient {
    private static final int VIDEO_RESOLUTION_WIDTH = 1080;
    private static final int VIDEO_RESOLUTION_HEIGHT = 1920;
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
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private MediaStream localMediaStream;

    private PeerConnection peerConnection;
    private ConnectionType connectionType;
    private String liveStreamId;

    private final String userId = UUID.randomUUID().toString(); // will be remove.

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
        createLocalVideoTrack(frontCamera);//here preview will be start
        createLocalAudioTrack();
    }

    public void startLikeViewer() {
        connectionType = ConnectionType.VIEWER;
        createLocalVideoTrack(frontCamera);//here preview will be start
        createLocalAudioTrack();
    }

    public void changeCamera() {
        if(peerConnection != null) {
            localMediaStream.removeTrack(localVideoTrack);
            disposeLocalVideoTrack();
            frontCamera = !frontCamera;
            createLocalVideoTrack(frontCamera);
            localMediaStream.addTrack(localVideoTrack);
        }
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

    private void initPeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
    }

    private void createPeerConnectionFactory() {
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        //specify the video codecs
        this.peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true, true))
                .setOptions(options)
                .createPeerConnectionFactory();
    }

    private void initSurfaceViews() {
        if (localSurfaceViewRenderer != null) {
//            localSurfaceViewRenderer.setMirror(true);
            localSurfaceViewRenderer.setEnableHardwareScaler(true);
            localSurfaceViewRenderer.init(rootEglBase.getEglBaseContext(), new RendererObserver() {
                @Override
                public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
                    Log.d("HMfilterOkHttp", "LocalFrameResolution: " + videoWidth + " x " + videoHeight);
                }
            });
        }

        if (remoteSurfaceViewRenderer != null) {
            remoteSurfaceViewRenderer.setEnableHardwareScaler(true);
            remoteSurfaceViewRenderer.init(rootEglBase.getEglBaseContext(), new RendererObserver() {
                @Override
                public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
                    Log.d("HMfilterOkHttp", "RemoteFrameResolution: " + videoWidth + " x " + videoHeight);
                }
            });
        }
    }

    private void createLocalVideoTrack(boolean frontCamera) {
        localVideoSource = peerConnectionFactory.createVideoSource(false);//if false - video resolution will change dynamically
        String threadName = Thread.currentThread().getName();
        surfaceTextureHelper = SurfaceTextureHelper.create(threadName, rootEglBase.getEglBaseContext());

        videoCapturer = VideoCapturerUtils.createVideoCapture(appContext, frontCamera);//choosing of front/back camera is here
        videoCapturer.initialize(surfaceTextureHelper, appContext, localVideoSource.getCapturerObserver());

//        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);//////////////////////////////////////////////////////////////////////////////////////////////////////
        videoCapturer.changeCaptureFormat(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);
        String localVideoTrackId = LOCAL_VIDEO_TRACK_ID + "_" + connectionType.name();
        localVideoTrack = peerConnectionFactory.createVideoTrack(localVideoTrackId, localVideoSource);
        localVideoTrack.addSink(localSurfaceViewRenderer);
    }

    private void disposeLocalVideoTrack() {
        if (localVideoTrack != null) {
            localVideoTrack.dispose();
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
        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localAudioTrack = peerConnectionFactory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setVolume(10);
    }

    private void createPeerConnection() {
        peerConnection = peerConnectionFactory.createPeerConnection(IceServersUtils.getStunIceServers(), new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                connectionEvents.onIceCandidate(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                if (remoteSurfaceViewRenderer != null && mediaStream != null) {
                    if (mediaStream.videoTracks != null && !mediaStream.videoTracks.isEmpty() && mediaStream.videoTracks.get(0) != null) {
                        VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                        remoteVideoTrack.addSink(remoteSurfaceViewRenderer);
                    }
                    if(mediaStream.audioTracks != null && !mediaStream.audioTracks.isEmpty() && mediaStream.audioTracks.get(0) != null) {
                        AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                        remoteAudioTrack.setVolume(10);
                    }
                }
            }


//            @Override
//            public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
//                MediaStreamTrack track = receiver.track();
//                if (track instanceof VideoTrack) {
//                    track.setEnabled(true);
//                    ((VideoTrack) track).addSink(remoteSurfaceViewRenderer);
//                }
//            }
        });
        if (peerConnection != null) {
            localMediaStream = peerConnectionFactory.createLocalMediaStream(LABEL_OF_LOCAL_MEDIA_STREAM);

            localMediaStream.addTrack(localVideoTrack);
            localMediaStream.addTrack(localAudioTrack);

            peerConnection.addStream(localMediaStream);

//            localVideoTrack.setEnabled(true);
//            peerConnection.addTrack(localVideoTrack);
//            peerConnection.addTrack(localAudioTrack);

            connectionEvents.connect(null);
        }
    }

    private void createOffer() {
        String strBool = connectionType == ConnectionType.PRESENTER ? "false" : "true";
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", strBool));

        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                setLocalDescription(sessionDescription);
            }
        }, mediaConstraints);
    }

    private void setLocalDescription(SessionDescription sessionDescription) {
        peerConnection.setLocalDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                sendLocalDescription();
            }
        }, sessionDescription);
    }

    private void setRemoteDescription(String remoteSdp) {
        peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, remoteSdp));
    }

    private void sendLocalDescription() {
        if (connectionType == ConnectionType.PRESENTER) {
            connectionEvents.onPresenterSdp(peerConnection.getLocalDescription().description, userId, liveStreamId);
        } else if (connectionType == ConnectionType.VIEWER) {
            connectionEvents.onViewerSdp(peerConnection.getLocalDescription().description, userId, liveStreamId);
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
            IceCandidate iceCandidate = new IceCandidate(candidate.getSdpMid(), candidate.getSdpMLineIndex(), candidate.getCandidate());
            peerConnection.addIceCandidate(iceCandidate);
        }

        @Override
        public void notConnected() {
        }
    }
}
