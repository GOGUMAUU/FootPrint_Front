package com.example.footprint.data.model;

import com.google.gson.annotations.SerializedName;

public class Guestbook {
    @SerializedName("guestbookId")
    private Long id;

    private Long userId;
    private String content;      // 본문 전체
    private String title;        // 말풍선용 요약 (서버에서 30자로 자름)
    private String thumbnailUrl;
    private double latitude;
    private double longitude;
    private String visibility;
    private String locationName;
    private String createdAt;
    private String updatedAt;

    private Double distanceMeter;  // nearby에서만 값 있음
    private Boolean isAccessible;
    private Integer likeCount;
    private Boolean isClustered;
    private Integer clusterCount;  // 대표 글 포함 개수

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

    // 말풍선 텍스트. 클러스터면 "대표 글 외 N개"
    public String getShortText() {
        String base;
        if (title != null && !title.isEmpty()) base = title;
        else if (content != null && !content.isEmpty()) base = content;
        else if (locationName != null) base = locationName;
        else base = "방명록";

        if (isClustered() && getClusterCount() > 1) {
            if (base.length() > 8) base = base.substring(0, 8) + "…";
            return base + " 외 " + (getClusterCount() - 1) + "개";
        }
        return base;
    }
}