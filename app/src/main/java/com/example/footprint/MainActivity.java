package com.example.footprint;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.ApiResponse;
import com.example.footprint.data.model.Guestbook;
import com.example.footprint.data.model.NearbyGuestbookResponse;
import com.example.footprint.databinding.ActivityMainBinding;
import com.example.footprint.ui.ChallengeActivity;
import com.example.footprint.ui.CreateGuestbookActivity;
import com.example.footprint.ui.UserSearchActivity;
import com.example.footprint.util.AppConfig;
import com.example.footprint.util.GuestbookMarkerRenderer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.kakao.vectormap.GestureType;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.camera.CameraPosition;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private ActivityMainBinding binding;
    private FusedLocationProviderClient fusedClient;
    private KakaoMap kakaoMap;
    private Location currentLocation;
    private LabelLayer labelLayer;
    private final Map<String, Guestbook> markerGuestbookById = new HashMap<>();
    private final Handler cameraReloadHandler = new Handler(Looper.getMainLooper());
    private final Runnable cameraReloadRunnable = this::reloadForCamera;
    private long suppressCameraIdleUntilMs = 0L;
    private long reloadRequestVersion = 0L;
    private Call<ApiResponse<NearbyGuestbookResponse>> nearbyCall;
    private Call<List<Guestbook>> boundsCall;
    private String mode = "nearby"; // nearby | following | me

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        binding.mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
            }

            @Override
            public void onMapError(Exception error) {
                Log.e("KakaoMap", "onMapError: " + error.getMessage(), error);
                Toast.makeText(MainActivity.this, "지도 초기화 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                onKakaoMapReady(map);
            }
        });

        binding.fabCreate.setOnClickListener(v -> startActivity(new Intent(this, CreateGuestbookActivity.class)));
        binding.btnUsers.setOnClickListener(v -> startActivity(new Intent(this, UserSearchActivity.class)));
        android.widget.Button accountButton = new android.widget.Button(this);
        accountButton.setText("로그인 / 회원가입");
        ((android.widget.LinearLayout) binding.btnUsers.getParent()).addView(accountButton);
        accountButton.setOnClickListener(v -> startActivity(new Intent(this, com.example.footprint.ui.LoginActivity.class)));
        binding.btnChallenges.setOnClickListener(v -> startActivity(new Intent(this, ChallengeActivity.class)));
        binding.btnNearby.setOnClickListener(v -> switchMode("nearby"));
        binding.btnFollowing.setOnClickListener(v -> switchMode("following"));
        binding.btnMyMap.setOnClickListener(v -> switchMode("me"));
        binding.btnFilter.setOnClickListener(v -> Toast.makeText(this, "필터 UI: 연령대 · 최근 N일 · 인기/최신", Toast.LENGTH_SHORT).show());
    }

    private void onKakaoMapReady(KakaoMap map) {
        this.kakaoMap = map;
        this.labelLayer = map.getLabelManager().getLayer();

        map.setOnLabelClickListener((clickedMap, layer, label) -> {
            Guestbook guestbook = markerGuestbookById.get(label.getLabelId());
            if (guestbook != null) openGuestbook(guestbook);
            return true;
        });
        map.setOnCameraMoveEndListener(new KakaoMap.OnCameraMoveEndListener() {
            @Override
            public void onCameraMoveEnd(@NonNull KakaoMap movedMap, @NonNull CameraPosition cameraPosition, @NonNull GestureType gestureType) {
                onCameraIdle();
            }
        });
        ensureLocationPermissionAndLoad();
    }

    private boolean hasLocationPermission() {
        boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        return hasFine || hasCoarse;
    }

    private void ensureLocationPermissionAndLoad() {
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        loadCurrentLocation();
    }

    private void onCameraIdle() {
        if (SystemClock.elapsedRealtime() < suppressCameraIdleUntilMs) return;
        scheduleReloadForCamera();
    }

    private void suppressCameraIdleShortly() {
        suppressCameraIdleUntilMs = SystemClock.elapsedRealtime() + 700L;
    }

    private void scheduleReloadForCamera() {
        cameraReloadHandler.removeCallbacks(cameraReloadRunnable);
        cameraReloadHandler.postDelayed(cameraReloadRunnable, 250L);
    }

    private void moveCameraTo(double lat, double lng, int zoomLevel) {
        if (kakaoMap == null) return;
        suppressCameraIdleShortly();
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng), zoomLevel));
    }

    private void loadCurrentLocation() {
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            currentLocation = location;
                            if ("nearby".equals(mode)) {
                                moveCameraTo(location.getLatitude(), location.getLongitude(), 15);
                            }
                            scheduleReloadForCamera();
                        }
                    });
        } catch (SecurityException ignored) {
        }
    }

    private void switchMode(String nextMode) {
        mode = nextMode;
        clearMarkers();
        if ("nearby".equals(mode)) {
            if (currentLocation != null) {
                moveCameraTo(currentLocation.getLatitude(), currentLocation.getLongitude(), 15);
            } else {
                ensureLocationPermissionAndLoad();
            }
        } else {
            moveCameraTo(36.35, 127.8, 6);
        }
        scheduleReloadForCamera();
    }

    private double[] estimateBoundsFromCamera() {
        LatLng center = kakaoMap.getCameraPosition().getPosition();
        int zoom = kakaoMap.getCameraPosition().getZoomLevel();
        double latScale = 30.0 / Math.pow(2.0, Math.max(1, zoom));
        double lngScale = latScale / Math.max(0.2, Math.cos(Math.toRadians(center.getLatitude())));
        return new double[]{
                center.getLatitude() - latScale,
                center.getLongitude() - lngScale,
                center.getLatitude() + latScale,
                center.getLongitude() + lngScale
        };
    }

    private void reloadForCamera() {
        if (kakaoMap == null) return;
        final long requestVersion = ++reloadRequestVersion;
        int zoom = kakaoMap.getCameraPosition().getZoomLevel();

        if ("nearby".equals(mode)) {
            if (currentLocation == null) return;
            if (nearbyCall != null && !nearbyCall.isCanceled()) nearbyCall.cancel();
            nearbyCall = ApiClient.get().getNearbyGuestbooks(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    zoom
            );
            nearbyCall.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<NearbyGuestbookResponse>> call,
                                       @NonNull Response<ApiResponse<NearbyGuestbookResponse>> response) {
                    if (requestVersion != reloadRequestVersion || call.isCanceled()) return;
                    if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) return;
                    renderGuestbooks(response.body().getData().getGuestbooks(), zoom);
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<NearbyGuestbookResponse>> call, @NonNull Throwable t) {
                    if (requestVersion != reloadRequestVersion || call.isCanceled()) return;
                    Toast.makeText(MainActivity.this, "주변 방명록 조회 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            double[] bounds = estimateBoundsFromCamera();
            if (boundsCall != null && !boundsCall.isCanceled()) boundsCall.cancel();
            boundsCall = ApiClient.get().getGuestbooksInBounds(
                    "following".equals(mode) ? "following" : "me",
                    null,
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
                    if (requestVersion != reloadRequestVersion || call.isCanceled()) return;
                    if (!response.isSuccessful() || response.body() == null) return;
                    renderGuestbooks(response.body(), zoom);
                }

                @Override
                public void onFailure(@NonNull Call<List<Guestbook>> call, @NonNull Throwable t) {
                    if (requestVersion != reloadRequestVersion || call.isCanceled()) return;
                    Toast.makeText(MainActivity.this, "영역 방명록 조회 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void renderGuestbooks(List<Guestbook> list, int zoom) {
        clearMarkers();
        if (list == null || labelLayer == null) return;

        for (Guestbook g : list) {
            if (!"nearby".equals(mode) && zoom < AppConfig.ZOOM_REGION && !g.isRepresentative()) continue;

            String labelId = "guestbook-" + g.getId();
            Label label = labelLayer.addLabel(
                    LabelOptions.from(labelId, LatLng.from(g.getLatitude(), g.getLongitude()))
                            .setStyles(LabelStyle.from(
                                    GuestbookMarkerRenderer.createMarkerBitmap(
                                            this,
                                            g.getShortText(),
                                            g.isRepresentative() || g.isClustered()
                                    )
                            ))
            );
            if (label != null) {
                markerGuestbookById.put(labelId, g);
            }
        }
    }

    private void openGuestbook(Guestbook g) {
        if (g.isClustered()) {
            int maxZoom = (int) Math.round(AppConfig.ZOOM_DETAIL) + 3;
            int zoom = Math.min(kakaoMap.getCameraPosition().getZoomLevel() + 2, maxZoom);
            moveCameraTo(g.getLatitude(), g.getLongitude(), zoom);
            return;
        }

        if ("nearby".equals(mode) && !g.isAccessible()) {
            String distMsg = g.getDistanceMeter() != null
                    ? String.format(Locale.getDefault(), " (현재 거리: %.0fm)", g.getDistanceMeter())
                    : "";
            Toast.makeText(this, "직접 가까이 이동해야 열람할 수 있어요." + distMsg, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, g.getShortText(), Toast.LENGTH_SHORT).show();
    }

    private void clearMarkers() {
        markerGuestbookById.clear();
        if (kakaoMap != null) kakaoMap.getLabelManager().clearAll();
        if (kakaoMap != null) labelLayer = kakaoMap.getLabelManager().getLayer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (hasLocationPermission()) {
                loadCurrentLocation();
            } else if ("nearby".equals(mode)) {
                Toast.makeText(this, "주변 모드는 위치 권한이 필요해요.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        cameraReloadHandler.removeCallbacks(cameraReloadRunnable);
        if (nearbyCall != null && !nearbyCall.isCanceled()) nearbyCall.cancel();
        if (boundsCall != null && !boundsCall.isCanceled()) boundsCall.cancel();
        super.onDestroy();
    }
}
