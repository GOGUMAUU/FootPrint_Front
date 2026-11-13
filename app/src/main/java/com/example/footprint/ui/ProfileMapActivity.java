package com.example.footprint.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.footprint.databinding.ActivityProfileMapBinding;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.camera.CameraUpdateFactory;

public class ProfileMapActivity extends AppCompatActivity {
    private ActivityProfileMapBinding binding;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.profileMapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
            }

            @Override
            public void onMapError(Exception error) {
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(36.35, 127.8), 6));
                // TODO call /api/maps/guestbooks/bound?scope=user&userId=...
            }
        });
    }
}
