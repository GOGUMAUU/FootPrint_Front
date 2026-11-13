package com.example.footprint.data.model;

import java.util.List;

public class NearbyGuestbookResponse {
    private Double centerLatitude;
    private Double centerLongitude;
    private Integer totalCount;
    private List<Guestbook> guestbooks;

    public Double getCenterLatitude() { return centerLatitude; }
    public Double getCenterLongitude() { return centerLongitude; }
    public Integer getTotalCount() { return totalCount; }
    public List<Guestbook> getGuestbooks() { return guestbooks; }
}