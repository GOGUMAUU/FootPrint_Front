package com.example.footprint.data.model;

/** 👣 GET /api/maps/guestbooks/cluster/footprints 응답 */
public class Footprints {
    public long count;  // 다녀간 사람 수 (서로 다른 사람, 작성자 제외)
    public long today;  // 그중 오늘(한국 시간)
}
