package com.example.footprint;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.ApiResponse;
import com.example.footprint.data.model.Guestbook;
import com.example.footprint.data.model.NearbyGuestbookResponse;
import com.example.footprint.databinding.ActivityMainBinding;
import com.example.footprint.map.FootprintMapController;
import com.example.footprint.ui.ChallengeActivity;
import com.example.footprint.ui.CreateGuestbookActivity;
import com.example.footprint.ui.FilterBottomSheetDialog;
import com.example.footprint.ui.UserSearchActivity;
import com.example.footprint.util.AppConfig;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements com.example.footprint.ui.LetterBoxSheet.OnClosedListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private ActivityMainBinding binding;
    private FootprintMapController map;
    private FusedLocationProviderClient fusedClient;

    private Location currentLocation;
    private Location lastReloadLocation;
    private LocationCallback locationCallback;
    private boolean requestingUpdates = false;

    private final Handler cameraReloadHandler = new Handler(Looper.getMainLooper());
    private final Runnable cameraReloadRunnable = this::reloadForCamera;
    private long reloadRequestVersion = 0L;
    private Call<ApiResponse<NearbyGuestbookResponse>> nearbyCall;
    private Call<List<Guestbook>> boundsCall;

    private String mode = "nearby"; // nearby | following | me
    private String lastPick; // 마지막으로 지도에 적용한 말풍선 대표 기준
    /** 주변 모드에서 카메라가 내 위치를 따라갈지. 사용자가 지도를 다른 곳으로 옮기면 false, 내 위치 버튼을 누르면 true */
    private boolean followMe = true;
    private boolean resumedOnce;

    private Long selectedFollowingUserId = null;
    private android.widget.Button profileButton;

    private com.example.footprint.data.model.MapFilterState filterState = new com.example.footprint.data.model.MapFilterState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        // ── 공통 지도 컴포넌트 ──
        map = new FootprintMapController(this, binding.mapView);
        map.setOnGuestbookClickListener(this::openGuestbook);
        map.setOnClusterClickListener(this::openBoard);
        map.setOnCameraIdleListener((center, zoom) -> {
            // 코드로 옮긴 이동은 여기로 안 옴 → 사용자가 직접 움직인 것.
            // 내 위치에서 50m 넘게 떨어진 곳을 보고 있으면 더 이상 내 위치로 끌고 오지 않는다
            if (currentLocation != null) {
                float[] d = new float[1];
                Location.distanceBetween(center.getLatitude(), center.getLongitude(),
                        currentLocation.getLatitude(), currentLocation.getLongitude(), d);
                followMe = d[0] <= 50f;
            }
            scheduleReloadForCamera();
        });
        map.start(() -> switchMode("nearby"));

        binding.fabCreate.setOnClickListener(v -> com.example.footprint.ui.LocationConsent.request(this, () -> startActivity(new Intent(this, CreateGuestbookActivity.class))));
        binding.btnUsers.setOnClickListener(v -> startActivity(new Intent(this, UserSearchActivity.class)));
        /*
        android.widget.Button accountButton = new android.widget.Button(this);
        accountButton.setText("로그아웃");
        ((android.widget.LinearLayout) binding.btnUsers.getParent()).addView(accountButton);
        accountButton.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("로그아웃할까요?").setNegativeButton("취소", null).setPositiveButton("로그아웃", (d,w) -> com.example.footprint.security.Session.logout(this)).show());
        profileButton = new android.widget.Button(this);
        profileButton.setText("마이페이지");
        ((android.widget.LinearLayout) binding.btnUsers.getParent()).addView(profileButton);
        profileButton.setOnClickListener(v -> startActivity(new Intent(this, com.example.footprint.ui.UserProfileActivity.class)));
        android.widget.Button locationPolicyButton = new android.widget.Button(this);
        locationPolicyButton.setText("위치 동의 관리");
        */
        binding.btnMap.setOnClickListener(v -> switchMode("nearby"));
        binding.btnProfile.setOnClickListener(v -> startActivity(new Intent(this, com.example.footprint.ui.UserProfileActivity.class)));
        /*
        ((android.widget.LinearLayout) binding.btnUsers.getParent()).addView(locationPolicyButton);
        locationPolicyButton.setOnClickListener(v -> {
            if (!com.example.footprint.security.Session.locationConsent) { com.example.footprint.ui.LocationConsent.request(this, this::ensureLocationPermissionAndLoad); return; }
            new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("위치 동의를 철회하고 위치 기능을 중지할까요?")
                    .setNegativeButton("취소", null).setPositiveButton("철회", (d,w) -> {
                        Runnable stop = () -> {
                            com.example.footprint.security.Session.locationConsent = false;
                            currentLocation = null; map.clearGuestbooks();
                            stopLocationUpdates(); map.hideMyLocation(); map.clearRadiusLimit();
                        };
                        if (com.example.footprint.data.repository.ProfileRepository.mock()) { stop.run(); return; }
                        locationPolicyButton.setEnabled(false);
                        ApiClient.create(com.example.footprint.api.PolicyApi.class).revoke("LOCATION").enqueue(new Callback<Void>() {
                            public void onResponse(Call<Void> c, Response<Void> r) {
                                if (isDestroyed()) return;
                                locationPolicyButton.setEnabled(true);
                                if (r.isSuccessful()) stop.run(); else Toast.makeText(MainActivity.this, "철회 실패. 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                            }
                            public void onFailure(Call<Void> c, Throwable t) {
                                if (isDestroyed()) return;
                                locationPolicyButton.setEnabled(true);
                                Toast.makeText(MainActivity.this, "철회 결과를 확인하지 못했습니다.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }).show();
        });
         */
        binding.btnChallenges.setOnClickListener(v -> com.example.footprint.ui.LocationConsent.request(this, () -> startActivity(new Intent(this, ChallengeActivity.class))));
        binding.btnMyLocation.setOnClickListener(v -> goToMyLocation());
        binding.btnLetters.setOnClickListener(v -> com.example.footprint.ui.LetterBoxSheet.show(getSupportFragmentManager(),
                currentLocation == null ? null : currentLocation.getLatitude(),
                currentLocation == null ? null : currentLocation.getLongitude()));
        binding.btnNearby.setOnClickListener(v -> switchMode("nearby"));
        binding.btnFollowing.setOnClickListener(v -> switchMode("following"));
        binding.btnMyMap.setOnClickListener(v -> switchMode("me"));
        binding.btnFilter.setOnClickListener(v -> {
            com.example.footprint.ui.FilterBottomSheetDialog dialog =
                    com.example.footprint.ui.FilterBottomSheetDialog.newInstance(filterState, mode);
            dialog.setOnFilterAppliedListener(newState -> {
                this.filterState = newState;
                map.clearGuestbooks();
                scheduleReloadForCamera();
            });
            dialog.show(getSupportFragmentManager(), "FilterBottomSheet");
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ───────────────────────── 모드 ─────────────────────────

    private void switchMode(String nextMode) {
        mode = nextMode;
        updateModeButtons();
        applyModeCameraPolicy();
        map.clearGuestbooks();

        if ("nearby".equals(mode)) {
            followMe = true;
            if (currentLocation != null) {
                map.moveCamera(currentLocation.getLatitude(), currentLocation.getLongitude(), AppConfig.NEARBY_DEFAULT_ZOOM);
            } else {
                // GPS를 기다리는 동안 마지막으로 알던 내 위치를 먼저 보여 줌 (빈 지도/엉뚱한 곳 방지)
                double[] last = com.example.footprint.util.MapPrefs.lastLocation(this);
                if (last != null) map.moveCamera(last[0], last[1], AppConfig.NEARBY_DEFAULT_ZOOM);
                ensureLocationPermissionAndLoad();
            }
        } else if ("me".equals(mode)) {
            if (currentLocation != null) {
                map.moveCamera(currentLocation.getLatitude(), currentLocation.getLongitude(), (int) Math.round(AppConfig.ZOOM_REGION));
            } else {
                ensureLocationPermissionAndLoad();
                map.moveCamera(36.35, 127.8, (int) Math.round(AppConfig.ZOOM_COUNTRY));
            }
        } else {
            map.moveCamera(36.35, 127.8, (int) Math.round(AppConfig.ZOOM_COUNTRY));
        }
        scheduleReloadForCamera();
    }

    /** 내 위치로 가기: 주변 모드는 기본 줌, 그 외에는 동네가 보일 만큼은 확대해서 이동 */
    private void goToMyLocation() {
        if (!map.isReady()) return;
        if (currentLocation == null) {
            Toast.makeText(this, "내 위치를 찾는 중이에요. 잠시 후 다시 눌러 주세요.", Toast.LENGTH_SHORT).show();
            ensureLocationPermissionAndLoad();
            return;
        }
        double lat = currentLocation.getLatitude(), lng = currentLocation.getLongitude();
        int zoom = "nearby".equals(mode)
                ? AppConfig.NEARBY_DEFAULT_ZOOM
                : Math.max(map.getZoomLevel(), (int) Math.round(AppConfig.ZOOM_DETAIL));
        if ("nearby".equals(mode)) map.setRadiusLimit(lat, lng, AppConfig.NEARBY_MAX_RADIUS_METERS);
        followMe = true;
        map.moveCamera(lat, lng, zoom);
        scheduleReloadForCamera(); // moveCamera 는 idle 재조회를 막으므로 직접 다시 불러옴
    }

    /** 모드별 줌 범위 + 반경 제한 */
    private void applyModeCameraPolicy() {
        if ("nearby".equals(mode)) {
            map.setZoomRange(AppConfig.NEARBY_MIN_ZOOM, AppConfig.NEARBY_MAX_ZOOM);
            if (currentLocation != null) {
                map.setRadiusLimit(currentLocation.getLatitude(), currentLocation.getLongitude(),
                        AppConfig.NEARBY_MAX_RADIUS_METERS);
            }
        } else {
            map.resetZoomRange();
            map.clearRadiusLimit();
        }
    }

    private void updateModeButtons() {
        binding.btnNearby.setSelected("nearby".equals(mode));
        binding.btnFollowing.setSelected("following".equals(mode));
        binding.btnMyMap.setSelected("me".equals(mode));
        binding.btnMap.setSelected(!"me".equals(mode));
    }

    // ───────────────────────── 위치 ─────────────────────────

    private boolean hasLocationPermission() {
        boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        return hasFine || hasCoarse;
    }

    private void ensureLocationPermissionAndLoad() {
        if (!com.example.footprint.security.Session.signedIn(this)) return;
        if (!com.example.footprint.security.Session.locationConsent) { com.example.footprint.ui.LocationConsent.request(this, this::ensureLocationPermissionAndLoad); return; }
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (requestingUpdates || !hasLocationPermission() || !com.example.footprint.security.Session.signedIn(this) || !com.example.footprint.security.Session.locationConsent) return;

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2000L)
                .setMinUpdateDistanceMeters(5f)
                .build();

        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    Location loc = result.getLastLocation();
                    if (loc != null) onNewLocation(loc);
                }
            };
        }
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
            requestingUpdates = true;
            // 정확한 GPS 첫 위치는 몇 초 걸릴 수 있어서, 기기가 마지막으로 알던 위치로 먼저 지도를 띄움
            if (currentLocation == null) {
                fusedClient.getLastLocation().addOnSuccessListener(this, last -> {
                    if (last != null && currentLocation == null) onNewLocation(last);
                });
            }
        } catch (SecurityException ignored) {
        }
    }

    private void stopLocationUpdates() {
        if (!requestingUpdates || locationCallback == null) return;
        fusedClient.removeLocationUpdates(locationCallback);
        requestingUpdates = false;
    }

    private void onNewLocation(Location loc) {
        if (!com.example.footprint.security.Session.signedIn(this) || !com.example.footprint.security.Session.locationConsent) return;
        boolean firstFix = (currentLocation == null);
        currentLocation = loc;
        map.showMyLocation(loc);
        com.example.footprint.util.MapPrefs.saveLastLocation(this, loc.getLatitude(), loc.getLongitude());

        if ("nearby".equals(mode)) {
            // 반경 제한 기준점을 내 위치로 갱신
            map.setRadiusLimit(loc.getLatitude(), loc.getLongitude(), AppConfig.NEARBY_MAX_RADIUS_METERS);
            if (firstFix) {
                followMe = true;
                map.moveCamera(loc.getLatitude(), loc.getLongitude(), AppConfig.NEARBY_DEFAULT_ZOOM);
            } else if (followMe) {
                map.moveCamera(loc.getLatitude(), loc.getLongitude()); // 줌 유지하고 따라가기
            }
            // (사용자가 다른 곳을 보고 있으면 카메라는 그대로 두고 파란 점만 움직임)
            // 내 위치 기준 조회라서 100m 이상 움직였을 때만 재조회
            if (lastReloadLocation == null || lastReloadLocation.distanceTo(loc) > 100f) {
                lastReloadLocation = loc;
                scheduleReloadForCamera();
            }
        } else if ("me".equals(mode) && firstFix) {
            map.moveCamera(loc.getLatitude(), loc.getLongitude(), (int) Math.round(AppConfig.ZOOM_REGION));
        }
    }

    // ───────────────────────── 데이터 ─────────────────────────

    private void scheduleReloadForCamera() {
        cameraReloadHandler.removeCallbacks(cameraReloadRunnable);
        cameraReloadHandler.postDelayed(cameraReloadRunnable, 250L);
    }

    private void reloadForCamera() {
        if (!map.isReady() || !com.example.footprint.security.Session.signedIn(this) || !com.example.footprint.security.Session.locationConsent) return;
        final long requestVersion = ++reloadRequestVersion;
        final int zoom = map.getZoomLevel();

        if ("nearby".equals(mode)) {
            if (currentLocation == null) return;
            if (nearbyCall != null && !nearbyCall.isCanceled()) nearbyCall.cancel();
            nearbyCall = ApiClient.get().getNearbyGuestbooks(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    zoom,
                    com.example.footprint.util.MapPrefs.pick(this),
                    filterState.getPeriod()
            );
            nearbyCall.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<NearbyGuestbookResponse>> call,
                                       @NonNull Response<ApiResponse<NearbyGuestbookResponse>> response) {
                    if (requestVersion != reloadRequestVersion || call.isCanceled()) return;
                    if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) return;
                    renderGuestbooks(response.body().getData().getGuestbooks(), zoom);
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<NearbyGuestbookResponse>> call, @NonNull Throwable t) {
                    if (requestVersion != reloadRequestVersion || call.isCanceled()) return;
                    Toast.makeText(MainActivity.this, "주변 방명록 조회 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            double[] bounds = map.estimateVisibleBounds();
            if (bounds == null) return;
            if (boundsCall != null && !boundsCall.isCanceled()) boundsCall.cancel();
            boundsCall = ApiClient.get().getGuestbooksInBounds(
                    "following".equals(mode) ? "following" : "me",
                    null,
                    zoom,
                    bounds[0],
                    bounds[1],
                    bounds[2],
                    bounds[3],
                    filterState.getPeriod(),
                    filterState.getAgeGroup(),
                    com.example.footprint.util.MapPrefs.pick(this)
            );
            boundsCall.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<Guestbook>> call, @NonNull Response<List<Guestbook>> response) {
                    if (requestVersion != reloadRequestVersion || call.isCanceled()) return;
                    if (!response.isSuccessful() || response.body() == null) return;
                    renderGuestbooks(response.body(), zoom);
                }

                @Override
                public void onFailure(@NonNull Call<List<Guestbook>> call, @NonNull Throwable t) {
                    if (requestVersion != reloadRequestVersion || call.isCanceled()) return;
                    Toast.makeText(MainActivity.this, "영역 방명록 조회 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /** 클러스터링·대표 글 선정·정렬은 서버가 해서 내려주므로 그대로 그림 */
    private void renderGuestbooks(List<Guestbook> list, int zoom) {
        map.showGuestbooks(list);
    }

    /** 확대된 상태에서 묶인 말풍선을 누르면: 그 자리의 방명록을 게시판(바텀시트)으로 보여줌 */
    private void openBoard(Guestbook cluster) {
        com.example.footprint.ui.GuestbookBoardSheet.show(getSupportFragmentManager(), cluster.getClusterIds(),
                currentLocation == null ? null : currentLocation.getLatitude(),
                currentLocation == null ? null : currentLocation.getLongitude(),
                cluster.getLocationName(), "nearby".equals(mode));
    }

    /** 멀리서 본 클러스터 클릭(줌인)은 지도 컴포넌트가 처리하고, 여기로는 일반 방명록만 들어옴 */
    private void openGuestbook(Guestbook g) {
        if ("nearby".equals(mode) && !g.isAccessible()) {
            String distMsg = g.getDistanceMeter() != null
                    ? String.format(Locale.getDefault(), " (현재 거리: %.0fm)", g.getDistanceMeter())
                    : "";
            Toast.makeText(this, (g.isLetter() ? "편지는 그 자리에 가야 열 수 있어요." : "직접 가까이 이동해야 열람할 수 있어요.") + distMsg, Toast.LENGTH_LONG).show();
            return;
        }
        com.example.footprint.ui.GuestbookDetailDialog.open(getSupportFragmentManager(), g.getId(),
                currentLocation == null ? null : currentLocation.getLatitude(),
                currentLocation == null ? null : currentLocation.getLongitude());
    }

    // ───────────────────────── 생명주기 ─────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (hasLocationPermission()) {
                startLocationUpdates();
            } else if ("nearby".equals(mode)) {
                Toast.makeText(this, "주변 모드는 위치 권한이 필요해요.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.resume();
        if (map.isReady()) startLocationUpdates();
        refreshFollowDot();
        refreshLetterDot();
        // 마이페이지에서 '말풍선에 보일 글' 설정을 바꾸고 돌아왔으면 지도 다시 불러오기
        String pick = com.example.footprint.util.MapPrefs.pick(this);
        if (lastPick != null && !lastPick.equals(pick) && map.isReady()) {
            map.clearGuestbooks();
            scheduleReloadForCamera();
        }
        lastPick = pick;
        // 다른 화면(작성·프로필 등)에서 돌아오면 바뀐 방명록이 있을 수 있어 한 번 다시 불러옴
        if (resumedOnce && map.isReady()) scheduleReloadForCamera();
        resumedOnce = true;
    }

    @Override public void onLetterBoxClosed() { refreshLetterDot(); }

    /** 아직 안 열어 본 받은 편지가 있으면 편지함 버튼에 핑크 점 */
    private void refreshLetterDot() {
        if (!com.example.footprint.security.Session.signedIn(this)) return;
        ApiClient.get().getLetterUnreadCount().enqueue(new Callback<com.example.footprint.data.model.LetterSummary.UnreadCount>() {
            @Override public void onResponse(@NonNull Call<com.example.footprint.data.model.LetterSummary.UnreadCount> c,
                                             @NonNull Response<com.example.footprint.data.model.LetterSummary.UnreadCount> r) {
                if (isFinishing() || isDestroyed()) return;
                boolean has = r.isSuccessful() && r.body() != null && r.body().count > 0;
                binding.letterDot.setVisibility(has ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            @Override public void onFailure(@NonNull Call<com.example.footprint.data.model.LetterSummary.UnreadCount> c, @NonNull Throwable t) {}
        });
    }

    private void refreshFollowDot() {
        if (!com.example.footprint.security.Session.signedIn(this)) return;
        com.example.footprint.util.FollowNotice.refresh(this, this::applyFollowDot);
    }

    private void applyFollowDot() {
        if (profileButton == null) return;
        if (com.example.footprint.util.FollowNotice.hasNew()) {
            android.text.SpannableString s = new android.text.SpannableString("마이페이지 ●");
            s.setSpan(new android.text.style.ForegroundColorSpan(0xFFE53935), 6, 7,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            profileButton.setText(s);
        } else {
            profileButton.setText("마이페이지");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.pause();
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        cameraReloadHandler.removeCallbacks(cameraReloadRunnable);
        if (nearbyCall != null && !nearbyCall.isCanceled()) nearbyCall.cancel();
        if (boundsCall != null && !boundsCall.isCanceled()) boundsCall.cancel();
        super.onDestroy();
    }
}