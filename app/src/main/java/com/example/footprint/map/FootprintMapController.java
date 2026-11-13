package com.example.footprint.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.footprint.data.model.Guestbook;
import com.example.footprint.util.AppConfig;
import com.example.footprint.util.GuestbookMarkerRenderer;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelLayerOptions;
import com.kakao.vectormap.label.LabelManager;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공통 지도 컴포넌트.
 * 지도에 "그리는 것"만 담당하고, 서버 호출·모드 전환은 각 화면(Activity)이 맡는다.
 *
 * 사용 예:
 *   map = new FootprintMapController(this, binding.mapView);
 *   map.setOnGuestbookClickListener(g -> ...);
 *   map.setOnCameraIdleListener((center, zoom) -> ...);
 *   map.start(() -> { ... 지도 준비 완료 ... });
 *   // onResume → map.resume(), onPause → map.pause()
 */
public class FootprintMapController {

    public interface OnReadyListener { void onReady(); }
    public interface OnGuestbookClickListener { void onGuestbookClick(@NonNull Guestbook guestbook); }
    public interface OnCameraIdleListener { void onCameraIdle(@NonNull LatLng center, int zoomLevel); }

    private static final String TAG = "FootprintMap";
    private static final String MY_LOCATION_LAYER_ID = "my_location";
    private static final String MY_LOCATION_LABEL_ID = "me";
    private static final long SUPPRESS_IDLE_MS = 700L;

    private final Context context;
    private final MapView mapView;

    private KakaoMap kakaoMap;
    private LabelLayer guestbookLayer;
    private LabelLayer myLocationLayer;
    private Label myLocationLabel;
    private Bitmap blueDotBitmap;

    private final Map<String, Guestbook> guestbookByLabelId = new HashMap<>();

    @Nullable private OnGuestbookClickListener guestbookClickListener;
    @Nullable private OnCameraIdleListener cameraIdleListener;

    private long suppressIdleUntilMs = 0L;

    // 반경 제한 (주변 모드 등). limitCenter == null 이면 제한 없음
    @Nullable private LatLng limitCenter;
    private double limitRadiusMeters;

    public FootprintMapController(@NonNull Context context, @NonNull MapView mapView) {
        this.context = context;
        this.mapView = mapView;
    }

    // ───────────────────────── 생명주기 ─────────────────────────

    public void start(@Nullable OnReadyListener readyListener) {
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                kakaoMap = null;
            }

            @Override
            public void onMapError(Exception error) {
                Log.e(TAG, "onMapError: " + error.getMessage(), error);
                Toast.makeText(context, "지도 초기화 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                setup(map);
                if (readyListener != null) readyListener.onReady();
            }
        });
    }

    public void resume() { mapView.resume(); }

    public void pause() { mapView.pause(); }

    public boolean isReady() { return kakaoMap != null; }

    private void setup(KakaoMap map) {
        kakaoMap = map;
        LabelManager labelManager = map.getLabelManager();
        guestbookLayer = labelManager.getLayer();
        myLocationLayer = labelManager.addLayer(
                LabelLayerOptions.from(MY_LOCATION_LAYER_ID).setZOrder(10001));

        map.setOnLabelClickListener((clickedMap, layer, label) -> {
            Guestbook g = guestbookByLabelId.get(label.getLabelId());
            if (g == null) return false;
            if (g.isClustered()) {
                zoomIntoCluster(g);
            } else if (guestbookClickListener != null) {
                guestbookClickListener.onGuestbookClick(g);
            }
            return true;
        });

        map.setOnCameraMoveEndListener((movedMap, cameraPosition, gestureType) -> handleCameraIdle());
    }

    // ───────────────────────── 리스너 ─────────────────────────

    public void setOnGuestbookClickListener(@Nullable OnGuestbookClickListener l) { guestbookClickListener = l; }

    public void setOnCameraIdleListener(@Nullable OnCameraIdleListener l) { cameraIdleListener = l; }

    // ───────────────────────── 카메라 ─────────────────────────

    /** 코드로 카메라 이동. 이동 직후의 idle 이벤트는 무시된다(불필요한 재조회 방지). */
    public void moveCamera(double lat, double lng, int zoomLevel) {
        moveCameraInternal(lat, lng, zoomLevel, true);
    }

    /** 현재 줌 유지하고 이동 */
    public void moveCamera(double lat, double lng) {
        moveCamera(lat, lng, getZoomLevel());
    }

    private void moveCameraInternal(double lat, double lng, int zoomLevel, boolean suppressIdle) {
        if (kakaoMap == null) return;
        if (suppressIdle) suppressIdleUntilMs = SystemClock.elapsedRealtime() + SUPPRESS_IDLE_MS;
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng), zoomLevel));
    }

    public int getZoomLevel() {
        return kakaoMap == null ? 0 : kakaoMap.getCameraPosition().getZoomLevel();
    }

    @Nullable
    public LatLng getCenter() {
        return kakaoMap == null ? null : kakaoMap.getCameraPosition().getPosition();
    }

    /** 줌 범위 제한 (SDK 허용 범위 안으로 자동 보정) */
    public void setZoomRange(int minLevel, int maxLevel) {
        if (kakaoMap == null) return;
        kakaoMap.setCameraMinLevel(Math.max(kakaoMap.getMinZoomLevel(), minLevel));
        kakaoMap.setCameraMaxLevel(Math.min(kakaoMap.getMaxZoomLevel(), maxLevel));
    }

    public void resetZoomRange() {
        if (kakaoMap == null) return;
        kakaoMap.setCameraMinLevel(kakaoMap.getMinZoomLevel());
        kakaoMap.setCameraMaxLevel(kakaoMap.getMaxZoomLevel());
    }

    /** 현재 화면 영역 추정 [minLat, minLng, maxLat, maxLng] */
    @Nullable
    public double[] estimateVisibleBounds() {
        if (kakaoMap == null) return null;
        LatLng center = getCenter();
        int zoom = getZoomLevel();
        double latScale = Math.max(0.03, 20.0 / Math.pow(2.0, Math.max(1, zoom - 6)));
        double lngScale = latScale / Math.max(0.2, Math.cos(Math.toRadians(center.getLatitude())));
        return new double[]{
                center.getLatitude() - latScale,
                center.getLongitude() - lngScale,
                center.getLatitude() + latScale,
                center.getLongitude() + lngScale
        };
    }

    // ───────────────────────── 반경 제한 ─────────────────────────

    /** 카메라 중심이 (lat,lng)에서 radiusMeters 밖으로 못 나가게 함 */
    public void setRadiusLimit(double lat, double lng, double radiusMeters) {
        limitCenter = LatLng.from(lat, lng);
        limitRadiusMeters = radiusMeters;
    }

    public void clearRadiusLimit() { limitCenter = null; }

    private void handleCameraIdle() {
        if (kakaoMap == null) return;
        if (SystemClock.elapsedRealtime() < suppressIdleUntilMs) return;
        if (enforceRadiusLimit()) return;
        LatLng center = getCenter();
        if (cameraIdleListener != null && center != null) {
            cameraIdleListener.onCameraIdle(center, getZoomLevel());
        }
    }

    /** 반경 밖이면 경계로 끌어오고 true 반환 */
    private boolean enforceRadiusLimit() {
        if (limitCenter == null || kakaoMap == null) return false;
        LatLng center = getCenter();
        float[] r = new float[2]; // [0]=거리, [1]=방위각
        Location.distanceBetween(
                limitCenter.getLatitude(), limitCenter.getLongitude(),
                center.getLatitude(), center.getLongitude(), r);
        if (r[0] <= limitRadiusMeters) return false;

        LatLng clamped = projectFrom(limitCenter.getLatitude(), limitCenter.getLongitude(),
                limitRadiusMeters, r[1]);
        moveCamera(clamped.getLatitude(), clamped.getLongitude(), getZoomLevel());
        return true;
    }

    private static LatLng projectFrom(double lat, double lng, double meters, double bearingDegrees) {
        double earthRadius = 6371000.0;
        double angular = meters / earthRadius;
        double bearing = Math.toRadians(bearingDegrees);
        double lat1 = Math.toRadians(lat);
        double lng1 = Math.toRadians(lng);
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(angular)
                + Math.cos(lat1) * Math.sin(angular) * Math.cos(bearing));
        double lng2 = lng1 + Math.atan2(Math.sin(bearing) * Math.sin(angular) * Math.cos(lat1),
                Math.cos(angular) - Math.sin(lat1) * Math.sin(lat2));
        return LatLng.from(Math.toDegrees(lat2), Math.toDegrees(lng2));
    }

    // ───────────────────────── 내 위치(파란 점) ─────────────────────────

    public void showMyLocation(@NonNull Location loc) {
        if (myLocationLayer == null) return;
        LatLng pos = LatLng.from(loc.getLatitude(), loc.getLongitude());
        if (myLocationLabel == null) {
            if (blueDotBitmap == null) blueDotBitmap = createBlueDot();
            myLocationLabel = myLocationLayer.addLabel(
                    LabelOptions.from(MY_LOCATION_LABEL_ID, pos)
                            .setStyles(LabelStyle.from(blueDotBitmap).setAnchorPoint(0.5f, 0.5f)));
        } else {
            myLocationLabel.moveTo(pos);
        }
    }

    public void hideMyLocation() {
        if (myLocationLayer != null) myLocationLayer.removeAll();
        myLocationLabel = null;
    }

    private Bitmap createBlueDot() {
        float density = context.getResources().getDisplayMetrics().density;
        int size = (int) (18 * density);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = size / 2f;
        p.setColor(Color.WHITE);
        c.drawCircle(r, r, r, p);
        p.setColor(Color.parseColor("#1E88E5"));
        c.drawCircle(r, r, r - 3 * density, p);
        return bmp;
    }

    // ───────────────────────── 방명록 말풍선 ─────────────────────────

    /** 기존 말풍선을 지우고 list를 새로 그림 (필터링은 호출하는 쪽에서) */
    public void showGuestbooks(@Nullable List<Guestbook> list) {
        clearGuestbooks();
        if (list == null || guestbookLayer == null) return;

        for (Guestbook g : list) {
            String labelId = "guestbook-" + g.getId();
            Bitmap bitmap = GuestbookMarkerRenderer.createMarkerBitmap(
                    context, g.getShortText(), g.isRepresentative() || g.isClustered());
            Label label = guestbookLayer.addLabel(
                    LabelOptions.from(labelId, LatLng.from(g.getLatitude(), g.getLongitude()))
                            // 말풍선 꼬리 끝(아래 가운데)이 실제 좌표를 가리키도록
                            .setStyles(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1.0f)));
            if (label != null) guestbookByLabelId.put(labelId, g);
        }
    }

    public void clearGuestbooks() {
        guestbookByLabelId.clear();
        if (guestbookLayer != null) guestbookLayer.removeAll();
    }

    /** 클러스터 클릭 → 줌인. 줌이 바뀌니 idle 이벤트를 막지 않아서 자동 재조회됨 */
    private void zoomIntoCluster(Guestbook g) {
        int maxZoom = AppConfig.CLUSTER_MAX_ZOOM; // 서버가 이 줌부터 낱개로 내려줌
        int zoom = Math.min(getZoomLevel() + 2, maxZoom);
        moveCameraInternal(g.getLatitude(), g.getLongitude(), zoom, false);
    }
}