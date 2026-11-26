package com.example.footprint.data.model;

import java.util.List;

/** GET /api/guestbooks/{id} 응답. 필드 이름은 서버 응답과 같아야 합니다. */
public class GuestbookDetail {
    public Long guestbookId;
    public Long userId;
    public String nickname;
    public String content;
    public double latitude;
    public double longitude;
    public String visibility;
    public String locationName;
    public String createdAt;
    public String updatedAt;
    public List<String> photoUrls;
    public long likeCount;
    public boolean liked;
    public boolean mine;
    public boolean letter;            // 그 자리에서 열리는 편지
    public Long recipientId;
    public String recipientNickname;
    public String letterOpenedAt;
    public long footprintCount;       // 👣 그 자리에 와서 열어 본 사람 수 (작성자 제외)
    public long todayFootprintCount;  // 그중 오늘
}