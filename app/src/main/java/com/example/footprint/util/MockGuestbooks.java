package com.example.footprint.util;

import android.location.Location;

import com.example.footprint.data.model.Guestbook;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 서버 없이 지도 테스트용 가짜 방명록.
 * AppConfig.USE_MOCK_DATA = true 일 때 MainActivity가 API 대신 이걸 씀.
 *
 * - 성신여대 주변 1.2km에 약 40개 + GPX 테스트 경로 위 5개
 * - 출발점(성신여대) 100m 안에 3개 → 바로 열람 가능 테스트
 * - 서울/전국 주요 지역에 인기 방명록 10개 → 팔로잉/내 지도 줌아웃 테스트
 * - 줌 17 미만이면 서버와 같은 격자 방식으로 클러스터링, 대표 글은 sort 기준 1등
 */
public final class MockGuestbooks {
    private MockGuestbooks() {}

    public static final long MY_USER_ID = 1L;
    private static final double BASE_CELL_DEG = 0.001; // 서버와 동일
    private static final int MAX_ITEMS = 50;

    private static final Gson GSON = new Gson();
    private static final List<Spec> SPECS = buildSpecs();

    private static final class Spec {
        final long id, userId;
        final String title, place;
        final double lat, lng;
        final int likes;

        Spec(long id, long userId, String title, String place, double lat, double lng, int likes) {
            this.id = id; this.userId = userId; this.title = title; this.place = place;
            this.lat = lat; this.lng = lng; this.likes = likes;
        }
    }

    // ───────────────────────── 조회 (API 대신) ─────────────────────────

    /** /nearby 흉내: 내 위치 1km 안 + 거리·열람가능 + 클러스터링. sort = distance | popular */
    public static List<Guestbook> nearby(double myLat, double myLng, int zoom, String sort) {
        List<Item> items = new ArrayList<>();
        float[] d = new float[1];
        for (Spec s : SPECS) {
            Location.distanceBetween(myLat, myLng, s.lat, s.lng, d);
            if (d[0] > AppConfig.NEARBY_MAX_RADIUS_METERS) continue;
            items.add(new Item(s, (double) d[0]));
        }
        return clusterAndPick(items, zoom, "popular".equals(sort) ? "popular" : "distance");
    }

    /** /bound 흉내: 화면 영역 안. scope = "me" 면 내 글만. sort = popular | latest */
    public static List<Guestbook> inBounds(String scope, double[] b, int zoom, String sort) {
        List<Item> items = new ArrayList<>();
        for (Spec s : SPECS) {
            if ("me".equals(scope) && s.userId != MY_USER_ID) continue;
            if (s.lat < b[0] || s.lat > b[2] || s.lng < b[1] || s.lng > b[3]) continue;
            items.add(new Item(s, null));
        }
        return clusterAndPick(items, zoom, "popular".equals(sort) ? "popular" : "latest");
    }

    // ───────────────────────── 클러스터링 (서버 MapService와 같은 방식) ─────────────────────────

    private static final class Item {
        final Spec s;
        final Double distance;
        Item(Spec s, Double distance) { this.s = s; this.distance = distance; }
    }

    private static Comparator<Item> comparatorFor(String sort) {
        Comparator<Item> byDistance = Comparator.comparing(
                (Item i) -> i.distance, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Item> byLikes = Comparator.comparingInt((Item i) -> i.s.likes).reversed();
        Comparator<Item> byLatest = Comparator.comparingLong((Item i) -> i.s.id).reversed(); // id 클수록 최신 취급
        switch (sort) {
            case "popular": return byLikes.thenComparing(byDistance);
            case "latest": return byLatest;
            default: return byDistance.thenComparing(byLikes);
        }
    }

    private static List<Guestbook> clusterAndPick(List<Item> items, int zoom, String sort) {
        Comparator<Item> cmp = comparatorFor(sort);
        List<Guestbook> out = new ArrayList<>();

        if (zoom >= AppConfig.CLUSTER_MAX_ZOOM) {
            items.sort(cmp);
            for (int i = 0; i < Math.min(items.size(), MAX_ITEMS); i++) out.add(toGuestbook(items.get(i), 1));
            return out;
        }

        double cell = BASE_CELL_DEG * Math.pow(2, (AppConfig.CLUSTER_MAX_ZOOM - 1) - zoom);
        Map<String, List<Item>> groups = new LinkedHashMap<>();
        for (Item it : items) {
            String key = (long) Math.floor(it.s.lat / cell) + ":" + (long) Math.floor(it.s.lng / cell);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(it);
        }

        List<Item> reps = new ArrayList<>();
        Map<Item, Integer> sizes = new HashMap<>();
        for (List<Item> group : groups.values()) {
            group.sort(cmp);
            reps.add(group.get(0));          // 정렬 기준 1등이 대표
            sizes.put(group.get(0), group.size());
        }
        reps.sort(cmp);
        for (int i = 0; i < Math.min(reps.size(), MAX_ITEMS); i++) {
            out.add(toGuestbook(reps.get(i), sizes.get(reps.get(i))));
        }
        return out;
    }

    // ───────────────────────── 데이터 생성 ─────────────────────────

    private static Guestbook toGuestbook(Item it, int clusterCount) {
        Spec s = it.s;
        JsonObject o = new JsonObject();
        o.addProperty("guestbookId", s.id);
        o.addProperty("userId", s.userId);
        o.addProperty("title", s.title);
        o.addProperty("content", s.title + " — 목데이터 본문입니다.");
        o.addProperty("locationName", s.place);
        o.addProperty("latitude", s.lat);
        o.addProperty("longitude", s.lng);
        o.addProperty("likeCount", s.likes);
        o.addProperty("isAccessible", it.distance == null || it.distance <= AppConfig.GUESTBOOK_UNLOCK_RADIUS_METERS);
        o.addProperty("isClustered", clusterCount > 1);
        o.addProperty("clusterCount", clusterCount);
        o.addProperty("visibility", "PUBLIC");
        o.addProperty("createdAt", "2026-11-13T12:00:00");
        if (it.distance != null) o.addProperty("distanceMeter", Math.round(it.distance * 10.0) / 10.0);
        return GSON.fromJson(o, Guestbook.class);
    }

    private static List<Spec> buildSpecs() {
        List<Spec> list = new ArrayList<>();
        Random r = new Random(42); // 고정 시드 → 실행할 때마다 같은 위치
        String[] titles = {
                "여기 붕어빵 맛집", "벤치에서 쉬다 감", "노을 진짜 예쁨", "고양이 있음 🐱", "과제하다 탈출",
                "첫 데이트 장소", "비 오는 날 산책", "여기 카페 조용함", "버스킹 하던 곳", "시험 끝!",
                "떡볶이 최고", "벚꽃 명소", "친구랑 사진 찍음", "야경 좋다", "운동하러 옴"
        };
        long id = 1;

        // 1) 출발점(성신여대) 100m 안 → 바로 열람 가능
        double sLat = 37.5913, sLng = 127.0221;
        list.add(new Spec(id++, MY_USER_ID, "학교 정문 도착!", "성신여대 정문", sLat + 0.0002, sLng + 0.0001, 12));
        list.add(new Spec(id++, 2, "수정관 앞 벤치", "성신여대", sLat - 0.0003, sLng - 0.0002, 3));
        list.add(new Spec(id++, 3, "학식 오늘 맛있음", "성신여대 학생식당", sLat + 0.0004, sLng - 0.0004, 7));

        // 2) GPX 경로 위 (걷다 보면 하나씩 열림)
        double[][] route = {
                {37.5926, 127.0164, 0}, {37.5905, 127.0113, 0}, {37.5884, 127.0062, 0},
                {37.5853, 127.0040, 0}, {37.5822, 127.0019, 0}
        };
        String[] routePlaces = {"성신여대입구역", "돈암동", "한성대입구역", "혜화동", "혜화역"};
        for (int i = 0; i < route.length; i++) {
            list.add(new Spec(id++, i % 2 == 0 ? MY_USER_ID : 4, titles[i] , routePlaces[i],
                    route[i][0] + 0.0003, route[i][1] + 0.0002, 2 + i * 2));
        }

        // 3) 성신여대 반경 1.2km 안 랜덤 40개 (1km 경계 테스트용으로 살짝 넘김)
        for (int i = 0; i < 40; i++) {
            double dist = 1200 * Math.sqrt(r.nextDouble());
            double angle = r.nextDouble() * 2 * Math.PI;
            double lat = sLat + (dist * Math.cos(angle)) / 111_320.0;
            double lng = sLng + (dist * Math.sin(angle)) / (111_320.0 * Math.cos(Math.toRadians(sLat)));
            long userId = (i % 4 == 0) ? MY_USER_ID : 2 + (i % 4);
            list.add(new Spec(id++, userId, titles[i % titles.length], "성북구 어딘가", lat, lng, r.nextInt(15)));
        }

        // 4) 전국 인기 방명록 (줌아웃 / 팔로잉·내 지도 테스트)
        Object[][] far = {
                {"강남역 퇴근길", "강남역", 37.4979, 127.0276}, {"홍대 공연 봄", "홍대입구", 37.5563, 126.9220},
                {"롯데타워 전망대", "잠실", 37.5126, 127.1025}, {"해운대 바다!", "해운대", 35.1587, 129.1604},
                {"동성로 쇼핑", "대구 동성로", 35.8690, 128.5960}, {"양림동 골목", "광주 양림동", 35.1395, 126.9150},
                {"제주 오름 등반", "제주시", 33.4996, 126.5312}, {"한옥마을 산책", "전주 한옥마을", 35.8150, 127.1530},
                {"안목해변 커피", "강릉", 37.7720, 128.9470}, {"차이나타운 짜장면", "인천", 37.4755, 126.6180}
        };
        for (int i = 0; i < far.length; i++) {
            list.add(new Spec(id++, i % 3 == 0 ? MY_USER_ID : 5, (String) far[i][0], (String) far[i][1],
                    (double) far[i][2], (double) far[i][3], 20 + i * 3));
        }
        return list;
    }
}