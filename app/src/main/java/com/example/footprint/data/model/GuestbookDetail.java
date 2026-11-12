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
}