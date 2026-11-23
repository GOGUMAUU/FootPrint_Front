package com.example.footprint.data.model;

import java.util.List;

public class BadgeListResponse {

    public int totalCount;
    public List<BadgeItem> badges;

    public static class BadgeItem {
        public Long questId;
        public String badgeName;
        public String badgeImageUrl;
        public String earnedAt;
    }
}
