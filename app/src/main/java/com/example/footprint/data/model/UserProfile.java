package com.example.footprint.data.model;

public final class UserProfile extends UserSummary {
    public String email;
    public boolean isPrivate, isSelf, blockedByMe;
    public int followerCount, followingCount, guestbookCount;
    public String outgoingFollowStatus, incomingFollowStatus;
}
