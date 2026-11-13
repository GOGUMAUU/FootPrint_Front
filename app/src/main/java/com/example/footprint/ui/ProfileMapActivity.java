package com.example.footprint.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.footprint.api.ApiClient;
import com.example.footprint.databinding.ActivityProfileMapBinding;
import com.example.footprint.map.FootprintMapController;
import com.example.footprint.data.model.Guestbook;
import com.example.footprint.util.AppConfig;
import com.example.footprint.util.MockGuestbooks;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.camera.CameraUpdateFactory;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileMapActivity extends AppCompatActivity {
    private ActivityProfileMapBinding binding;
    private FootprintMapController map;

    private long targetUserId = -1L;
    private Call<List<Guestbook>> boundsCall;

    private final Handler reloadHandler = new Handler(Looper.getMainLooper());
    private final Runnable reloadRunnable = this::loadUserGuestbooks;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        targetUserId = getIntent().getLongExtra("TARGET_USER_ID", -1L);
        String nickname = getIntent().getStringExtra("TARGET_USER_NICKNAME");
        if (nickname != null && !nickname.isEmpty()) {
            setTitle(nickname + "님의 발자국");
        }

        map = new FootprintMapController(this, binding.profileMapView);
        map.setOnGuestbookClickListener(this::openGuestbook);
        map.setOnCameraIdleListener((center, zoom) -> scheduleReload());
        map.start(() -> {
            map.moveCamera(36.35, 127.8, (int) Math.round(AppConfig.ZOOM_COUNTRY));
            scheduleReload();
        });
    }
    private void scheduleReload() {
        reloadHandler.removeCallbacks(reloadRunnable);
        reloadHandler.postDelayed(reloadRunnable, 250L);
    }

    private void loadUserGuestbooks() {
        if (!map.isReady() || targetUserId <= 0) return;

        double[] bounds = map.estimateVisibleBounds();
        if (bounds == null) return;
        int zoom = map.getZoomLevel();

        if (AppConfig.USE_MOCK_DATA) {
            List<Guestbook> mockList = MockGuestbooks.inBounds(
                    "user", bounds, zoom, zoom < AppConfig.ZOOM_DETAIL ? "popular" : "latest");
            map.showGuestbooks(mockList);
            return;
        }

        if (boundsCall != null && !boundsCall.isCanceled()) boundsCall.cancel();
        boundsCall = ApiClient.get().getGuestbooksInBounds(
                "user",
                targetUserId,
                zoom,
                bounds[0],
                bounds[1],
                bounds[2],
                bounds[3],
                null,
                null,
                zoom < AppConfig.ZOOM_DETAIL ? "popular" : "latest"
        );

        boundsCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Guestbook>> call, @NonNull Response<List<Guestbook>> response) {
                if (isDestroyed() || call.isCanceled()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ProfileMapActivity.this, "방명록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                map.showGuestbooks(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<List<Guestbook>> call, @NonNull Throwable t) {
                if (isDestroyed() || call.isCanceled()) return;
                Toast.makeText(ProfileMapActivity.this, "조회 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGuestbook(Guestbook g) {
        Toast.makeText(this, g.getShortText(), Toast.LENGTH_SHORT).show();
    }

    // ───────────────────────── 생명주기 관리 ─────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        map.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.pause();
    }

    @Override
    protected void onDestroy() {
        reloadHandler.removeCallbacks(reloadRunnable);
        if (boundsCall != null && !boundsCall.isCanceled()) boundsCall.cancel();
        super.onDestroy();
    }
}
