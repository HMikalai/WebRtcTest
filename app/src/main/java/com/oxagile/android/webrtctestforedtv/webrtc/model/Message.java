package com.oxagile.android.webrtctestforedtv.webrtc.model;

import com.google.gson.annotations.SerializedName;
import com.oxagile.android.webrtctestforedtv.webrtc.constants.MessageDataType;
import com.oxagile.android.webrtctestforedtv.webrtc.constants.Role;

public class Message {
    //MessageDataType:
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

    //MessageDataType.ICE_CANDIDATE_FOR_SEND or MessageDataType.ICE_CANDIDATE_FOR_RECEIVE
    @SerializedName("candidate")
    private Candidate candidate;

    public Message() {
    }

    public Message(@Role String type, String sdpOffer, String userId, String liveStreamId) {
        this.type = type;
        this.sdpOffer = sdpOffer;
        this.userId = userId;
        this.liveStreamId = liveStreamId;
    }

    public Message(Candidate candidate) {
        this.type = MessageDataType.ICE_CANDIDATE_FOR_SEND;
        this.candidate = candidate;
    }

    public String getType() {
        return type;
    }

    public String getSdpAnswer() {
        return sdpAnswer;
    }

    public Candidate getCandidate() {
        return candidate;
    }
}
