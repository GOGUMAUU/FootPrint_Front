package com.example.footprint.ui;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.footprint.databinding.ActivityCreateGuestbookBinding;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.content.ContextCompat;
import com.example.footprint.util.AppConfig;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.overlay.CircleOverlay;
import java.util.Locale;
import java.util.function.Consumer;

public class CreateGuestbookActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityCreateGuestbookBinding binding;
    private NaverMap naverMap;
    private Marker selectedMarker;
    private LatLng selected;
    private Location currentLocation;
    private CircleOverlay allowedCircle;
    private boolean cameraMoved;
    private FusedLocationProviderClient fusedClient;
    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> loadCurrentLocation(null));
    private Uri selectedPhoto;

    private final ActivityResultLauncher<String> pickPhoto = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                selectedPhoto = uri;
                binding.btnPickPhoto.setText(uri == null ? "사진 선택" : "사진 선택됨");
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateGuestbookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.createMapView.onCreate(savedInstanceState);
        binding.createMapView.getMapAsync(this);
        binding.btnPickPhoto.setOnClickListener(v -> pickPhoto.launch("image/*"));
        binding.btnSave.setOnClickListener(v -> save());
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocation();
    }

    @Override public void onMapReady(@NonNull NaverMap map) {
        naverMap = map;
        showMyLocationOnMap();
        map.setOnMapClickListener((point, coord) -> {
            if (currentLocation == null) {
                Toast.makeText(this, "내 위치를 확인하는 중이에요. 잠시 후 다시 눌러주세요.", Toast.LENGTH_SHORT).show();
                requestLocation();
                return;
            }
            float meters = distanceTo(coord);
            if (meters > AppConfig.CREATE_ALLOWED_RADIUS_METERS) {
                Toast.makeText(this, "내 위치에서 " + (int) AppConfig.CREATE_ALLOWED_RADIUS_METERS
                        + "m 이내만 선택할 수 있어요. (지금 " + (int) meters + "m)", Toast.LENGTH_SHORT).show();
                return;
            }

            selected = coord;
            if (selectedMarker == null) selectedMarker = new Marker();
            selectedMarker.setPosition(coord);
            selectedMarker.setMap(naverMap);
            binding.tvSelectedLocation.setText(String.format(Locale.KOREA, "선택 위치: %.5f, %.5f (내 위치에서 %dm)",
                    coord.latitude, coord.longitude, (int) meters));

        });
    }

    private void save() {
        String text = binding.etContent.getText().toString().trim();
        if (selected == null) { Toast.makeText(this, "지도에서 장소를 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (selectedPhoto == null) { Toast.makeText(this, "사진을 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (text.isEmpty()) { Toast.makeText(this, "짧은 글을 입력하세요.", Toast.LENGTH_SHORT).show(); return; }
        // TODO: 1) current GPS와 selected 좌표 간 create 허용거리 검증 2) 사진 업로드 3) guestbook POST.

        Toast.makeText(this, "저장 API 연결 지점까지 구현된 스캐폴드입니다.", Toast.LENGTH_LONG).show();
    }

    // ---------- 위치 ----------
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocation() {
        if (hasLocationPermission()) { loadCurrentLocation(null); return; }
        locationPermission.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    /** 현재 위치를 읽어 currentLocation에 저장합니다. 끝나면 done에 결과(실패 시 null)를 전달합니다. */
    private void loadCurrentLocation(Consumer<Location> done) {
        if (!hasLocationPermission()) { if (done != null) done.accept(null); return; }
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                    .addOnSuccessListener(loc -> {
                        if (loc != null) { currentLocation = loc; showMyLocationOnMap(); }
                        if (done != null) done.accept(loc);
                    })
                    .addOnFailureListener(e -> { if (done != null) done.accept(null); });
        } catch (SecurityException e) {
            if (done != null) done.accept(null);
        }
    }

    private float distanceTo(LatLng p) {
        float[] r = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), p.latitude, p.longitude, r);
        return r[0];
    }

    /** 내 위치 주변의 선택 가능 범위를 원으로 표시하고, 처음 한 번만 그 위치로 카메라를 옮깁니다. */
    private void showMyLocationOnMap() {
        if (naverMap == null || currentLocation == null) return;
        LatLng me = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        if (allowedCircle == null) {
            allowedCircle = new CircleOverlay();
            allowedCircle.setColor(0x222457E6);
            allowedCircle.setOutlineColor(0xFF2457E6);
            allowedCircle.setOutlineWidth(3);
        }
        allowedCircle.setCenter(me);
        allowedCircle.setRadius(AppConfig.CREATE_ALLOWED_RADIUS_METERS);
        allowedCircle.setMap(naverMap);
        if (!cameraMoved) {
            cameraMoved = true;
            naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(me, 16));
        }
    }

    @Override protected void onStart(){super.onStart();binding.createMapView.onStart();}
    @Override protected void onResume(){super.onResume();binding.createMapView.onResume();}
    @Override protected void onPause(){binding.createMapView.onPause();super.onPause();}
    @Override protected void onStop(){binding.createMapView.onStop();super.onStop();}
    @Override protected void onDestroy(){binding.createMapView.onDestroy();super.onDestroy();}
    @Override public void onLowMemory(){super.onLowMemory();binding.createMapView.onLowMemory();}
    @Override protected void onSaveInstanceState(@NonNull Bundle outState){super.onSaveInstanceState(outState);binding.createMapView.onSaveInstanceState(outState);}
}
