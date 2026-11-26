package com.example.footprint.data.model;

/** GET /api/letters/inbox, /api/letters/sent 응답 항목 (본문 없음 — 그 자리에서 상세로만 열람) */
public class LetterSummary {
    public Long guestbookId;
    public Long senderId;
    public String senderNickname;
    public Long recipientId;
    public String recipientNickname;
    public String locationName;
    public double latitude;
    public double longitude;
    public String createdAt;
    public boolean opened;
    public String openedAt;
    public Double distanceMeter; // 받은 편지함에서 내 위치를 보냈을 때만

    public static class UnreadCount { public long count; }
}
