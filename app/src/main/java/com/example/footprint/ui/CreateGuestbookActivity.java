package com.example.footprint.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.Guestbook;
import com.example.footprint.databinding.ActivityCreateGuestbookBinding;
import com.example.footprint.util.AppConfig;
import com.example.footprint.security.TokenManager;
import com.example.footprint.util.ImageCompressor;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.shape.DotPoints;
import com.kakao.vectormap.shape.Polygon;
import com.kakao.vectormap.shape.PolygonOptions;
import com.kakao.vectormap.shape.ShapeLayer;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class CreateGuestbookActivity extends AppCompatActivity {
    private ActivityCreateGuestbookBinding binding;
    private KakaoMap kakaoMap;
    private LabelLayer labelLayer;
    private ShapeLayer shapeLayer;
    private Label selectedMarker;
    private LatLng selected;
    private Location currentLocation;
    private Polygon allowedCircle;
    private boolean cameraMoved;
    private FusedLocationProviderClient fusedClient;
    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> loadCurrentLocation(null));
    private static final int MAX_PHOTOS = 3;
    private final List<Uri> photos = new ArrayList<>();
    private Uri cameraUri;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean saving;
    private String selectedName;
    private int geoSeq;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateGuestbookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (new TokenManager(this).read() == null) {
            Toast.makeText(this, "로그인 후에 방명록을 남길 수 있어요.", Toast.LENGTH_LONG).show();
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding.createMapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
            }

            @Override
            public void onMapError(Exception error) {
                Toast.makeText(CreateGuestbookActivity.this, "지도 초기화 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap map) {
                onKakaoMapReady(map);
            }
        });

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

    private void onKakaoMapReady(KakaoMap map) {
        kakaoMap = map;
        labelLayer = map.getLabelManager().getLayer();
        shapeLayer = map.getShapeManager().getLayer();
        showMyLocationOnMap();

        map.setOnMapClickListener((clickedMap, coord, screenPoint, poi) -> {
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
            if (selectedMarker == null) {
                selectedMarker = labelLayer.addLabel(
                        LabelOptions.from("selected-point", coord)
                );
            } else {
                selectedMarker.moveTo(coord);
            }

            binding.tvSelectedLocation.setText(String.format(Locale.KOREA, "선택 위치: %.5f, %.5f (내 위치에서 %dm)",
                    coord.getLatitude(), coord.getLongitude(), (int) meters));


            final String poiName = (poi != null && poi.getName() != null && !poi.getName().isEmpty())
                    ? poi.getName() : null;
            selectedName = poiName;
            if (poiName != null) {
                binding.tvSelectedLocation.setText("선택 위치: " + poiName + " (내 위치에서 " + (int) meters + "m)");
            }
            final int seq = ++geoSeq;
            final int distM = (int) meters;
            io.execute(() -> {
                try {
                    Response<Map<String, String>> r = ApiClient.get()
                            .reverseGeocode(coord.getLatitude(), coord.getLongitude()).execute();
                    final String disp = (r.isSuccessful() && r.body() != null) ? r.body().get("display") : null;
                    runOnUiThread(() -> {
                        if (seq != geoSeq || poiName != null || disp == null || disp.isEmpty()) return;
                        selectedName = disp;
                        binding.tvSelectedLocation.setText("선택 위치: " + disp + " (내 위치에서 " + distM + "m)");
                    });
                } catch (Exception ignored) {
                }
            });
        });
    }

    private void save() {
        String text = binding.etContent.getText().toString().trim();
        if (selected == null) { Toast.makeText(this, "지도에서 장소를 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (photos.isEmpty()) { Toast.makeText(this, "사진을 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (text.isEmpty()) { Toast.makeText(this, "짧은 글을 입력하세요.", Toast.LENGTH_SHORT).show(); return; }
        submit(text);
    }

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
            final double lat = selected.getLatitude();
            final double lng = selected.getLongitude();
            final double curLat = loc.getLatitude();
            final double curLng = loc.getLongitude();
            final boolean isPrivate = binding.rbPrivate.isChecked();
            final String locName = selectedName == null ? null
                    : (selectedName.length() > 100 ? selectedName.substring(0, 100) : selectedName);

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
                    body.put("visibility", isPrivate ? "PRIVATE" : "PUBLIC");
                    body.put("photoPaths", paths);
                    if (locName != null) body.put("locationName", locName);

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

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocation() {
        if (!com.example.footprint.security.Session.signedIn(this)) { com.example.footprint.security.Session.login(this); return; }
        if (!com.example.footprint.security.Session.locationConsent) { LocationConsent.request(this, this::requestLocation); return; }
        if (hasLocationPermission()) { loadCurrentLocation(null); return; }
        locationPermission.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    private void loadCurrentLocation(Consumer<Location> done) {
        if (!com.example.footprint.security.Session.signedIn(this) || !com.example.footprint.security.Session.locationConsent) { if (done != null) done.accept(null); return; }
        if (!hasLocationPermission()) { if (done != null) done.accept(null); return; }
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                    .addOnSuccessListener(loc -> {
                        if (!com.example.footprint.security.Session.signedIn(this) || !com.example.footprint.security.Session.locationConsent) return;
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
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), p.getLatitude(), p.getLongitude(), r);
        return r[0];
    }

    private void showMyLocationOnMap() {
        if (kakaoMap == null || currentLocation == null || shapeLayer == null) return;
        LatLng me = LatLng.from(currentLocation.getLatitude(), currentLocation.getLongitude());
        if (allowedCircle != null) {
            shapeLayer.remove(allowedCircle);
        }
        allowedCircle = shapeLayer.addPolygon(
                PolygonOptions.from(DotPoints.fromCircle(me, (float) AppConfig.CREATE_ALLOWED_RADIUS_METERS), 0x222457E6)
        );
        if (!cameraMoved) {
            cameraMoved = true;
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(me, 16));
        }
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
