package com.oxagile.android.webrtctestforedtv;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.oxagile.android.webrtctestforedtv.webrtc.PeerConnectionClient;

import org.webrtc.SurfaceViewRenderer;

public class MainActivity extends AppCompatActivity {

    private PeerConnectionClient peerConnectionClient;

    private Button startLikePresenter;
    private Button startLikeViewer;

    private Button changeCamera;
    private LinearLayout connectionControl;
    private EditText liveStreamId;
    private Button startStream;
    private Button connectToStream;

    private SurfaceViewRenderer localSurfaceViewRenderer;
    private SurfaceViewRenderer remoteSurfaceViewRenderer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLikePresenter = findViewById(R.id.start_like_presenter);
        startLikePresenter.setOnClickListener(v -> startLikePresenter());

        startLikeViewer = findViewById(R.id.start_like_viewer);
        startLikeViewer.setOnClickListener(v -> startLikeViewer());

        changeCamera = findViewById(R.id.change_camera);
//        changeCamera.setOnClickListener(v -> peerConnectionClient.changeCamera());

        connectionControl = findViewById(R.id.connection_control);
        liveStreamId = findViewById(R.id.live_stream_id);

        startStream = findViewById(R.id.start_stream);
        startStream.setOnClickListener(v -> startStream());

        connectToStream = findViewById(R.id.connect_to_stream);
        connectToStream.setOnClickListener(v -> connectToStream());

        Button closeWebSocket = findViewById(R.id.close_websocket);
        closeWebSocket.setOnClickListener(v -> peerConnectionClient.closeConnection());

        localSurfaceViewRenderer = findViewById(R.id.localSurfaceViewRenderer);
        remoteSurfaceViewRenderer = findViewById(R.id.remoteSurfaceViewRenderer);
        startWedRtc(localSurfaceViewRenderer, remoteSurfaceViewRenderer);
    }

    private void startLikePresenter(){
        startLikePresenter.setVisibility(View.GONE);
        startLikeViewer.setVisibility(View.GONE);
        connectToStream.setVisibility(View.GONE);
        changeCamera.setVisibility(View.VISIBLE);
        connectionControl.setVisibility(View.VISIBLE);

        peerConnectionClient.startLikePresenter();

        localSurfaceViewRenderer.setVisibility(View.VISIBLE);
        remoteSurfaceViewRenderer.setVisibility(View.GONE);
    }

    private void startLikeViewer(){
        startLikePresenter.setVisibility(View.GONE);
        startLikeViewer.setVisibility(View.GONE);
        startStream.setVisibility(View.GONE);
        changeCamera.setVisibility(View.VISIBLE);
        connectionControl.setVisibility(View.VISIBLE);

        peerConnectionClient.startLikeViewer();

        localSurfaceViewRenderer.setVisibility(View.GONE);
        remoteSurfaceViewRenderer.setVisibility(View.VISIBLE);
    }

    private void startWedRtc(SurfaceViewRenderer localSurfaceViewRenderer, SurfaceViewRenderer remoteSurfaceViewRenderer) {
        peerConnectionClient = new PeerConnectionClient(localSurfaceViewRenderer, remoteSurfaceViewRenderer);
    }


    private void startStream() {
        String strLiveStreamId = liveStreamId.getText().toString();
        if (!TextUtils.isEmpty(strLiveStreamId)) {
            peerConnectionClient.connect(strLiveStreamId);
        }
        hideKeyboard(liveStreamId);
    }

    private void connectToStream() {
        String strLiveStreamId = liveStreamId.getText().toString();
        if (!TextUtils.isEmpty(strLiveStreamId)) {
            peerConnectionClient.connect(strLiveStreamId);
        }
        hideKeyboard(liveStreamId);
    }

    public static void hideKeyboard(@Nullable View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
