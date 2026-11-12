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
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.Guestbook;
import com.example.footprint.util.ImageCompressor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.json.JSONObject;
import retrofit2.Response;

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
    private static final int MAX_PHOTOS = 3;
    private final List<Uri> photos = new ArrayList<>();
    private Uri cameraUri;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean saving;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotos = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS), uris -> {
                for (Uri u : uris) {
                    if (photos.size() >= MAX_PHOTOS) break;
                    photos.add(u);
                }
                renderPhotos();
            });

    private final ActivityResultLauncher<Uri> takePhoto = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && cameraUri != null && photos.size() < MAX_PHOTOS) {
                    photos.add(cameraUri);
                    renderPhotos();
                }
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateGuestbookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.createMapView.onCreate(savedInstanceState);
        binding.createMapView.getMapAsync(this);
        binding.btnPickPhoto.setOnClickListener(v -> {
            if (photos.size() >= MAX_PHOTOS) {
                Toast.makeText(this, "사진은 최대 " + MAX_PHOTOS + "장까지예요.", Toast.LENGTH_SHORT).show();
                return;
            }
            pickPhotos.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        });
        binding.btnTakePhoto.setOnClickListener(v -> openCamera());
        renderPhotos();
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
        if (photos.isEmpty()) { Toast.makeText(this, "사진을 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (text.isEmpty()) { Toast.makeText(this, "짧은 글을 입력하세요.", Toast.LENGTH_SHORT).show(); return; }
        submit(text);
    }
    // ---------- 사진 ----------
    private void openCamera() {
        if (photos.size() >= MAX_PHOTOS) {
            Toast.makeText(this, "사진은 최대 " + MAX_PHOTOS + "장까지예요.", Toast.LENGTH_SHORT).show();
            return;
        }
        File dir = new File(getCacheDir(), "images");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "cam_" + System.currentTimeMillis() + ".jpg");
        cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        takePhoto.launch(cameraUri);
    }

    /** photos 목록을 썸네일로 다시 그리고 "사진 n/3" 문구를 갱신합니다. */
    private void renderPhotos() {
        binding.photoContainer.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        int size = (int) (96 * d);
        int pad = (int) (6 * d);
        for (Uri uri : photos) {
            FrameLayout cell = new FrameLayout(this);
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(size, size);
            cellLp.setMarginEnd((int) (8 * d));
            cell.setLayoutParams(cellLp);

            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(uri).into(img);
            cell.addView(img, new FrameLayout.LayoutParams(size, size));

            TextView x = new TextView(this);
            x.setText("✕");
            x.setTextColor(0xFFFFFFFF);
            x.setBackgroundColor(0x99000000);
            x.setPadding(pad, pad, pad, pad);
            x.setOnClickListener(v -> { photos.remove(uri); renderPhotos(); });
            cell.addView(x, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END));

            binding.photoContainer.addView(cell);
        }
        binding.tvPhotoCount.setText("사진 " + photos.size() + "/" + MAX_PHOTOS);
    }
    // ---------- 저장 ----------
    /** 현재 위치를 다시 확인 → 사진 압축·업로드 → 방명록 저장. */
    private void submit(String text) {
        if (saving) return;
        setSaving(true);
        loadCurrentLocation(loc -> {
            if (loc == null) {
                fail("현재 위치를 확인하지 못했어요. 위치 권한과 GPS를 확인하세요.");
                return;
            }
            currentLocation = loc;
            float meters = distanceTo(selected);
            if (meters > AppConfig.CREATE_ALLOWED_RADIUS_METERS) {
                fail("내 위치에서 " + (int) AppConfig.CREATE_ALLOWED_RADIUS_METERS
                        + "m 이내만 작성할 수 있어요. (지금 " + (int) meters + "m)");
                return;
            }
            final List<Uri> toUpload = new ArrayList<>(photos);
            final double lat = selected.latitude;
            final double lng = selected.longitude;
            final double curLat = loc.getLatitude();
            final double curLng = loc.getLongitude();

            io.execute(() -> {
                try {
                    List<String> paths = new ArrayList<>();
                    for (Uri uri : toUpload) paths.add(uploadOne(uri));

                    Map<String, Object> body = new HashMap<>();
                    body.put("latitude", lat);
                    body.put("longitude", lng);
                    body.put("currentLatitude", curLat);
                    body.put("currentLongitude", curLng);
                    body.put("content", text);
                    body.put("visibility", "PUBLIC");
                    body.put("photoPaths", paths);

                    Response<Guestbook> res = ApiClient.get().createGuestbook(body).execute();
                    if (!res.isSuccessful()) throw new IllegalStateException(errorMessage(res));

                    runOnUiThread(() -> {
                        Toast.makeText(this, "방명록을 남겼어요!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                } catch (IllegalStateException e) {
                    runOnUiThread(() -> fail(e.getMessage()));
                } catch (Exception e) {
                    runOnUiThread(() -> fail("서버에 연결할 수 없어요. 인터넷과 서버 주소를 확인하세요."));
                }
            });
        });
    }

    /** 사진 1장을 줄여서 서버에 올리고, 서버가 알려준 경로를 돌려줍니다. (백그라운드 스레드에서 호출) */
    private String uploadOne(Uri uri) throws IOException {
        File file;
        try {
            file = ImageCompressor.compress(this, uri);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("사진을 처리하지 못했어요.");
        }
        try {
            RequestBody rb = RequestBody.create(MediaType.parse("image/jpeg"), file);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", "photo.jpg", rb);
            Response<Map<String, String>> res = ApiClient.get().uploadPhoto(part, null).execute();
            if (!res.isSuccessful() || res.body() == null || res.body().get("path") == null) {
                throw new IllegalStateException(errorMessage(res));
            }
            return res.body().get("path");
        } finally {
            file.delete();
        }
    }

    /** 서버 에러 응답의 message를 꺼냅니다. 없으면 코드만 알려줍니다. */
    private String errorMessage(Response<?> res) {
        try {
            if (res.errorBody() != null) {
                String msg = new JSONObject(res.errorBody().string()).optString("message", "");
                if (!msg.isEmpty()) return msg;
            }
        } catch (Exception ignored) {
        }
        return "서버 오류가 났어요. (코드 " + res.code() + ")";
    }

    private void setSaving(boolean s) {
        saving = s;
        binding.btnSave.setEnabled(!s);
        if (s) Toast.makeText(this, "저장 중이에요...", Toast.LENGTH_SHORT).show();
    }

    private void fail(String message) {
        setSaving(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
