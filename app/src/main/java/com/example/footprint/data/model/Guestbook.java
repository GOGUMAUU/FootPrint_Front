package com.example.footprint.data.model;

import com.google.gson.annotations.SerializedName;

public class Guestbook {
    @SerializedName("guestbookId")
    private Long id;

    private Long userId;
    private String content;      // /bound 응답용 본문
    private String title;        // /nearby 응답용 요약 제목
    private String thumbnailUrl;
    private double latitude;
    private double longitude;
    private String visibility;
    private String locationName;
    private String createdAt;
    private String updatedAt;

    // /nearby 전용 필드
    private Double distanceMeter;
    private Boolean isAccessible;
    private Integer likeCount;
    private Boolean isClustered;
    private Integer clusterCount;

    private boolean representative;

    public Guestbook() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getContent() { return content; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getVisibility() { return visibility; }
    public String getLocationName() { return locationName; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public Double getDistanceMeter() { return distanceMeter; }
    public boolean isAccessible() { return isAccessible == null || isAccessible; }
    public int getLikeCount() { return likeCount != null ? likeCount : 0; }
    public boolean isClustered() { return Boolean.TRUE.equals(isClustered); }
    public int getClusterCount() { return clusterCount != null ? clusterCount : 1; }
    public boolean isRepresentative() { return representative || getLikeCount() >= 5; }

    // 말풍선 마커에 띄울 텍스트 자동 조합
    public String getShortText() {
        if (isClustered()) {
            return (locationName != null ? locationName : "이 지역") + " (+" + getClusterCount() + ")";
        }
        if (title != null && !title.isEmpty()) return title;
        if (content != null && !content.isEmpty()) return content;
        return locationName != null ? locationName : "방명록";
    }
}