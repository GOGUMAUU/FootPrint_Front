package com.example.footprint.data.model;

import java.util.List;

public class ChallengeListResponse {

    public int LocalChallenges_totalCount;

    public List<ChallengeItem> challenges;

    public static class ChallengeItem {
        public Long challengeId;
        public String title;
        public String region;
        public String category;
        public String iconUrl;
        public boolean isCompleted;

        // 도전과제 달성 인원
        public long completedCount;

        // 도전과제 목표 좌표
        public double latitude;
        public double longitude;

        // 도전과제 생성자 ID
        public Long creatorId;

        // 도전과제 유형
        public String questType;
    }
}