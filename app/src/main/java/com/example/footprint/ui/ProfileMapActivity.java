package com.example.footprint.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.footprint.databinding.ActivityProfileMapBinding;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;

public class ProfileMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityProfileMapBinding binding;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.profileMapView.onCreate(savedInstanceState);
        binding.profileMapView.getMapAsync(this);
    }
    @Override public void onMapReady(@NonNull NaverMap map) {
        // User-specific map: country-wide browsing allowed because scope is one explicit user.
        map.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(36.35,127.8), 6.5));
        // TODO call /api/maps/guestbooks/bound?scope=user&userId=...
    }
    @Override protected void onStart(){super.onStart();binding.profileMapView.onStart();}
    @Override protected void onResume(){super.onResume();binding.profileMapView.onResume();}
    @Override protected void onPause(){binding.profileMapView.onPause();super.onPause();}
    @Override protected void onStop(){binding.profileMapView.onStop();super.onStop();}
    @Override protected void onDestroy(){binding.profileMapView.onDestroy();super.onDestroy();}
    @Override public void onLowMemory(){super.onLowMemory();binding.profileMapView.onLowMemory();}
    @Override protected void onSaveInstanceState(@NonNull Bundle outState){super.onSaveInstanceState(outState);binding.profileMapView.onSaveInstanceState(outState);}
}
