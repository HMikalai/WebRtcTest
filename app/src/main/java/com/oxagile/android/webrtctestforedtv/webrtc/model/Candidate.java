package com.oxagile.android.webrtctestforedtv.webrtc.model;

import com.google.gson.annotations.SerializedName;

public class Candidate {

    @SerializedName("candidate")
    private String candidate;
    @SerializedName("sdpMid")
    private String sdpMid;
    @SerializedName("sdpMLineIndex")
    private Integer sdpMLineIndex;

    public Candidate() {
    }

    public Candidate(String candidate, String sdpMid, Integer sdpMLineIndex) {
        this.candidate = candidate;
        this.sdpMid = sdpMid;
        this.sdpMLineIndex = sdpMLineIndex;
    }

    public String getCandidate() {
        return candidate;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public Integer getSdpMLineIndex() {
        return sdpMLineIndex;
    }
}
