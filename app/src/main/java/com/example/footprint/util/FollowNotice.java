package com.example.footprint.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.PageResponse;
import com.example.footprint.data.model.UserSummary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** 새 팔로워 알림: 빨간 점 표시용 상태 + 마이페이지 진입 시 메시지 */
public final class FollowNotice {
    private FollowNotice() {}

    private static final String PREF = "follow_notice";
    private static long myId = -1;
    private static List<UserSummary> pending = new ArrayList<>();

    public static boolean hasNew() { return !pending.isEmpty(); }

    /** 팔로워 목록을 받아 "아직 못 본 팔로워"를 갱신하고 done을 실행 */
    public static void refresh(Context ctx, Runnable done) {
        final Context app = ctx.getApplicationContext();
        ApiClient.get().getMyProfile().enqueue(new Callback<UserSummary>() {
            @Override public void onResponse(@NonNull Call<UserSummary> c, @NonNull Response<UserSummary> r) {
                if (!r.isSuccessful() || r.body() == null || r.body().userId == null) return;
                final long id = r.body().userId;
                ApiClient.get().getUserFollowers(id, 100).enqueue(new Callback<PageResponse<UserSummary>>() {
                    @Override public void onResponse(@NonNull Call<PageResponse<UserSummary>> c2,
                                                     @NonNull Response<PageResponse<UserSummary>> r2) {
                        if (!r2.isSuccessful() || r2.body() == null || r2.body().content == null) return;
                        apply(app, id, r2.body().content);
                        if (done != null) done.run();
                    }
                    @Override public void onFailure(@NonNull Call<PageResponse<UserSummary>> c2, @NonNull Throwable t) {}
                });
            }
            @Override public void onFailure(@NonNull Call<UserSummary> c, @NonNull Throwable t) {}
        });
    }

    private static void apply(Context app, long id, List<UserSummary> followers) {
        myId = id;
        SharedPreferences sp = app.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> seen = new HashSet<>(sp.getStringSet("seen_" + id, new HashSet<String>()));
        List<UserSummary> fresh = new ArrayList<>();
        if (!sp.getBoolean("init_" + id, false)) {
            // 처음엔 이미 있는 팔로워를 전부 "본 것"으로 처리
            for (UserSummary u : followers) if (u.userId != null) seen.add(String.valueOf(u.userId));
            sp.edit().putStringSet("seen_" + id, seen).putBoolean("init_" + id, true).apply();
        } else {
            for (UserSummary u : followers) {
                if (u.userId != null && !seen.contains(String.valueOf(u.userId))) fresh.add(u);
            }
        }
        pending = fresh;
    }

    /** 마이페이지에 들어왔을 때: 메시지를 띄우고 본 것으로 저장 → 빨간 점 사라짐 */
    public static void showAndClear(android.app.Activity act) {
        if (pending.isEmpty() || myId < 0) return;
        Context app = act.getApplicationContext();
        String first = pending.get(0).nickname == null ? "누군가" : pending.get(0).nickname;
        String msg = pending.size() == 1
                ? first + "님이 팔로우합니다!"
                : first + "님 외 " + (pending.size() - 1) + "명이 팔로우합니다!";

        android.view.View v = android.view.LayoutInflater.from(act)
                .inflate(com.example.footprint.R.layout.dialog_follow_notice, null);
        ((android.widget.TextView) v.findViewById(com.example.footprint.R.id.tvFollowNoticeMsg)).setText(msg);
        androidx.appcompat.app.AlertDialog dlg = new androidx.appcompat.app.AlertDialog.Builder(act)
                .setView(v).create();
        if (dlg.getWindow() != null) dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        v.findViewById(com.example.footprint.R.id.btnFollowNoticeOk).setOnClickListener(x -> dlg.dismiss());
        dlg.show();
        // 4초 뒤 자동으로 닫힘 (화면이 이미 닫혔으면 건드리지 않음)
        v.postDelayed(() -> {
            if (!act.isFinishing() && !act.isDestroyed() && dlg.isShowing()) dlg.dismiss();
        }, 4000);

        SharedPreferences sp = app.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> seen = new HashSet<>(sp.getStringSet("seen_" + myId, new HashSet<String>()));
        for (UserSummary u : pending) if (u.userId != null) seen.add(String.valueOf(u.userId));
        sp.edit().putStringSet("seen_" + myId, seen).apply();
        pending = new ArrayList<>();
    }
}