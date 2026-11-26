package com.example.footprint.ui;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.footprint.R;
import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.LetterSummary;
import com.example.footprint.util.AppConfig;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** 💌 편지함: 받은 편지 / 보낸 편지. 본문은 그 자리에서 상세로만 열린다. */
public class LetterBoxSheet extends BottomSheetDialogFragment {

    /** 편지함이 닫힐 때 (안 읽은 편지 표시 갱신용) */
    public interface OnClosedListener { void onLetterBoxClosed(); }

    private static final String ARG_LAT = "lat";
    private static final String ARG_LNG = "lng";

    public static void show(FragmentManager fm, @Nullable Double lat, @Nullable Double lng) {
        if (fm.isStateSaved()) return;
        Bundle b = new Bundle();
        if (lat != null && lng != null) { b.putDouble(ARG_LAT, lat); b.putDouble(ARG_LNG, lng); }
        LetterBoxSheet f = new LetterBoxSheet();
        f.setArguments(b);
        f.show(fm, "letter_box");
    }

    @Nullable private Double lat, lng;
    private boolean inbox = true;
    @Nullable private Call<List<LetterSummary>> pending;
    private TextView tabInbox, tabSent, state;
    private final Adapter adapter = new Adapter();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        return inflater.inflate(R.layout.sheet_letter_box, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        Bundle a = requireArguments();
        if (a.containsKey(ARG_LAT)) { lat = a.getDouble(ARG_LAT); lng = a.getDouble(ARG_LNG); }
        tabInbox = v.findViewById(R.id.tabInbox);
        tabSent = v.findViewById(R.id.tabSent);
        state = v.findViewById(R.id.tvLetterState);
        RecyclerView rv = v.findViewById(R.id.rvLetters);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        tabInbox.setOnClickListener(x -> { if (!inbox) { inbox = true; renderTabs(); load(); } });
        tabSent.setOnClickListener(x -> { if (inbox) { inbox = false; renderTabs(); load(); } });
        renderTabs();
        load();
    }

    @Override
    public void onStart() {
        super.onStart();
        View sheet = getDialog() == null ? null : getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            sheet.setBackgroundColor(Color.TRANSPARENT);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            behavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.55));
            behavior.setMaxHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.8));
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() instanceof OnClosedListener) ((OnClosedListener) getActivity()).onLetterBoxClosed();
    }

    @Override
    public void onDestroyView() {
        if (pending != null) pending.cancel();
        super.onDestroyView();
    }

    private void renderTabs() {
        style(tabInbox, inbox);
        style(tabSent, !inbox);
    }

    private void style(TextView tab, boolean on) {
        tab.setBackgroundResource(on ? R.drawable.bg_fp_letter_chip_on : R.drawable.bg_fp_letter_chip);
        tab.setTextColor(on ? Color.WHITE : Color.parseColor("#8A4B5C"));
    }

    private void load() {
        if (pending != null) pending.cancel();
        showState("편지를 불러오는 중이에요…");
        pending = inbox ? ApiClient.get().getLetterInbox(lat, lng) : ApiClient.get().getLetterSent();
        pending.enqueue(new Callback<List<LetterSummary>>() {
            @Override public void onResponse(@NonNull Call<List<LetterSummary>> c, @NonNull Response<List<LetterSummary>> r) {
                if (!isAdded() || c.isCanceled()) return;
                if (!r.isSuccessful() || r.body() == null) { showState("편지함을 불러오지 못했어요."); return; }
                adapter.submit(r.body());
                if (r.body().isEmpty()) showState(inbox ? "아직 도착한 편지가 없어요." : "아직 보낸 편지가 없어요.\n방명록을 남길 때 '친구에게 편지로 남기기'를 눌러 보세요.");
                else state.setVisibility(View.GONE);
            }
            @Override public void onFailure(@NonNull Call<List<LetterSummary>> c, @NonNull Throwable t) {
                if (!isAdded() || c.isCanceled()) return;
                showState("서버에 연결할 수 없어요.");
            }
        });
    }

    private void showState(String msg) {
        adapter.submit(new ArrayList<>());
        state.setText(msg);
        state.setVisibility(View.VISIBLE);
    }

    private boolean canOpen(LetterSummary l) {
        if (!inbox || l.opened) return true; // 보낸 편지, 이미 연 편지는 언제든 다시 볼 수 있음
        return l.distanceMeter != null && l.distanceMeter <= AppConfig.GUESTBOOK_UNLOCK_RADIUS_METERS;
    }

    private void open(LetterSummary l) {
        if (!canOpen(l)) {
            String more = l.distanceMeter == null ? ""
                    : String.format(Locale.getDefault(), " (%s 더 가까이)", meters(l.distanceMeter - AppConfig.GUESTBOOK_UNLOCK_RADIUS_METERS));
            Toast.makeText(requireContext(), "편지가 남겨진 자리에 가면 열 수 있어요." + more, Toast.LENGTH_SHORT).show();
            return;
        }
        GuestbookDetailDialog.open(getParentFragmentManager(), l.guestbookId, lat, lng);
    }

    private static String meters(double m) {
        m = Math.max(0, m);
        return m < 1000 ? String.format(Locale.getDefault(), "%.0fm", m) : String.format(Locale.getDefault(), "%.1fkm", m / 1000);
    }

    /** "2026-11-26T10:20:30" → "11.26" */
    private static String shortDate(@Nullable String iso) {
        if (iso == null || iso.length() < 10) return "";
        return iso.substring(5, 7) + "." + iso.substring(8, 10);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final List<LetterSummary> items = new ArrayList<>();

        void submit(List<LetterSummary> list) { items.clear(); items.addAll(list); notifyDataSetChanged(); }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_letter, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            LetterSummary l = items.get(position);
            h.who.setText(inbox ? "From. " + nz(l.senderNickname) : "To. " + nz(l.recipientNickname));
            h.date.setText(shortDate(l.createdAt));
            h.place.setText("📍 " + (l.locationName == null || l.locationName.isEmpty() ? "이름 없는 장소" : l.locationName));

            if (inbox) {
                if (l.opened) {
                    status(h, "열어본 편지 · " + shortDate(l.openedAt), R.drawable.bg_fp_chip_date, "#7A4A0E");
                } else if (canOpen(l)) {
                    status(h, "지금 열 수 있어요!", R.drawable.bg_fp_chip_vis, "#2F5E35");
                } else if (l.distanceMeter != null) {
                    status(h, "그 자리에 가면 열려요 · " + meters(l.distanceMeter), R.drawable.bg_fp_chip_letter, "#B5546C");
                } else {
                    status(h, "그 자리에 가면 열려요", R.drawable.bg_fp_chip_letter, "#B5546C");
                }
            } else {
                if (l.opened) status(h, nz(l.recipientNickname) + "님이 열어봤어요 · " + shortDate(l.openedAt), R.drawable.bg_fp_chip_vis, "#2F5E35");
                else status(h, "아직 열어보지 않았어요", R.drawable.bg_fp_chip_letter, "#B5546C");
            }
            h.itemView.setAlpha(inbox && !canOpen(l) ? 0.75f : 1f);
            h.itemView.setOnClickListener(x -> open(l));
        }

        private void status(Holder h, String text, int bg, String color) {
            h.status.setText(text);
            h.status.setBackgroundResource(bg);
            h.status.setTextColor(Color.parseColor(color));
        }

        private String nz(@Nullable String s) { return s == null || s.isEmpty() ? "친구" : s; }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final TextView who, date, place, status;
            Holder(View v) {
                super(v);
                who = v.findViewById(R.id.tvLetterWho);
                date = v.findViewById(R.id.tvLetterDate);
                place = v.findViewById(R.id.tvLetterPlace);
                status = v.findViewById(R.id.tvLetterStatus);
            }
        }
    }
}
