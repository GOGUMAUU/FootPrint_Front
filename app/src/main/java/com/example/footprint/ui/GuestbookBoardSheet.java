package com.example.footprint.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.footprint.R;
import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.Guestbook;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 게시판: 지도에서 한 자리에 묶인(클러스터) 방명록들을 목록으로 보여준다.
 * 지도 응답의 clusterIds 를 받아 서버(/api/maps/guestbooks/cluster)에 목록을 요청한다.
 */
public class GuestbookBoardSheet extends BottomSheetDialogFragment {

    private static final String ARG_IDS = "ids";
    private static final String ARG_LAT = "lat";
    private static final String ARG_LNG = "lng";
    private static final String ARG_TITLE = "title";
    private static final String ARG_STRICT = "strict";

    /**
     * @param lat,lng       내 위치 (있으면 거리·열람 가능 여부 계산, 거리순 정렬 가능)
     * @param strictUnlock  true면 열람 불가(거리 밖) 글은 상세를 열지 않는다 (주변 모드)
     */
    public static void show(FragmentManager fm, List<Long> ids, @Nullable Double lat, @Nullable Double lng,
                            @Nullable String title, boolean strictUnlock) {
        if (ids == null || ids.isEmpty() || fm.isStateSaved()) return;
        long[] arr = new long[ids.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = ids.get(i);
        Bundle b = new Bundle();
        b.putLongArray(ARG_IDS, arr);
        if (lat != null && lng != null) { b.putDouble(ARG_LAT, lat); b.putDouble(ARG_LNG, lng); }
        b.putString(ARG_TITLE, title);
        b.putBoolean(ARG_STRICT, strictUnlock);
        GuestbookBoardSheet f = new GuestbookBoardSheet();
        f.setArguments(b);
        f.show(fm, "guestbook_board");
    }

    private static final String[][] SORTS = {{"latest", "최신순"}, {"popular", "인기순"}, {"distance", "가까운순"}};

    private String idsCsv;
    @Nullable private Double lat, lng;
    private boolean strict;
    private String sort = "latest";
    @Nullable private Call<List<Guestbook>> pending;

    private TextView count, state;
    private LinearLayout sortBar;
    private final BoardAdapter adapter = new BoardAdapter();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        return inflater.inflate(R.layout.sheet_guestbook_board, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        Bundle a = requireArguments();
        long[] ids = a.getLongArray(ARG_IDS);
        StringBuilder sb = new StringBuilder();
        if (ids != null) for (long id : ids) { if (sb.length() > 0) sb.append(','); sb.append(id); }
        idsCsv = sb.toString();
        if (a.containsKey(ARG_LAT)) { lat = a.getDouble(ARG_LAT); lng = a.getDouble(ARG_LNG); }
        strict = a.getBoolean(ARG_STRICT);
        // 마지막으로 고른 정렬을 기억 (위치가 없으면 가까운순은 못 쓰니 최신순)
        sort = com.example.footprint.util.MapPrefs.boardSort(requireContext());
        if ("distance".equals(sort) && lat == null) sort = "latest";
        String title = a.getString(ARG_TITLE);
        if (title != null && !title.trim().isEmpty()) ((TextView) v.findViewById(R.id.tvBoardTitle)).setText(title);

        count = v.findViewById(R.id.tvBoardCount);
        state = v.findViewById(R.id.tvBoardState);
        sortBar = v.findViewById(R.id.boardSortBar);
        RecyclerView rv = v.findViewById(R.id.rvBoard);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        buildSortBar();
        load();
    }

    @Override
    public void onStart() {
        super.onStart();
        // 기본 흰 배경을 없애서 위쪽 둥근 카드 모양이 보이게, 화면 70%까지 펼침
        View sheet = getDialog() == null ? null : getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            sheet.setBackgroundColor(Color.TRANSPARENT);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            behavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.55));
            behavior.setMaxHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.8));
        }
    }

    @Override
    public void onDestroyView() {
        if (pending != null) pending.cancel();
        super.onDestroyView();
    }

    // ───────────────────────── 정렬 탭 ─────────────────────────

    private void buildSortBar() {
        sortBar.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        for (String[] opt : SORTS) {
            if ("distance".equals(opt[0]) && lat == null) continue; // 내 위치 없으면 거리순 숨김
            TextView chip = new TextView(requireContext());
            chip.setText(opt[1]);
            chip.setTextSize(14);
            chip.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.jua));
            chip.setPadding((int) (14 * d), (int) (6 * d), (int) (14 * d), (int) (6 * d));
            boolean on = opt[0].equals(sort);
            chip.setBackgroundResource(on ? R.drawable.bg_fp_chip_selected_static : R.drawable.bg_fp_chip_normal_static);
            chip.setTextColor(on ? Color.parseColor("#FFFDF4") : Color.parseColor("#5E4128"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int) (6 * d));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(x -> {
                if (opt[0].equals(sort)) return;
                sort = opt[0];
                com.example.footprint.util.MapPrefs.setBoardSort(requireContext(), sort);
                buildSortBar();
                load();
            });
            sortBar.addView(chip);
        }
    }

    // ───────────────────────── 목록 조회 ─────────────────────────

    private void load() {
        if (pending != null) pending.cancel();
        showState("방명록을 불러오는 중이에요…");
        pending = ApiClient.get().getClusterGuestbooks(idsCsv, lat, lng, sort);
        pending.enqueue(new Callback<List<Guestbook>>() {
            @Override public void onResponse(@NonNull Call<List<Guestbook>> c, @NonNull Response<List<Guestbook>> r) {
                if (!isAdded() || c.isCanceled()) return;
                if (!r.isSuccessful() || r.body() == null) { showState("목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요."); return; }
                List<Guestbook> list = r.body();
                count.setText(list.size() + "개의 방명록");
                loadFootprints(list.size());
                adapter.submit(list);
                if (list.isEmpty()) showState("볼 수 있는 방명록이 없어요."); else state.setVisibility(View.GONE);
            }
            @Override public void onFailure(@NonNull Call<List<Guestbook>> c, @NonNull Throwable t) {
                if (!isAdded() || c.isCanceled()) return;
                showState("서버에 연결할 수 없어요.");
            }
        });
    }

    /** 👣 "N개의 방명록 · 23명이 다녀간 자리" */
    private void loadFootprints(int postCount) {
        ApiClient.get().getClusterFootprints(idsCsv).enqueue(new Callback<com.example.footprint.data.model.Footprints>() {
            @Override public void onResponse(@NonNull Call<com.example.footprint.data.model.Footprints> c,
                                             @NonNull Response<com.example.footprint.data.model.Footprints> r) {
                if (!isAdded() || !r.isSuccessful() || r.body() == null || r.body().count <= 0) return;
                count.setText(postCount + "개의 방명록 · 👣 " + r.body().count + "명이 다녀간 자리"
                        + (r.body().today > 0 ? " (오늘 " + r.body().today + "명)" : ""));
            }
            @Override public void onFailure(@NonNull Call<com.example.footprint.data.model.Footprints> c, @NonNull Throwable t) {}
        });
    }

    private void showState(String msg) {
        adapter.submit(new ArrayList<>());
        state.setText(msg);
        state.setVisibility(View.VISIBLE);
    }

    private void open(Guestbook g) {
        if (strict && !g.isAccessible()) {
            Double dist = g.getDistanceMeter();
            String more = dist == null ? "" : String.format(Locale.getDefault(), " (%.0fm 더 가까이)",
                    Math.max(0, dist - com.example.footprint.util.AppConfig.GUESTBOOK_UNLOCK_RADIUS_METERS));
            Toast.makeText(requireContext(), "직접 가까이 가야 열어 볼 수 있어요." + more, Toast.LENGTH_SHORT).show();
            return;
        }
        GuestbookDetailDialog.open(getParentFragmentManager(), g.getId(), lat, lng);
    }

    // ───────────────────────── 어댑터 ─────────────────────────

    private class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.Holder> {
        private final List<Guestbook> items = new ArrayList<>();

        void submit(List<Guestbook> list) { items.clear(); items.addAll(list); notifyDataSetChanged(); }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guestbook_board, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Guestbook g = items.get(position);
            h.nickname.setText(g.getNickname() == null || g.getNickname().isEmpty() ? "익명" : g.getNickname());
            h.date.setText(shortDate(g.getCreatedAt()));
            h.likes.setText("좋아요 " + g.getLikeCount());

            boolean locked = strict && !g.isAccessible();
            Double dist = g.getDistanceMeter();
            if (locked) {
                h.content.setText("가까이 가면 내용을 볼 수 있어요");
                h.distance.setText(dist == null ? "잠겨 있어요" : String.format(Locale.getDefault(), "%.0fm 남음",
                        Math.max(0, dist - com.example.footprint.util.AppConfig.GUESTBOOK_UNLOCK_RADIUS_METERS)));
                h.distance.setBackgroundResource(R.drawable.bg_fp_consent_sky);
                h.distance.setTextColor(Color.parseColor("#1F4F6B"));
                h.distance.setVisibility(View.VISIBLE);
                h.itemView.setAlpha(0.6f);
            } else {
                h.content.setText(g.getContent() == null ? "" : g.getContent());
                if (dist != null) {
                    h.distance.setText(dist < 1000 ? String.format(Locale.getDefault(), "%.0fm", dist)
                            : String.format(Locale.getDefault(), "%.1fkm", dist / 1000));
                    h.distance.setBackgroundResource(R.drawable.bg_fp_chip_vis);
                    h.distance.setTextColor(Color.parseColor("#2F5E35"));
                    h.distance.setVisibility(View.VISIBLE);
                } else {
                    h.distance.setVisibility(View.GONE);
                }
                h.itemView.setAlpha(1f);
            }
            h.itemView.setOnClickListener(x -> open(g));
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final TextView nickname, date, content, likes, distance;
            Holder(View v) {
                super(v);
                nickname = v.findViewById(R.id.tvItemNickname);
                date = v.findViewById(R.id.tvItemDate);
                content = v.findViewById(R.id.tvItemContent);
                likes = v.findViewById(R.id.tvItemLikes);
                distance = v.findViewById(R.id.tvItemDistance);
            }
        }
    }

    /** "2026-11-26T10:20:30.123" → "11.26" */
    private static String shortDate(@Nullable String iso) {
        if (iso == null || iso.length() < 10) return "";
        return iso.substring(5, 7) + "." + iso.substring(8, 10);
    }
}
