package com.oxagile.android.webrtctestforedtv.webrtc.constants;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@StringDef({MessageDataType.PRESENTER, MessageDataType.VIEWER})
@Retention(RetentionPolicy.SOURCE)
public @interface MessageDataType {
    String PRESENTER = "presenter";
    String VIEWER = "viewer";
    String ICE_CANDIDATE = "iceCandidate";
    String PRESENTER_RESPONSE = "presenterResponse";
    String VIEWER_RESPONSE = "viewerResponse";
    String STOP = "stop";
}
