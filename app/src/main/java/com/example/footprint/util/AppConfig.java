package com.example.footprint.util;

public final class AppConfig {
    private AppConfig() {}

    // Product assumptions: easy to change before backend contracts are frozen.
    public static final double NEARBY_MAX_RADIUS_METERS = 5000.0;
    public static final double GUESTBOOK_UNLOCK_RADIUS_METERS = 100.0;
    public static final double CREATE_ALLOWED_RADIUS_METERS = 150.0;
    public static final int SHORT_TEXT_MAX = 120;

    // Zoom policy: lower zoom = representative/popular records only.
    public static final double ZOOM_COUNTRY = 6.5;
    public static final double ZOOM_REGION = 10.0;
    public static final double ZOOM_DETAIL = 14.0;
}
