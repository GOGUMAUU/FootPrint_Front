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
}
