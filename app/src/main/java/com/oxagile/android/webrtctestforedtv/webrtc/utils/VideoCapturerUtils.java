package com.oxagile.android.webrtctestforedtv.webrtc.utils;

import android.content.Context;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.VideoCapturer;

public class VideoCapturerUtils {
    private VideoCapturerUtils() {}

    public static VideoCapturer createVideoCapture(Context appContext, boolean fromFrontCamera) {
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
}
