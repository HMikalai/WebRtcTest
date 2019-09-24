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
    private Button startPreview;
    private Button changeCamera;
    private LinearLayout connectionControl;
    private EditText liveStreamId;

    private SurfaceViewRenderer localSurfaceViewRenderer;
    private SurfaceViewRenderer remoteSurfaceViewRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startWedRtc();

        startPreview = findViewById(R.id.start_preview);
        startPreview.setOnClickListener(v -> peerConnectionClient.startPreviewFromLocalCamera());

        changeCamera = findViewById(R.id.change_camera);
        changeCamera.setOnClickListener(v -> peerConnectionClient.changeCamera());

        connectionControl = findViewById(R.id.connection_control);
        liveStreamId = findViewById(R.id.live_stream_id);

        Button startStream = findViewById(R.id.start_stream);
        startStream.setOnClickListener(v -> startStream());

        Button connectToStream = findViewById(R.id.connect_to_stream);
        connectToStream.setOnClickListener(v -> connectToStream());

        Button closeWebSocket = findViewById(R.id.close_websocket);
        closeWebSocket.setOnClickListener(v -> peerConnectionClient.closeConnection());

    }

    private void startWedRtc() {
        this.localSurfaceViewRenderer = findViewById(R.id.localSurfaceViewRenderer);
        this.remoteSurfaceViewRenderer = findViewById(R.id.remoteSurfaceViewRenderer);
        peerConnectionClient = new PeerConnectionClient(localSurfaceViewRenderer, remoteSurfaceViewRenderer, new PeerConnectionClient.PeerConnectionClientListener() {
            @Override
            public void onLocalVideoCapturerStarted() {
                runOnUiThread(() -> {
                    startPreview.setVisibility(View.GONE);
                    changeCamera.setVisibility(View.VISIBLE);
                    connectionControl.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onLocalVideoCapturerStopped() {
                runOnUiThread(() -> {
                    startPreview.setVisibility(View.VISIBLE);
                    changeCamera.setVisibility(View.GONE);
                    connectionControl.setVisibility(View.GONE);
                });
            }
        });
    }

    private void startStream() {
        String strLiveStreamId = liveStreamId.getText().toString();
        if (!TextUtils.isEmpty(strLiveStreamId)) {
            peerConnectionClient.startStream(strLiveStreamId);
        }
        hideKeyboard(liveStreamId);
    }

    private void connectToStream() {
        String strLiveStreamId = liveStreamId.getText().toString();
        if (!TextUtils.isEmpty(strLiveStreamId)) {
            peerConnectionClient.connectToStream(strLiveStreamId);
        }
        hideKeyboard(liveStreamId);
        localSurfaceViewRenderer.setVisibility(View.GONE);
        remoteSurfaceViewRenderer.setVisibility(View.VISIBLE);
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
