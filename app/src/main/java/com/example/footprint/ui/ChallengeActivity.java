package com.example.footprint.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.footprint.MainActivity;
import com.example.footprint.R;
import com.example.footprint.databinding.ActivityChallengeBinding;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.footprint.data.model.Challenge;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;


public class ChallengeActivity extends AppCompatActivity {

    private ActivityChallengeBinding binding;

    // 현재 선택된 필터
    private String selectedFilter = "all";
    private ChallengeAdapter challengeAdapter;
    private final List<Challenge> allChallenges = new ArrayList<>();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChallengeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // GPS 위치 서비스 준비
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        // 현재 위치 확인 시작
        checkLocationPermission();

        // 홈 버튼
        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ChallengeActivity.this,
                    MainActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });

        // 필터 버튼 클릭 이벤트
        binding.btnFilterAll.setOnClickListener(v ->
                selectFilter("all")
        );

        binding.btnFilterDefault.setOnClickListener(v ->
                selectFilter("default")
        );

        binding.btnFilterMine.setOnClickListener(v ->
                selectFilter("mine")
        );

        binding.btnFilterFollowing.setOnClickListener(v ->
                selectFilter("following")
        );

        // 도전과제 목록 연결
        challengeAdapter = new ChallengeAdapter();

        binding.rvAchievableChallenges.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.rvAchievableChallenges.setAdapter(challengeAdapter);

        // 임시 도전과제 데이터
        loadSampleChallenges();

        // 처음에는 전체 필터 선택
        selectFilter("all");
    }

    // 선택된 필터 변경
    private void selectFilter(String filter) {
        selectedFilter = filter;

        // 모든 필터를 선택 해제 상태로 변경
        resetFilter(binding.btnFilterAll);
        resetFilter(binding.btnFilterDefault);
        resetFilter(binding.btnFilterMine);
        resetFilter(binding.btnFilterFollowing);

        // 선택한 필터만 활성화
        switch (selectedFilter) {
            case "all":
                activateFilter(binding.btnFilterAll);
                break;

            case "default":
                activateFilter(binding.btnFilterDefault);
                break;

            case "mine":
                activateFilter(binding.btnFilterMine);
                break;

            case "following":
                activateFilter(binding.btnFilterFollowing);
                break;
        }
    }

    // 선택되지 않은 필터 디자인
    private void resetFilter(TextView filterButton) {
        filterButton.setBackgroundResource(
                R.drawable.bg_filter_normal
        );
        filterButton.setTextColor(
                android.graphics.Color.BLACK
        );
    }

    // 선택된 필터 디자인
    private void activateFilter(TextView filterButton) {
        filterButton.setBackgroundResource(
                R.drawable.bg_filter_selected
        );
        filterButton.setTextColor(
                android.graphics.Color.WHITE
        );
    }

    // 테스트용 임시 데이터
    private void loadSampleChallenges() {
        allChallenges.clear();

        Challenge first = new Challenge();
        first.id = "1";
        first.title = "지역 탐방 도전과제";
        first.regionName = "서울";
        allChallenges.add(first);

        Challenge second = new Challenge();
        second.id = "2";
        second.title = "나만의 여행 기록";
        second.regionName = "부산";
        allChallenges.add(second);

        Challenge third = new Challenge();
        third.id = "3";
        third.title = "친구의 여행 도전과제";
        third.regionName = "제주";
        allChallenges.add(third);

        challengeAdapter.setChallenges(allChallenges);
    }


    // 위치 권한 확인
    private void checkLocationPermission() {

        boolean fineGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    // 현재 위치 가져오기
    private void getCurrentLocation() {

        boolean fineGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            binding.tvCurrentLocation.setText("위치 권한이 필요합니다.");
            return;
        }

        binding.tvCurrentLocation.setText("현재 위치 확인 중...");

        int priority = fineGranted
                ? Priority.PRIORITY_HIGH_ACCURACY
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;

        CancellationTokenSource tokenSource =
                new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(
                priority,
                tokenSource.getToken()
        ).addOnSuccessListener(location -> {

            if (location != null) {
                currentLocation = location;

                binding.tvCurrentLocation.setText(
                        "현재 위치 확인 완료"
                );

            } else {
                binding.tvCurrentLocation.setText(
                        "현재 위치를 확인할 수 없습니다."
                );
            }

        }).addOnFailureListener(e -> {

            binding.tvCurrentLocation.setText(
                    "위치 확인 중 오류가 발생했습니다."
            );

        });
    }

    // 위치 권한 요청 결과
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            boolean granted = false;

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                getCurrentLocation();
            } else {
                binding.tvCurrentLocation.setText(
                        "위치 권한이 거부되었습니다."
                );
            }
        }
    }

}
