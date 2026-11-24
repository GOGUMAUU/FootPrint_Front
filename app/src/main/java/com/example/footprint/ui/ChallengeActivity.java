package com.example.footprint.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.footprint.MainActivity;
import com.example.footprint.R;
import com.example.footprint.api.ApiClient;
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

import com.example.footprint.api.ApiService;
import com.example.footprint.data.model.ChallengeListResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.util.Log;
import android.widget.Toast;

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
        
        // 로그인하지 않은 사용자는 로그인 화면으로 이동
        if (!com.example.footprint.security.Session.signedIn(this)) {
            com.example.footprint.security.Session.login(this);
            return;
        }

        binding = ActivityChallengeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // GPS 위치 서비스 준비
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        // 현재 위치 확인 시작
        checkLocationPermission();

        // 뒤로가기 버튼
        binding.btnHome.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });


        // 내 배지 버튼 클릭 시 배지 화면으로 이동
        binding.btnMyBadges.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ChallengeActivity.this,
                    MyBadgesActivity.class
            );

            startActivity(intent);
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


        // 백엔드에서 실제 도전과제 목록 불러오기
        loadChallenges();

        // 아래로 당겨서 새로고침
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadChallenges();
        });

        // 처음에는 전체 필터 선택
        selectFilter("all");
        }

    @Override
    protected void onResume() {
        super.onResume();

        // 상세 화면에서 돌아왔을 때 도전과제 목록 갱신
        if (challengeAdapter != null) {
            loadChallenges();
        }
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

    // 실제 도전과제 목록
    private void loadChallenges() {

        ApiService apiService = ApiClient.get();

        apiService.getChallenges().enqueue(
                new Callback<ChallengeListResponse>() {

                    @Override
                    public void onResponse(
                            Call<ChallengeListResponse> call,
                            Response<ChallengeListResponse> response
                    ) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        if (response.isSuccessful()
                                && response.body() != null) {
                            Log.d("ChallengeAPI",
                                    "전체 도전과제 수: " + response.body().LocalChallenges_totalCount);

                            for (ChallengeListResponse.ChallengeItem item : response.body().challenges) {
                                Log.d("ChallengeAPI",
                                        "도전과제 ID: " + item.challengeId
                                                + ", 제목: " + item.title
                                                + ", 완료 여부: " + item.isCompleted);
                            }

                            allChallenges.clear();

                            for (ChallengeListResponse.ChallengeItem item
                                    : response.body().challenges) {


                                Challenge challenge = new Challenge();
                                challenge.id = String.valueOf(item.challengeId);
                                challenge.title = item.title;
                                challenge.regionName = item.region;
                                challenge.isCompleted = item.isCompleted;
                                // 서버에서 받은 달성 인원 저장
                                challenge.completedCount = item.completedCount;
                                // 서버에서 받은 도전과제 목표 좌표 저장
                                challenge.latitude = item.latitude;
                                challenge.longitude = item.longitude;

                                allChallenges.add(challenge);

                            }

                            challengeAdapter.setChallenges(allChallenges);
                            updateChallengeDistances();

                        } else {
                            Log.e("ChallengeAPI",
                                    "목록 조회 실패: " + response.code());

                            Toast.makeText(
                                    ChallengeActivity.this,
                                    "도전과제 목록을 불러오지 못했습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<ChallengeListResponse> call,
                            Throwable t
                    ) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        Log.e("ChallengeAPI", "서버 연결 실패", t);

                        Toast.makeText(
                                ChallengeActivity.this,
                                "서버에 연결할 수 없습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void updateChallengeDistances() {

        // 현재 위치를 가져오지 못했다면 기존 순서 유지
        if (currentLocation == null) {
            return;
        }

        // 각 도전과제까지의 거리 계산
        for (Challenge challenge : allChallenges) {

            float[] results = new float[1];

            Location.distanceBetween(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    challenge.latitude,
                    challenge.longitude,
                    results
            );

            challenge.distanceMeters = results[0];
        }

        // 현재 위치에서 가까운 도전과제부터 정렬
        allChallenges.sort((a, b) ->
                Float.compare(a.distanceMeters, b.distanceMeters)
        );

        // 정렬된 목록을 화면에 반영
        if (challengeAdapter != null) {
            challengeAdapter.setChallenges(allChallenges);
        }
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
                updateChallengeDistances();

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
