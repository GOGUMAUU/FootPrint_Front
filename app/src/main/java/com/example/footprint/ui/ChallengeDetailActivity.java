package com.example.footprint.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.footprint.databinding.ActivityChallengeDetailBinding;

import android.net.Uri;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.HashMap;
import java.util.Map;

public class ChallengeDetailActivity extends AppCompatActivity {

    private ActivityChallengeDetailBinding binding;

    // 사용자가 선택한 사진의 URI
    private Uri selectedPhotoUri;

    // 현재 GPS 위치를 가져오는 객체
    private FusedLocationProviderClient fusedLocationClient;

    // GPS 인증에 사용할 현재 위치
    private Location verificationLocation;

    // 도전과제 ID
    private String challengeId;

    // 사진 선택 화면을 열고 결과를 받는 기능
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        if (uri != null) {
                            selectedPhotoUri = uri;

                            // 선택한 사진을 상세 화면에 표시
                            binding.ivChallengePhoto.setImageURI(uri);

                            Toast.makeText(
                                    this,
                                    "사진이 선택되었습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상세 화면 XML 연결
        binding = ActivityChallengeDetailBinding.inflate(
                getLayoutInflater()
        );
        setContentView(binding.getRoot());

        // 목록에서 전달받은 도전과제 정보
        challengeId = getIntent().getStringExtra("challengeId");
        String challengeTitle = getIntent().getStringExtra("challengeTitle");
        String challengeDescription = getIntent().getStringExtra("challengeDescription");
        String challengeRegion = getIntent().getStringExtra("challengeRegion");

        // 상세 화면에 정보 표시
        binding.tvChallengeTitle.setText(
                challengeTitle != null ? challengeTitle : "도전과제"
        );

        binding.tvChallengeDescription.setText(
                challengeDescription != null
                        ? challengeDescription
                        : "도전과제 상세 설명을 준비 중입니다."
        );

        binding.tvChallengeCategory.setText(
                challengeRegion != null ? challengeRegion : "지역 정보 없음"
        );

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(v -> finish());

        // GPS 인증 전에는 활동 등록 불가능
        binding.btnSubmit.setEnabled(false);
        binding.btnSubmit.setAlpha(0.5f);

        // 초기 인증 상태
        binding.tvVerificationStatus.setText("위치인증 미완료");

        // 사진 추가하기 버튼 클릭
        binding.btnSelectPhoto.setOnClickListener(v -> {

            photoPickerLauncher.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE
                            )
                            .build()
            );

        });

        // GPS 위치 서비스 준비
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        // GPS 인증 버튼 클릭
        binding.btnVerify.setOnClickListener(v -> {
            verifyCurrentLocation();
        });

        // 메모 입력값은 활동 등록 시 사용 예정
        // 현재는 별도로 저장하거나 서버에 전송하지 않음

    }

    private void verifyCurrentLocation() {

        boolean fineGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        // 위치 권한 확인
        if (!fineGranted && !coarseGranted) {
            Toast.makeText(
                    this,
                    "위치 권한이 필요합니다.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        binding.tvLocationStatus.setText("현재 위치 확인 중...");

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

                verificationLocation = location;

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                binding.tvLocationStatus.setText(
                        "현재 위치 확인 완료"
                );

                Toast.makeText(
                        this,
                        "GPS 위치 확인 성공",
                        Toast.LENGTH_SHORT
                ).show();

                // 다음 단계에서 latitude, longitude를
                // 서버의 GPS 인증 API로 전달할 예정

            } else {
                binding.tvLocationStatus.setText(
                        "현재 위치를 확인할 수 없습니다."
                );
            }

        }).addOnFailureListener(e -> {

            binding.tvLocationStatus.setText(
                    "위치 확인 중 오류가 발생했습니다."
            );

        });
    }


    private Map<String, Double> createVerificationLocationData() {

        // GPS 위치를 아직 확인하지 않은 경우
        if (verificationLocation == null) {
            return null;
        }

        // 서버에 보낼 데이터 생성
        Map<String, Double> locationData = new HashMap<>();

        locationData.put(
                "userLatitude",
                verificationLocation.getLatitude()
        );

        locationData.put(
                "userLongitude",
                verificationLocation.getLongitude()
        );

        return locationData;
    }

}
