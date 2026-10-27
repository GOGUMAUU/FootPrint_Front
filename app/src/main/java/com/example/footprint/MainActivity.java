package com.example.footprint;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.footprint.api.ApiClient;
import com.example.footprint.databinding.ActivityMainBinding;
import com.example.footprint.data.model.Guestbook;
import com.example.footprint.ui.UserSearchActivity;
import com.example.footprint.ui.CreateGuestbookActivity;
import com.example.footprint.ui.ChallengeActivity;
import com.example.footprint.ui.ProfileMapActivity;
import com.example.footprint.util.AppConfig;
import com.example.footprint.util.GuestbookMarkerRenderer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private ActivityMainBinding binding;
    private FusedLocationSource locationSource;
    private FusedLocationProviderClient fusedClient;
    private NaverMap naverMap;
    private Location currentLocation;
    private final List<Marker> markers = new ArrayList<>();
    private String mode = "nearby"; // nearby | following | me

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(this);

        binding.fabCreate.setOnClickListener(v -> startActivity(new Intent(this, CreateGuestbookActivity.class)));
        binding.btnUsers.setOnClickListener(v -> startActivity(new Intent(this, UserSearchActivity.class)));
        binding.btnChallenges.setOnClickListener(v -> startActivity(new Intent(this, ChallengeActivity.class)));
        binding.btnNearby.setOnClickListener(v -> switchMode("nearby"));
        binding.btnFollowing.setOnClickListener(v -> switchMode("following"));
        binding.btnMyMap.setOnClickListener(v -> switchMode("me"));
        binding.btnFilter.setOnClickListener(v -> Toast.makeText(this, "필터 UI: 연령대 · 최근 N일 · 인기/최신", Toast.LENGTH_SHORT).show());
    }

    @Override public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
        map.setLocationSource(locationSource);
        map.getUiSettings().setLocationButtonEnabled(true);
        map.setLocationTrackingMode(LocationTrackingMode.Follow);
        map.setMinZoom(5.5);
        map.setMaxZoom(20.0);
        map.addOnCameraIdleListener(this::reloadForCamera);
        requestAndLoadLocation();
    }

    private void requestAndLoadLocation() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        try {
            fusedClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(location.getLatitude(), location.getLongitude()), 14));
                    reloadForCamera();
                }
            });
        } catch (SecurityException ignored) {}
    }

    private void switchMode(String nextMode) {
        mode = nextMode;
        clearMarkers();
        if ("nearby".equals(mode) && currentLocation != null) {
            naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 14));
        } else {
            // Full-country view is allowed only for my records / people I follow.
            naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(36.35, 127.8), 6.5));
        }
        reloadForCamera();
    }

    private void reloadForCamera() {
        if (naverMap == null) return;
        double zoom = naverMap.getCameraPosition().zoom;
        if ("nearby".equals(mode)) {
            if (currentLocation == null) return;
            // Backend must enforce radius as well; UI restriction alone is not security.
            ApiClient.get().getNearbyGuestbooks(currentLocation.getLatitude(), currentLocation.getLongitude(), zoom,
                            AppConfig.NEARBY_MAX_RADIUS_METERS, null, 30, zoom < AppConfig.ZOOM_DETAIL ? "popular" : "latest")
                    .enqueue(guestbookCallback());
        } else {
            com.naver.maps.geometry.LatLngBounds b = naverMap.getContentBounds();
            ApiClient.get().getGuestbooksInBounds("following".equals(mode) ? "following" : "me", null, zoom,
                            b.getSouthLatitude(), b.getWestLongitude(), b.getNorthLatitude(), b.getEastLongitude())
                    .enqueue(guestbookCallback());
        }
    }

    private Callback<List<Guestbook>> guestbookCallback() {
        return new Callback<List<Guestbook>>() {
            @Override public void onResponse(@NonNull Call<List<Guestbook>> call, @NonNull Response<List<Guestbook>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                clearMarkers();
                double zoom = naverMap.getCameraPosition().zoom;
                for (Guestbook g : response.body()) {
                    if (zoom < AppConfig.ZOOM_REGION && !g.isRepresentative()) continue;
                    Marker m = new Marker();
                    m.setPosition(new LatLng(g.getLatitude(), g.getLongitude()));
                    GuestbookMarkerRenderer.apply(MainActivity.this, m, g.getShortText(), g.isRepresentative());
                    m.setOnClickListener(overlay -> {
                        openGuestbook(g);
                        return true;
                    });
                    m.setMap(naverMap);
                    markers.add(m);
                }
            }
            @Override public void onFailure(@NonNull Call<List<Guestbook>> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "목록을 불러오지 못했습니다: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void openGuestbook(Guestbook g) {
        if (!"nearby".equals(mode)) {
            Toast.makeText(this, g.getShortText() == null ? "방명록" : g.getShortText(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentLocation == null) return;
        java.util.Map<String, Double> body = new java.util.HashMap<>();
        body.put("lat", currentLocation.getLatitude());
        body.put("lng", currentLocation.getLongitude());
        ApiClient.get().checkDistance(String.valueOf(g.getId()), body).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override public void onResponse(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Response<java.util.Map<String, Object>> response) {
                boolean allowed = response.body() != null && Boolean.TRUE.equals(response.body().get("allowed"));
                Toast.makeText(MainActivity.this, allowed ? (g.getShortText() == null ? "방명록 열림" : g.getShortText()) : "직접 가까이 이동해야 열람할 수 있어요.", Toast.LENGTH_LONG).show();
            }
            @Override public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {}
        });
    }

    private void clearMarkers() { for (Marker m : markers) m.setMap(null); markers.clear(); }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) return;
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override protected void onStart(){super.onStart();binding.mapView.onStart();}
    @Override protected void onResume(){super.onResume();binding.mapView.onResume();}
    @Override protected void onPause(){binding.mapView.onPause();super.onPause();}
    @Override protected void onStop(){binding.mapView.onStop();super.onStop();}
    @Override protected void onDestroy(){binding.mapView.onDestroy();super.onDestroy();}
    @Override public void onLowMemory(){super.onLowMemory();binding.mapView.onLowMemory();}
    @Override protected void onSaveInstanceState(@NonNull Bundle outState){super.onSaveInstanceState(outState);binding.mapView.onSaveInstanceState(outState);}
}
