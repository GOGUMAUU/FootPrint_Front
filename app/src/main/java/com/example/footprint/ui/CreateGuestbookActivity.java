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

public class CreateGuestbookActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityCreateGuestbookBinding binding;
    private NaverMap naverMap;
    private Marker selectedMarker;
    private LatLng selected;
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
    }

    @Override public void onMapReady(@NonNull NaverMap map) {
        naverMap = map;
        map.setOnMapClickListener((point, coord) -> {
            selected = coord;
            if (selectedMarker == null) selectedMarker = new Marker();
            selectedMarker.setPosition(coord);
            selectedMarker.setMap(naverMap);
            binding.tvSelectedLocation.setText(String.format("선택 위치: %.5f, %.5f", coord.latitude, coord.longitude));
        });
    }

    private void save() {
        String text = binding.etContent.getText().toString().trim();
        if (selected == null) { Toast.makeText(this, "지도에서 장소를 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (selectedPhoto == null) { Toast.makeText(this, "사진을 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (text.isEmpty()) { Toast.makeText(this, "짧은 글을 입력하세요.", Toast.LENGTH_SHORT).show(); return; }
        // TODO: 1) current GPS와 selected 좌표 간 create 허용거리 검증 2) 사진 업로드 3) guestbook POST.
        // 서버에서도 반드시 동일한 위치 검증을 수행해야 합니다.
        Toast.makeText(this, "저장 API 연결 지점까지 구현된 스캐폴드입니다.", Toast.LENGTH_LONG).show();
    }

    @Override protected void onStart(){super.onStart();binding.createMapView.onStart();}
    @Override protected void onResume(){super.onResume();binding.createMapView.onResume();}
    @Override protected void onPause(){binding.createMapView.onPause();super.onPause();}
    @Override protected void onStop(){binding.createMapView.onStop();super.onStop();}
    @Override protected void onDestroy(){binding.createMapView.onDestroy();super.onDestroy();}
    @Override public void onLowMemory(){super.onLowMemory();binding.createMapView.onLowMemory();}
    @Override protected void onSaveInstanceState(@NonNull Bundle outState){super.onSaveInstanceState(outState);binding.createMapView.onSaveInstanceState(outState);}
}
