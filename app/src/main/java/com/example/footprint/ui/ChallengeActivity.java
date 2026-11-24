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
import java.util.Map;

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

import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;
import android.widget.ArrayAdapter;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.example.footprint.databinding.BottomSheetChallengeFilterBinding;


public class ChallengeActivity extends AppCompatActivity {

    private ActivityChallengeBinding binding;

    // 상단 카테고리 필터
    private String selectedFilter = "all";

    // 상세 필터
    private String selectedRegion = null;
    private String selectedQuestType = null;
    private Boolean selectedCompleted = null;

    // 위치 인증된 기본 지역
// 아직 인증된 지역 정보가 없으므로 전체 지역으로 설정
    private String defaultRegion = null;
    // 사용자가 직접 필터를 적용했는지 확인
    private boolean isRegionManuallySelected = false;

    // 현재 위치 기반 지역 조회 요청
    private Call<Map<String, String>> regionCall;

    // 자동 지역 설정 완료 여부
    private boolean isDefaultRegionInitialized = false;

    // 지역 선택 목록
    private final String[] regionOptions = {
            "전체 지역",
            "서울",
            "경기",
            "인천",
            "강원",
            "충북",
            "충남",
            "대전",
            "세종",
            "전북",
            "전남",
            "광주",
            "경북",
            "경남",
            "대구",
            "울산",
            "부산",
            "제주"
    };

    // 제목 검색어
    private String searchKeyword = "";

    // 검색 요청 지연 처리
    private final Handler searchHandler =
            new Handler(Looper.getMainLooper());

    private Runnable searchRunnable;

    // 현재 진행 중인 API 요청
    private Call<ChallengeListResponse> challengeCall;
    private ChallengeAdapter challengeAdapter;
    private final List<Challenge> allChallenges = new ArrayList<>();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private boolean firstResume = true;


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


        // 제목 검색창 연결
        setupSearch();

        // 상세 필터 버튼 클릭
        binding.btnChallengeAdvancedFilter.setOnClickListener(v ->
                showAdvancedFilter()
        );

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

        // 최초 실행 시 중복 조회 방지
        if (firstResume) {
            firstResume = false;
            return;
        }

        // 상세 화면에서 돌아왔을 때 목록 갱신
        if (challengeAdapter != null) {
            loadChallenges();
        }
    }
    @Override
    protected void onDestroy() {

        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        if (challengeCall != null) {
            challengeCall.cancel();
        }

        // 주소 변환 API 요청 취소
        if (regionCall != null) {
            regionCall.cancel();
        }

        super.onDestroy();
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

        // 선택된 필터 조건으로 목록 다시 조회
        loadChallenges();
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

    // 필터 조건에 맞는 도전과제 목록 조회
    private void loadChallenges() {

        ApiService apiService = ApiClient.get();

        // 이전 요청이 진행 중이라면 취소
        if (challengeCall != null) {
            challengeCall.cancel();
        }

        // 백엔드에 전달할 카테고리
        String category = selectedFilter.equals("all")
                ? null
                : selectedFilter.toUpperCase(Locale.ROOT);

        // 검색어가 비어 있으면 전체 검색
        String keyword = searchKeyword.trim().isEmpty()
                ? null
                : searchKeyword.trim();

        // 선택된 모든 필터 조건을 서버에 전달
        challengeCall = apiService.getChallenges(
                category,
                selectedRegion,
                selectedQuestType,
                selectedCompleted,
                keyword
        );

        challengeCall.enqueue(
                new Callback<ChallengeListResponse>() {

                    @Override
                    public void onResponse(
                            Call<ChallengeListResponse> call,
                            Response<ChallengeListResponse> response
                    ) {
                        // 이전 요청의 응답은 무시
                        if (call != challengeCall || call.isCanceled()) {
                            return;
                        }

                        binding.swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful()
                                && response.body() != null) {

                            Log.d("ChallengeAPI",
                                    "조회된 도전과제 수: "
                                            + response.body().LocalChallenges_totalCount);

                            allChallenges.clear();

                            for (ChallengeListResponse.ChallengeItem item
                                    : response.body().challenges) {

                                Challenge challenge = new Challenge();

                                challenge.id = String.valueOf(item.challengeId);
                                challenge.title = item.title;
                                challenge.regionName = item.region;
                                challenge.isCompleted = item.isCompleted;

                                // 달성 인원
                                challenge.completedCount = item.completedCount;

                                // 목표 좌표
                                challenge.latitude = item.latitude;
                                challenge.longitude = item.longitude;

                                // 필터링 관련 데이터
                                challenge.category = item.category;
                                challenge.creatorId = item.creatorId;
                                challenge.questType = item.questType;

                                allChallenges.add(challenge);
                            }

                            // 기존 GPS 거리 계산 및 정렬 유지
                            updateChallengeDistances();

                            // 필터링된 목록을 화면에 표시
                            challengeAdapter.setChallenges(allChallenges);

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
                        // 취소된 요청이나 이전 요청의 오류는 무시
                        if (call != challengeCall || call.isCanceled()) {
                            return;
                        }

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

    // 도전과제 제목 검색
    private void setupSearch() {

        binding.etChallengeSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        searchKeyword = s.toString();

                        // 이전 검색 예약 취소
                        if (searchRunnable != null) {
                            searchHandler.removeCallbacks(searchRunnable);
                        }

                        // 400ms 후 검색 실행
                        searchRunnable = () -> loadChallenges();

                        searchHandler.postDelayed(
                                searchRunnable,
                                400
                        );
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }
        );
    }

    // 상세 필터 BottomSheet 표시
    private void showAdvancedFilter() {

        // BottomSheet 생성
        BottomSheetDialog dialog =
                new BottomSheetDialog(this);

        // 상세 필터 XML 연결
        BottomSheetChallengeFilterBinding filterBinding =
                BottomSheetChallengeFilterBinding.inflate(
                        getLayoutInflater()
                );

        dialog.setContentView(filterBinding.getRoot());

        // 1. 지역 선택 Spinner 설정
        ArrayAdapter<String> regionAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        regionOptions
                );

        regionAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        filterBinding.spinnerRegion.setAdapter(regionAdapter);


        // 2. 기존에 적용한 지역 복원
        filterBinding.spinnerRegion.setSelection(
                getRegionIndex(selectedRegion)
        );


        // 3. 기존에 적용한 도전과제 유형 복원
        if ("SINGLE".equals(selectedQuestType)) {

            filterBinding.rgChallengeType.check(
                    R.id.rbTypeSingle
            );

        } else if ("MULTI".equals(selectedQuestType)) {

            filterBinding.rgChallengeType.check(
                    R.id.rbTypeMulti
            );

        } else {

            filterBinding.rgChallengeType.check(
                    R.id.rbTypeAll
            );
        }


        // 4. 기존에 적용한 달성 상태 복원
        if (Boolean.TRUE.equals(selectedCompleted)) {

            filterBinding.rgChallengeStatus.check(
                    R.id.rbStatusCompleted
            );

        } else if (Boolean.FALSE.equals(selectedCompleted)) {

            filterBinding.rgChallengeStatus.check(
                    R.id.rbStatusIncomplete
            );

        } else {

            filterBinding.rgChallengeStatus.check(
                    R.id.rbStatusAll
            );
        }


        // 5. 닫기 버튼
        filterBinding.btnCloseFilter.setOnClickListener(v ->
                dialog.dismiss()
        );


        // 6. 초기화 버튼
        filterBinding.btnResetFilter.setOnClickListener(v -> {

            // 위치 인증된 기본 지역으로 복원
            filterBinding.spinnerRegion.setSelection(
                    getRegionIndex(defaultRegion)
            );

            // 도전과제 유형 전체
            filterBinding.rgChallengeType.check(
                    R.id.rbTypeAll
            );

            // 달성 상태 전체
            filterBinding.rgChallengeStatus.check(
                    R.id.rbStatusAll
            );
        });


        // 7. 필터 적용 버튼
        filterBinding.btnApplyFilter.setOnClickListener(v -> {

            // 사용자가 직접 필터를 적용했음을 기록
            isRegionManuallySelected = true;
            // 선택된 지역 저장
            int regionPosition =
                    filterBinding.spinnerRegion
                            .getSelectedItemPosition();

            selectedRegion = regionPosition <= 0
                    ? null
                    : regionOptions[regionPosition];


            // 선택된 도전과제 유형 저장
            int typeId =
                    filterBinding.rgChallengeType
                            .getCheckedRadioButtonId();

            if (typeId == R.id.rbTypeSingle) {

                selectedQuestType = "SINGLE";

            } else if (typeId == R.id.rbTypeMulti) {

                selectedQuestType = "MULTI";

            } else {

                selectedQuestType = null;
            }


            // 선택된 달성 상태 저장
            int statusId =
                    filterBinding.rgChallengeStatus
                            .getCheckedRadioButtonId();

            if (statusId == R.id.rbStatusCompleted) {

                selectedCompleted = true;

            } else if (statusId == R.id.rbStatusIncomplete) {

                selectedCompleted = false;

            } else {

                selectedCompleted = null;
            }


            // 선택한 필터 조건으로 목록 다시 조회
            loadChallenges();

            // BottomSheet 닫기
            dialog.dismiss();
        });


        // BottomSheet 화면에 표시
        dialog.show();
    }

    // 지역 이름에 해당하는 Spinner 위치 찾기
    private int getRegionIndex(String region) {

        // 전체 지역
        if (region == null) {
            return 0;
        }

        // 지역 목록에서 해당 지역 찾기
        for (int i = 0; i < regionOptions.length; i++) {

            if (regionOptions[i].equals(region)) {
                return i;
            }
        }

        // 해당 지역이 목록에 없으면 전체 지역
        return 0;
    }

    // 주소에서 도전과제 필터에 사용할 지역명 추출
    private String normalizeRegion(String address) {

        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        // 주소의 첫 번째 단어 추출
        String region = address.trim().split("\\s+")[0];

        switch (region) {

            case "서울특별시":
            case "서울":
                return "서울";

            case "경기도":
            case "경기":
                return "경기";

            case "인천광역시":
            case "인천":
                return "인천";

            case "강원특별자치도":
            case "강원도":
            case "강원":
                return "강원";

            case "충청북도":
            case "충북":
                return "충북";

            case "충청남도":
            case "충남":
                return "충남";

            case "대전광역시":
            case "대전":
                return "대전";

            case "세종특별자치시":
            case "세종":
                return "세종";

            case "전북특별자치도":
            case "전라북도":
            case "전북":
                return "전북";

            case "전라남도":
            case "전남":
                return "전남";

            case "광주광역시":
            case "광주":
                return "광주";

            case "경상북도":
            case "경북":
                return "경북";

            case "경상남도":
            case "경남":
                return "경남";

            case "대구광역시":
            case "대구":
                return "대구";

            case "울산광역시":
            case "울산":
                return "울산";

            case "부산광역시":
            case "부산":
                return "부산";

            case "제주특별자치도":
            case "제주도":
            case "제주":
                return "제주";

            default:
                return null;
        }
    }

    // 현재 GPS 위치를 기준으로 기본 지역 설정
    private void initializeDefaultRegion(Location location) {

        // 이미 초기화되었거나 요청 중이라면 실행하지 않음
        if (isDefaultRegionInitialized || regionCall != null) {
            return;
        }

        ApiService apiService = ApiClient.get();

        regionCall = apiService.reverseGeocode(
                location.getLatitude(),
                location.getLongitude()
        );

        regionCall.enqueue(new Callback<Map<String, String>>() {

            @Override
            public void onResponse(
                    Call<Map<String, String>> call,
                    Response<Map<String, String>> response
            ) {

                if (call.isCanceled() || isDestroyed() || isFinishing()) {
                    return;
                }

                regionCall = null;

                if (response.isSuccessful()
                        && response.body() != null) {

                    Map<String, String> result = response.body();

                    // 지번 주소에서 지역 추출
                    String region = normalizeRegion(
                            result.get("address")
                    );

                    // 지번 주소에서 추출하지 못하면 도로명 주소 사용
                    if (region == null) {
                        region = normalizeRegion(
                                result.get("roadAddress")
                        );
                    }

                    // 기본 지역 저장
                    defaultRegion = region;

                    isDefaultRegionInitialized = true;

                    Log.d(
                            "ChallengeRegion",
                            "자동 설정된 기본 지역: " + defaultRegion
                    );

                    // 사용자가 직접 필터를 적용하지 않은 경우만
                    // 자동으로 지역 필터 적용
                    if (!isRegionManuallySelected) {

                        selectedRegion = defaultRegion;

                        loadChallenges();
                    }

                } else {

                    Log.e(
                            "ChallengeRegion",
                            "주소 변환 실패: " + response.code()
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<Map<String, String>> call,
                    Throwable t
            ) {

                if (call.isCanceled() || isDestroyed() || isFinishing()) {
                    return;
                }

                regionCall = null;

                Log.e(
                        "ChallengeRegion",
                        "주소 변환 API 연결 실패",
                        t
                );
            }
        });
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

                binding.tvCurrentLocation.setText("가까운 순");

                // 현재 GPS 기반 기본 지역 자동 설정
                initializeDefaultRegion(location);
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
