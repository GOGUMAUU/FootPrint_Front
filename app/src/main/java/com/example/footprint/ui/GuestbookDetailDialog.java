package com.example.footprint.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.footprint.BuildConfig;
import com.example.footprint.R;
import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.GuestbookDetail;
import com.example.footprint.databinding.DialogGuestbookDetailBinding;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** 방명록 상세: 화면 가운데에 뜨는 엽서 카드. 방명록 번호와 내 현재 위치(없으면 null)를 받습니다. */
public class GuestbookDetailDialog extends DialogFragment {
    private static final String ARG_ID = "id";
    private static final String ARG_LAT = "lat";
    private static final String ARG_LNG = "lng";
    private static final float WIDTH_RATIO = 0.88f;

    private DialogGuestbookDetailBinding binding;
    private long guestbookId;
    private Double lat;
    private Double lng;
    private boolean liked;
    private long likeCount;
    private boolean busy;

    public static void open(FragmentManager fm, long id, @Nullable Double lat, @Nullable Double lng) {
        GuestbookDetailDialog f = new GuestbookDetailDialog();
        Bundle b = new Bundle();
        b.putLong(ARG_ID, id);
        if (lat != null && lng != null) {
            b.putDouble(ARG_LAT, lat);
            b.putDouble(ARG_LNG, lng);
        }
        f.setArguments(b);
        f.show(fm, "guestbook_detail");
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle saved) {
        Dialog d = super.onCreateDialog(saved);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        binding = DialogGuestbookDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window w = getDialog().getWindow();
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * WIDTH_RATIO),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        Bundle a = requireArguments();
        guestbookId = a.getLong(ARG_ID);
        if (a.containsKey(ARG_LAT)) {
            lat = a.getDouble(ARG_LAT);
            lng = a.getDouble(ARG_LNG);
        }
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnLike.setOnClickListener(v -> toggleLike());
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.btnEdit.setOnClickListener(v -> openEdit());
        load();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ---------- 불러오기 ----------
    private void load() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.tvError.setVisibility(View.GONE);
        binding.content.setVisibility(View.GONE);
        ApiClient.get().getGuestbookDetail(guestbookId, lat, lng).enqueue(new Callback<GuestbookDetail>() {
            @Override public void onResponse(@NonNull Call<GuestbookDetail> call, @NonNull Response<GuestbookDetail> res) {
                if (binding == null) return;
                if (res.isSuccessful() && res.body() != null) {
                    bind(res.body());
                    saveView();
                } else {
                    showError(errorMessage(res));
                }
            }
            @Override public void onFailure(@NonNull Call<GuestbookDetail> call, @NonNull Throwable t) {
                if (binding == null) return;
                showError("서버에 연결할 수 없어요. 인터넷과 서버 주소를 확인하세요.");
            }
        });
    }

    private void bind(GuestbookDetail d) {
        liked = d.liked;
        likeCount = d.likeCount;
        binding.progress.setVisibility(View.GONE);
        binding.tvError.setVisibility(View.GONE);
        binding.content.setVisibility(View.VISIBLE);
        binding.tvContent.setText(d.content);
        binding.tvTitle.setText(d.nickname == null || d.nickname.isEmpty() ? "방명록" : d.nickname + "의 방명록");
        boolean hasPlace = d.locationName != null && !d.locationName.isEmpty();
        binding.tvChipPlace.setVisibility(hasPlace ? View.VISIBLE : View.GONE);
        if (hasPlace) binding.tvChipPlace.setText(d.locationName);
        binding.tvChipDate.setText(formatDateShort(d.createdAt));
        binding.tvChipVis.setText(visibilityLabel(String.valueOf(d.visibility)));
        renderPhotos(d.photoUrls);
        binding.ownerRow.setVisibility(d.mine ? View.VISIBLE : View.GONE);
        binding.btnEdit.setTag(d);
        renderLike();
        renderFootprint(d);
        if (d.letter) styleAsLetter(d);
    }

    /** 👣 다녀간 발자국 (편지는 표시 안 함) */
    private void renderFootprint(GuestbookDetail d) {
        if (d.letter) { binding.tvFootprint.setVisibility(View.GONE); return; }
        String text = d.footprintCount == 0
                ? "👣 아직 다녀간 사람이 없어요"
                : "👣 " + d.footprintCount + "명이 다녀갔어요" + (d.todayFootprintCount > 0 ? " · 오늘 " + d.todayFootprintCount + "명" : "");
        binding.tvFootprint.setText(text);
        binding.tvFootprint.setVisibility(View.VISIBLE);
    }

    /** 💌 그 자리에서 열리는 편지: 같은 카드 구조에 연핑크 편지지 느낌 */
    private void styleAsLetter(GuestbookDetail d) {
        binding.getRoot().setBackgroundResource(R.drawable.bg_fp_card_letter);
        binding.tvTitle.setTextColor(0xFFC0607A);
        String from = d.nickname == null || d.nickname.isEmpty() ? "친구" : d.nickname;
        String to = d.recipientNickname == null || d.recipientNickname.isEmpty() ? "친구" : d.recipientNickname;
        binding.tvTitle.setText(d.mine ? "💌 " + to + "에게 보낸 편지" : "💌 " + from + "의 편지");
        binding.tvContent.setBackgroundResource(R.drawable.bg_fp_letter_paper);
        binding.tvContent.setTextColor(0xFF6B3A48);
        binding.tvChipVis.setBackgroundResource(R.drawable.bg_fp_chip_letter);
        binding.tvChipVis.setTextColor(0xFFB5546C);
        if (d.mine) {
            binding.tvChipVis.setText(d.letterOpenedAt == null ? "아직 안 열어봤어요" : to + "님이 열어봤어요");
        } else {
            binding.tvChipVis.setText("나에게 온 편지");
        }
        // 편지는 받는 사람·공개 범위를 바꿀 수 없어서 수정 대신 삭제만
        binding.btnEdit.setVisibility(View.GONE);
    }

    private void showError(String message) {
        binding.progress.setVisibility(View.GONE);
        binding.content.setVisibility(View.GONE);
        binding.tvError.setVisibility(View.VISIBLE);
        binding.tvError.setText(message);
    }

    /** 카드 안쪽 너비에 맞춰 4:3 사진을 그립니다. 2장 이상이면 다음 사진이 살짝 보입니다. */
    private void renderPhotos(List<String> urls) {
        binding.photoContainer.removeAllViews();
        if (urls == null || urls.isEmpty()) return;
        float d = getResources().getDisplayMetrics().density;
        int cardWidth = (int) (getResources().getDisplayMetrics().widthPixels * WIDTH_RATIO);
        int w = (int) (206 * d);
        int h = w;
        int radius = (int) (21 * d);
        for (String path : urls) {
            ImageView img = new ImageView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.setMarginEnd((int) (8 * d));
            img.setLayoutParams(lp);
            img.setBackgroundResource(R.drawable.bg_fp_photo_frame);
            int pad = (int) (3 * d);
            img.setPadding(pad, pad, pad, pad);
            Glide.with(this).load(com.example.footprint.util.AuthImage.of(requireContext(), fullUrl(path)))
                    .transform(new CenterCrop(), new RoundedCorners(radius))
                    .into(img);
            binding.photoContainer.addView(img);
        }
    }

    /** "/files/xxx.jpg" → "http://서버주소/files/xxx.jpg" */
    private String fullUrl(String path) {
        if (path == null) return "";
        if (path.startsWith("http")) return path;
        return BuildConfig.API_BASE_URL + (path.startsWith("/") ? path.substring(1) : path);
    }

    private String formatDate(String iso) {
        if (iso == null) return "";
        String s = iso.replace('T', ' ');
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    private String formatDateShort(String iso) {
        try {
            return Integer.parseInt(iso.substring(5, 7)) + "월 " + Integer.parseInt(iso.substring(8, 10)) + "일";
        } catch (Exception e) {
            return formatDate(iso);
        }
    }

    private String visibilityLabel(String v) {
        if ("PRIVATE".equals(v)) return "나만 보기";
        if ("FOLLOWERS_ONLY".equals(v)) return "팔로워 공개";
        return "전체 공개";
    }

    // ---------- 열람 기록 ----------
    private void saveView() {
        ApiClient.get().saveView(String.valueOf(guestbookId)).enqueue(new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> res) {}
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    // ---------- 수정 ----------
    private void openEdit() {
        Object tag = binding.btnEdit.getTag();
        if (!(tag instanceof GuestbookDetail)) return;
        GuestbookDetail d = (GuestbookDetail) tag;
        android.content.Intent it = new android.content.Intent(requireContext(), EditGuestbookActivity.class);
        it.putExtra(EditGuestbookActivity.EXTRA_ID, guestbookId);
        it.putExtra(EditGuestbookActivity.EXTRA_CONTENT, d.content);
        it.putExtra(EditGuestbookActivity.EXTRA_VISIBILITY, d.visibility);
        it.putExtra(EditGuestbookActivity.EXTRA_LOCATION_NAME, d.locationName);
        it.putStringArrayListExtra(EditGuestbookActivity.EXTRA_PHOTOS,
                d.photoUrls == null ? new java.util.ArrayList<String>() : new java.util.ArrayList<String>(d.photoUrls));
        startActivity(it);
        dismissAllowingStateLoss();
    }

    // ---------- 삭제 ----------
    private void confirmDelete() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete, null);
        androidx.appcompat.app.AlertDialog dlg = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(v).create();
        if (dlg.getWindow() != null) dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        v.findViewById(R.id.btnConfirmCancel).setOnClickListener(x -> dlg.dismiss());
        v.findViewById(R.id.btnConfirmDelete).setOnClickListener(x -> {
            dlg.dismiss();
            doDelete();
        });
        dlg.show();
    }

    private void doDelete() {
        if (busy) return;
        busy = true;
        binding.btnDelete.setEnabled(false);
        final android.content.Context appCtx = requireContext().getApplicationContext();
        ApiClient.get().deleteGuestbook(guestbookId).enqueue(new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> res) {
                busy = false;
                if (res.isSuccessful()) {
                    Toast.makeText(appCtx, "방명록을 삭제했어요.", Toast.LENGTH_SHORT).show();
                    if (isAdded()) dismissAllowingStateLoss();
                    return;
                }
                if (binding == null) return;
                binding.btnDelete.setEnabled(true);
                Toast.makeText(appCtx, errorMessage(res), Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {
                busy = false;
                if (binding == null) return;
                binding.btnDelete.setEnabled(true);
                Toast.makeText(appCtx, "서버에 연결할 수 없어요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------- 좋아요 ----------
    private void renderLike() {
        binding.btnLike.setText("좋아요 " + likeCount);
        binding.btnLike.setCompoundDrawablesRelativeWithIntrinsicBounds(
                liked ? R.drawable.ic_fp_heart : R.drawable.ic_fp_heart_outline, 0, 0, 0);
    }

    private void toggleLike() {
        if (busy) return;
        busy = true;
        binding.btnLike.setEnabled(false);
        Call<Void> call = liked
                ? ApiClient.get().unlike(String.valueOf(guestbookId))
                : ApiClient.get().like(String.valueOf(guestbookId));
        call.enqueue(new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> res) {
                busy = false;
                if (binding == null) return;
                binding.btnLike.setEnabled(true);
                if (res.isSuccessful()) {
                    liked = !liked;
                    likeCount += liked ? 1 : -1;
                    if (likeCount < 0) likeCount = 0;
                    renderLike();
                } else {
                    Toast.makeText(requireContext(), errorMessage(res), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {
                busy = false;
                if (binding == null) return;
                binding.btnLike.setEnabled(true);
                Toast.makeText(requireContext(), "서버에 연결할 수 없어요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String errorMessage(Response<?> res) {
        try {
            if (res.errorBody() != null) {
                String msg = new JSONObject(res.errorBody().string()).optString("message", "");
                if (!msg.isEmpty()) return msg;
            }
        } catch (Exception ignored) {
        }
        return "서버 오류가 났어요. (코드 " + res.code() + ")";
    }
}
