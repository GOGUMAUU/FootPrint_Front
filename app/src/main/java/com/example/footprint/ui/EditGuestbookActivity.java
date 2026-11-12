package com.example.footprint.ui;

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
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.footprint.BuildConfig;
import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.GuestbookDetail;
import com.example.footprint.databinding.ActivityEditGuestbookBinding;
import com.example.footprint.util.ImageCompressor;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class EditGuestbookActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_PHOTOS = "photos";
    public static final String EXTRA_VISIBILITY = "visibility";
    public static final String EXTRA_LOCATION_NAME = "locationName";
    private static final int MAX_PHOTOS = 3;

    /** 사진 한 장: 서버에 이미 있는 것(path)이거나 새로 고른 것(uri). */
    private static class Item {
        final String path;
        final Uri uri;
        Item(String path, Uri uri) { this.path = path; this.uri = uri; }
    }

    private ActivityEditGuestbookBinding binding;
    private long guestbookId;
    private String visibility;
    private String locationName;
    private final List<Item> items = new ArrayList<>();
    private Uri cameraUri;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean saving;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotos = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS), uris -> {
                for (Uri u : uris) {
                    if (items.size() >= MAX_PHOTOS) break;
                    items.add(new Item(null, u));
                }
                renderPhotos();
            });

    private final ActivityResultLauncher<Uri> takePhoto = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && cameraUri != null && items.size() < MAX_PHOTOS) {
                    items.add(new Item(null, cameraUri));
                    renderPhotos();
                }
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditGuestbookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        guestbookId = getIntent().getLongExtra(EXTRA_ID, -1);
        visibility = getIntent().getStringExtra(EXTRA_VISIBILITY);
        locationName = getIntent().getStringExtra(EXTRA_LOCATION_NAME);
        binding.etContent.setText(getIntent().getStringExtra(EXTRA_CONTENT));
        binding.rbPrivate.setChecked("PRIVATE".equals(visibility));
        List<String> old = getIntent().getStringArrayListExtra(EXTRA_PHOTOS);
        if (old != null) {
            for (String url : old) {
                int i = url.indexOf("/files/");
                items.add(new Item(i >= 0 ? url.substring(i) : url, null));
            }
        }
        if (guestbookId < 0) { finish(); return; }

        binding.btnPickPhoto.setOnClickListener(v -> {
            if (items.size() >= MAX_PHOTOS) {
                Toast.makeText(this, "사진은 최대 " + MAX_PHOTOS + "장까지예요.", Toast.LENGTH_SHORT).show();
                return;
            }
            pickPhotos.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        });
        binding.btnTakePhoto.setOnClickListener(v -> openCamera());
        binding.btnSave.setOnClickListener(v -> save());
        renderPhotos();
    }

    private void openCamera() {
        if (items.size() >= MAX_PHOTOS) {
            Toast.makeText(this, "사진은 최대 " + MAX_PHOTOS + "장까지예요.", Toast.LENGTH_SHORT).show();
            return;
        }
        File dir = new File(getCacheDir(), "images");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "cam_" + System.currentTimeMillis() + ".jpg");
        cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        takePhoto.launch(cameraUri);
    }

    private String fullUrl(String path) {
        if (path.startsWith("http")) return path;
        return BuildConfig.API_BASE_URL + (path.startsWith("/") ? path.substring(1) : path);
    }

    private void renderPhotos() {
        binding.photoContainer.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        int size = (int) (96 * d);
        int pad = (int) (6 * d);
        for (Item item : items) {
            FrameLayout cell = new FrameLayout(this);
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(size, size);
            cellLp.setMarginEnd((int) (8 * d));
            cell.setLayoutParams(cellLp);

            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (item.path != null) Glide.with(this).load(fullUrl(item.path)).into(img);
            else Glide.with(this).load(item.uri).into(img);
            cell.addView(img, new FrameLayout.LayoutParams(size, size));

            TextView x = new TextView(this);
            x.setText("✕");
            x.setTextColor(0xFFFFFFFF);
            x.setBackgroundColor(0x99000000);
            x.setPadding(pad, pad, pad, pad);
            x.setOnClickListener(v -> { items.remove(item); renderPhotos(); });
            cell.addView(x, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END));

            binding.photoContainer.addView(cell);
        }
        binding.tvPhotoCount.setText("사진 " + items.size() + "/" + MAX_PHOTOS);
    }

    private void save() {
        if (saving) return;
        final String text = binding.etContent.getText().toString().trim();
        if (items.isEmpty()) { Toast.makeText(this, "사진은 1장 이상 필요해요.", Toast.LENGTH_SHORT).show(); return; }
        if (text.isEmpty()) { Toast.makeText(this, "짧은 글을 입력하세요.", Toast.LENGTH_SHORT).show(); return; }
        setSaving(true);

        final List<Item> snapshot = new ArrayList<>(items);
        final boolean isPrivate = binding.rbPrivate.isChecked();
        io.execute(() -> {
            try {
                List<String> paths = new ArrayList<>();
                for (Item item : snapshot) {
                    paths.add(item.path != null ? item.path : uploadOne(item.uri));
                }
                Map<String, Object> body = new HashMap<>();
                body.put("content", text);
                body.put("photoPaths", paths);
                body.put("visibility", isPrivate ? "PRIVATE" : "PUBLIC");
                if (locationName != null) body.put("locationName", locationName);

                Response<GuestbookDetail> res = ApiClient.get().updateGuestbook(guestbookId, body).execute();
                if (!res.isSuccessful()) throw new IllegalStateException(errorMessage(res));

                runOnUiThread(() -> {
                    Toast.makeText(this, "방명록을 수정했어요.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (IllegalStateException e) {
                runOnUiThread(() -> fail(e.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> fail("서버에 연결할 수 없어요. 인터넷과 서버 주소를 확인하세요."));
            }
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
}
