package com.example.footprint.util;

import android.content.Context;
import android.content.SharedPreferences;

/** 지도 관련 기기별 취향 설정 */
public final class MapPrefs {
    private MapPrefs() {}

    private static final String FILE = "map_prefs";
    private static final String KEY_PICK = "bubble_pick";
    private static final String KEY_BOARD_SORT = "board_sort";

    /** 말풍선(묶음 대표)에 보일 글: recommended(기본) | popular | latest */
    public static final String[] PICK_VALUES = {"recommended", "popular", "latest"};
    public static final String[] PICK_LABELS = {"추천", "인기 글", "최신 글"};

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String pick(Context c) { return sp(c).getString(KEY_PICK, "recommended"); }
    public static void setPick(Context c, String v) { sp(c).edit().putString(KEY_PICK, v).apply(); }

    public static String pickLabel(Context c) {
        String v = pick(c);
        for (int i = 0; i < PICK_VALUES.length; i++) if (PICK_VALUES[i].equals(v)) return PICK_LABELS[i];
        return PICK_LABELS[0];
    }

    /** 게시판 마지막 정렬: latest(기본) | popular | distance */
    public static String boardSort(Context c) { return sp(c).getString(KEY_BOARD_SORT, "latest"); }
    public static void setBoardSort(Context c, String v) { sp(c).edit().putString(KEY_BOARD_SORT, v).apply(); }

    /** 마지막으로 알던 내 위치 (앱을 다시 켤 때 지도를 바로 그 자리로 띄우기 위함, 기기에만 저장) */
    public static void saveLastLocation(Context c, double lat, double lng) {
        sp(c).edit().putLong("last_lat", Double.doubleToRawLongBits(lat))
                .putLong("last_lng", Double.doubleToRawLongBits(lng)).apply();
    }

    @androidx.annotation.Nullable
    public static double[] lastLocation(Context c) {
        SharedPreferences p = sp(c);
        if (!p.contains("last_lat") || !p.contains("last_lng")) return null;
        return new double[]{Double.longBitsToDouble(p.getLong("last_lat", 0)), Double.longBitsToDouble(p.getLong("last_lng", 0))};
    }
}
