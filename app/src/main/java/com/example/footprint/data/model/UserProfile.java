package com.example.footprint.data.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public final class UserProfile extends UserSummary {
    public String email;
    public boolean isPrivate, isSelf, blockedByMe;
    public int followerCount, followingCount, guestbookCount;
    public String outgoingFollowStatus, incomingFollowStatus;

    public static UserProfile fromApi(JsonObject json) {
        UserProfile profile = new Gson().fromJson(json, UserProfile.class);
        JsonObject relationship = json.getAsJsonObject("relationship");
        if (relationship == null) throw new JsonParseException("Missing profile relationship");
        profile.isSelf = relationship.get("isSelf").getAsBoolean();
        profile.blockedByMe = relationship.get("blockedByMe").getAsBoolean();
        profile.outgoingFollowStatus = relationship.get("outgoingFollowStatus").getAsString();
        profile.incomingFollowStatus = relationship.get("incomingFollowStatus").getAsString();
        if (!validStatus(profile.outgoingFollowStatus) || !validStatus(profile.incomingFollowStatus))
            throw new JsonParseException("Invalid profile relationship status");
        return profile;
    }

    private static boolean validStatus(String status) {
        return "NONE".equals(status) || "PENDING".equals(status) || "ACCEPTED".equals(status);
    }
}
