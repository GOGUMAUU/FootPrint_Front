package com.example.footprint.util;

public final class AppConfig {
    private AppConfig() {}

    // 서버 없이 테스트할 때 true (MockGuestbooks 사용)
    public static final boolean USE_MOCK_DATA = false;

    // Product assumptions: easy to change before backend contracts are frozen.
    // 서버 MapService.NEARBY_RADIUS_KM 과 같은 값이어야 함 (서버가 내 위치 기준 1km만 내려줌)
    public static final double NEARBY_MAX_RADIUS_METERS = 1000.0;
    public static final double GUESTBOOK_UNLOCK_RADIUS_METERS = 100.0;
    public static final double CREATE_ALLOWED_RADIUS_METERS = 150.0;
    public static final int SHORT_TEXT_MAX = 120;

    // 주변 모드 줌 범위 (15 ≈ 반경 1km 정도가 한 화면). 에뮬레이터에서 보고 조절
    public static final int NEARBY_MIN_ZOOM = 15;
    public static final int NEARBY_DEFAULT_ZOOM = 16;
    public static final int NEARBY_MAX_ZOOM = 19;

    // 서버 MapService.CLUSTER_MAX_ZOOM 과 같은 값 (이 줌부터 낱개)
    public static final int CLUSTER_MAX_ZOOM = 20;

    // Zoom policy: lower zoom = representative/popular records only.
    public static final double ZOOM_COUNTRY = 6.5;
    public static final double ZOOM_REGION = 10.0;
    public static final double ZOOM_DETAIL = 14.0;
}