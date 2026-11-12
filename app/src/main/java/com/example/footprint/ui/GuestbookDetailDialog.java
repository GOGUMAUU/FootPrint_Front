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
        binding.btnEdit.setOnClickListener(v ->
                Toast.makeText(requireContext(), "수정 화면은 다음 단계에서 연결돼요.", Toast.LENGTH_SHORT).show());
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
        String meta = "- " + (d.nickname == null ? "" : d.nickname) + " · " + formatDate(d.createdAt);
        if (d.locationName != null && !d.locationName.isEmpty()) meta += " · " + d.locationName;
        binding.tvMeta.setText(meta);
        renderPhotos(d.photoUrls);
        binding.ownerRow.setVisibility(d.mine ? View.VISIBLE : View.GONE);
        renderLike();
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
        int inner = cardWidth - (int) (32 * d);
        int w = urls.size() == 1 ? inner : (int) (inner * 0.82f);
        int h = w * 3 / 4;
        int radius = (int) (16 * d);
        for (String path : urls) {
            ImageView img = new ImageView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.setMarginEnd((int) (8 * d));
            img.setLayoutParams(lp);
            Glide.with(this).load(fullUrl(path))
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

    // ---------- 열람 기록 ----------
    private void saveView() {
        ApiClient.get().saveView(String.valueOf(guestbookId)).enqueue(new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> res) {}
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    // ---------- 삭제 ----------
    private void confirmDelete() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("방명록 삭제")
                .setMessage("이 방명록을 삭제할까요? 삭제하면 되돌릴 수 없어요.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (d, w) -> doDelete())
                .show();
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
        binding.btnLike.setText(liked ? "♥ 좋아요 취소" : "♡ 좋아요");
        binding.tvLikeCount.setText("좋아요 " + likeCount);
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
