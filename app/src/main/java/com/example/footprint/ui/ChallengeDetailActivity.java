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

import com.example.footprint.api.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.util.HashMap;
import java.util.Map;

public class ChallengeDetailActivity extends AppCompatActivity {

    private ActivityChallengeDetailBinding binding;

    // 사용자가 선택한 사진의 URI
    private Uri selectedPhotoUri;
    private String uploadedPhotoPath;

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
                            uploadedPhotoPath = null;

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

        binding.btnSubmit.setOnClickListener(v -> {

            if (selectedPhotoUri == null) {
                Toast.makeText(this,
                        "사진을 먼저 선택해 주세요.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (verificationLocation == null) {
                Toast.makeText(this,
                        "위치 인증을 먼저 완료해 주세요.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 중복 클릭 방지
            binding.btnSubmit.setEnabled(false);
            binding.btnSubmit.setAlpha(0.5f);

            // 선택한 사진을 서버에 업로드
            uploadChallengePhoto();
        });


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

                // 현재 GPS 좌표를 서버에 보내 위치 인증 요청
                requestLocationVerification();

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
                "latitude",
                verificationLocation.getLatitude()
        );

        locationData.put(
                "longitude",
                verificationLocation.getLongitude()
        );


        return locationData;
    }


    private void requestLocationVerification() {

        // 서버 인증 전에는 활동 등록 버튼 비활성화
        binding.btnSubmit.setEnabled(false);
        binding.btnSubmit.setAlpha(0.5f);
        binding.tvVerificationStatus.setText("서버 위치 인증 중...");

        Map<String, Double> locationData =
                createVerificationLocationData();

        if (locationData == null || challengeId == null) {
            binding.tvVerificationStatus.setText("위치 인증을 요청할 수 없습니다.");
            return;
        }

        ApiClient.get()
                .verifyChallengeLocation(challengeId, locationData)
                .enqueue(new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response
                    ) {
                        if (response.isSuccessful()
                                && response.body() != null) {

                            // 백엔드 응답의 verified 값 확인
                            boolean verified = Boolean.TRUE.equals(
                                    response.body().get("verified")
                            );

                            if (verified) {
                                binding.tvVerificationStatus.setText(
                                        "위치 인증 완료"
                                );

                                binding.btnSubmit.setEnabled(true);
                                binding.btnSubmit.setAlpha(1.0f);

                            } else {
                                binding.tvVerificationStatus.setText(
                                        "도전과제 인증 위치가 아닙니다."
                                );
                            }

                        } else {
                            binding.tvVerificationStatus.setText(
                                    "위치 인증 실패 (" + response.code() + ")"
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t
                    ) {
                        binding.tvVerificationStatus.setText(
                                "서버 연결에 실패했습니다."
                        );
                    }
                });
    }


    private void uploadChallengePhoto() {

        if (selectedPhotoUri == null) {
            Toast.makeText(this, "사진을 먼저 선택해 주세요.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 파일 읽기는 백그라운드 스레드에서 실행
        Uri photoUri = selectedPhotoUri;

        new Thread(() -> {
            try (InputStream inputStream =
                         getContentResolver().openInputStream(photoUri);
                 ByteArrayOutputStream outputStream =
                         new ByteArrayOutputStream()) {

                if (inputStream == null) {
                    throw new IOException("사진을 열 수 없습니다.");
                }

                byte[] buffer = new byte[8192];
                int count;

                while ((count = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, count);
                }

                byte[] imageBytes = outputStream.toByteArray();

                if (imageBytes.length == 0) {
                    throw new IOException("사진 파일이 비어 있습니다.");
                }

                String mimeType = getContentResolver().getType(photoUri);

                if (mimeType == null || !mimeType.startsWith("image/")) {
                    mimeType = "image/jpeg";
                }

                RequestBody requestBody = RequestBody.create(
                        MediaType.parse(mimeType),
                        imageBytes
                );

                MultipartBody.Part filePart =
                        MultipartBody.Part.createFormData(
                                "file",
                                "challenge_photo.jpg",
                                requestBody
                        );

                runOnUiThread(() -> {
                    ApiClient.get()
                            .uploadChallengePhoto(filePart)
                            .enqueue(new Callback<Map<String, String>>() {

                                @Override
                                public void onResponse(
                                        Call<Map<String, String>> call,
                                        Response<Map<String, String>> response
                                ) {
                                    if (response.isSuccessful()
                                            && response.body() != null) {

                                        uploadedPhotoPath =
                                                response.body().get("path");


                                        if (uploadedPhotoPath != null) {
                                            Toast.makeText(
                                                    ChallengeDetailActivity.this,
                                                    "사진 업로드 성공",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            // 사진 업로드 성공 후 도전과제 제출
                                            submitChallengeToServer();
                                        }
                                        else {
                                            Toast.makeText(
                                                    ChallengeDetailActivity.this,
                                                    "사진 경로를 받지 못했습니다.",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            binding.btnSubmit.setEnabled(true);
                                            binding.btnSubmit.setAlpha(1.0f);
                                        }

                                    } else {
                                        Toast.makeText(
                                                ChallengeDetailActivity.this,
                                                "사진 업로드 실패 (" +
                                                        response.code() + ")",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        binding.btnSubmit.setEnabled(true);
                                        binding.btnSubmit.setAlpha(1.0f);
                                    }
                                }

                                @Override
                                public void onFailure(
                                        Call<Map<String, String>> call,
                                        Throwable t
                                ) {
                                    Log.e("ChallengePhoto",
                                            "사진 업로드 오류", t);

                                    Toast.makeText(
                                            ChallengeDetailActivity.this,
                                            "사진 업로드 중 오류가 발생했습니다.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    binding.btnSubmit.setEnabled(true);
                                    binding.btnSubmit.setAlpha(1.0f);
                                }
                            });
                });

            } catch (IOException e) {
                Log.e("ChallengePhoto", "사진 읽기 실패", e);

                runOnUiThread(() ->
                        Toast.makeText(
                                ChallengeDetailActivity.this,
                                "사진을 읽을 수 없습니다.",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }
        }).start();
    }


    private void submitChallengeToServer() {

        if (uploadedPhotoPath == null || verificationLocation == null
                || challengeId == null) {
            Toast.makeText(this, "제출에 필요한 정보가 없습니다.",
                    Toast.LENGTH_SHORT).show();
            binding.btnSubmit.setEnabled(true);
            binding.btnSubmit.setAlpha(1.0f);
            return;
        }

        Map<String, Object> body = new HashMap<>();

        body.put("photoUrl", uploadedPhotoPath);
        body.put("memo", binding.etChallengeMemo.getText().toString().trim());
        body.put("latitude", verificationLocation.getLatitude());
        body.put("longitude", verificationLocation.getLongitude());

        ApiClient.get().submitChallenge(challengeId, body)
                .enqueue(new Callback<Void>() {

                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ) {
                        if (response.isSuccessful()) {
                            Toast.makeText(
                                    ChallengeDetailActivity.this,
                                    "도전과제 등록 완료!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();

                        } else {
                            Toast.makeText(
                                    ChallengeDetailActivity.this,
                                    "도전과제 등록 실패 (" + response.code() + ")",
                                    Toast.LENGTH_SHORT
                            ).show();

                            binding.btnSubmit.setEnabled(true);
                            binding.btnSubmit.setAlpha(1.0f);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("ChallengeSubmit", "도전과제 제출 오류", t);

                        Toast.makeText(
                                ChallengeDetailActivity.this,
                                "서버 연결에 실패했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();

                        binding.btnSubmit.setEnabled(true);
                        binding.btnSubmit.setAlpha(1.0f);
                    }
                });
    }

}
