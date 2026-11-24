package com.example.footprint.data.model;

public class Challenge {
    public String id;
    public String title;
    public String description;
    public String regionName;
    public boolean locationVerified;
    public boolean isCompleted;
    // 도전과제를 달성한 전체 인원 수
    public long completedCount;

    // 도전과제 목표 위치
    public double latitude;
    public double longitude;

    // 현재 위치에서 도전과제까지의 거리 (미터)
    public float distanceMeters = Float.MAX_VALUE;
}
