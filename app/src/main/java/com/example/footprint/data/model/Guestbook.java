package com.example.footprint.data.model;

import com.google.gson.annotations.SerializedName;

public class Guestbook {
    @SerializedName("guestbookId")
    private Long id; // 또는 guestbookId로 직렬화

    private Long userId;
    private String content;
    private double latitude;
    private double longitude;
    private String visibility;
    private String locationName;
    private String createdAt;
    private String updatedAt;

    // 지도 줌 레벨 표시에 필요한 비즈니스 필드 (서버에서 넘겨주거나 계산)
    private boolean representative;

    // 기본 생성자
    public Guestbook() {}

    // Getter 메서드들
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getContent() { return content; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getVisibility() { return visibility; }
    public String getLocationName() { return locationName; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public boolean isRepresentative() { return representative; }

    // MainActivity에서 g.shortText 대신 사용할 보조 메서드 (content 반환)
    public String getShortText() {
        return content != null ? content : locationName;
    }
}