package com.oxagile.android.webrtctestforedtv.webrtc.model;

import com.google.gson.annotations.SerializedName;
import com.oxagile.android.webrtctestforedtv.webrtc.constants.MessageDataType;

public class Message {

    @SerializedName("id")
    private String type;
    //MessageDataType.PRESENTER or MessageDataType.VIEWER
    @SerializedName("sdpOffer")
    private String sdpOffer;
    @SerializedName("userId")
    private String userId;
    @SerializedName("liveStreamId")
    private String liveStreamId;
    //MessageDataType.PRESENTER_RESPONSE or MessageDataType.VIEWER_RESPONSE
    @SerializedName("sdpAnswer")
    private String sdpAnswer;
    //MessageDataType.ICE_CANDIDATE
    @SerializedName("candidate")
    private Candidate candidate;

    public Message() {
    }

    public Message(@MessageDataType String type, String sdpOffer, String userId, String liveStreamId) {
        this.type = type;
        this.sdpOffer = sdpOffer;
        this.userId = userId;
        this.liveStreamId = liveStreamId;
    }

    public Message(Candidate candidate) {
        this.type = MessageDataType.ICE_CANDIDATE;
        this.candidate = candidate;
    }

    public String getType() {
        return type;
    }

    public String getSdpOffer() {
        return sdpOffer;
    }

    public String getUserId() {
        return userId;
    }

    public String getLiveStreamId() {
        return liveStreamId;
    }

    public String getSdpAnswer() {
        return sdpAnswer;
    }

    public Candidate getCandidate() {
        return candidate;
    }
}
