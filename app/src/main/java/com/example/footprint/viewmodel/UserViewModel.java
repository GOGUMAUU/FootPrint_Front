package com.example.footprint.viewmodel;

import androidx.lifecycle.*;
import com.example.footprint.data.model.UserProfile;
import com.example.footprint.data.repository.ProfileRepository;
import retrofit2.Call;

public final class UserViewModel extends ViewModel {
    public final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    public final MutableLiveData<String> error = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final ProfileRepository repository = new ProfileRepository();
    private Call<?> pending;
    private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private volatile boolean cleared;
    private final ProfileRepository.Result result = (user, message) -> {
        busy.setValue(false); error.setValue(message == null ? "" : message);
        if (user != null) profile.setValue(user);
    };
    public void load(long id) { if (Boolean.TRUE.equals(busy.getValue())) return; busy.setValue(true); pending = repository.load(id, result); }
    public void save(String nickname, boolean isPrivate) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        if (nickname.trim().isEmpty()) { error.setValue("닉네임을 입력해 주세요."); return; }
        busy.setValue(true); pending = repository.update(nickname.trim(), isPrivate, result);
    }
    public void mockImage(String uri) { repository.mockImage(uri, result); }
    public void relationship(String action, long id) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        if (ProfileRepository.mock()) {
            try {
                switch (action) {
                    case "follow": com.example.footprint.data.repository.MockRelations.follow(id); break;
                    case "unfollow": com.example.footprint.data.repository.MockRelations.unfollow(id); break;
                    case "block": com.example.footprint.data.repository.MockRelations.block(id); break;
                    case "unblock": com.example.footprint.data.repository.MockRelations.unblock(id); break;
                }
                busy.setValue(false); load(id);
            } catch (IllegalStateException e) { result.complete(null, e.getMessage()); }
            return;
        }
        com.example.footprint.api.RelationshipApi api = com.example.footprint.api.ApiClient.create(com.example.footprint.api.RelationshipApi.class);
        Call<?> call = action.equals("follow") ? api.follow(id) : action.equals("unfollow") ? api.unfollow(id) : action.equals("block") ? api.block(id) : api.unblock(id);
        relationCall(call, id);
    }
    private <T> void relationCall(Call<T> call, long id) {
        pending = call;
        call.enqueue(new retrofit2.Callback<T>() {
            public void onResponse(Call<T> c, retrofit2.Response<T> r) {
                if (!r.isSuccessful()) { result.complete(null, "관계 변경 실패 (" + r.code() + ")"); return; }
                busy.setValue(false); load(id);
            }
            public void onFailure(Call<T> c, Throwable t) { if (!c.isCanceled()) result.complete(null, "관계 변경 결과를 확인하지 못했습니다. 새로고침해 주세요."); }
        });
    }
    public void upload(android.content.ContentResolver resolver, android.net.Uri uri) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        worker.execute(() -> {
            try {
                String mime = resolver.getType(uri);
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                try (java.io.InputStream input = resolver.openInputStream(uri)) {
                    if (input == null) throw new java.io.IOException();
                    byte[] buffer = new byte[8192]; int read;
                    while ((read = input.read(buffer)) != -1) {
                        if (output.size() + read > com.example.footprint.util.ProfileImagePolicy.MAX_BYTES) throw new java.io.IOException();
                        output.write(buffer, 0, read);
                    }
                }
                byte[] bytes = output.toByteArray();
                if (!com.example.footprint.util.ProfileImagePolicy.accepts(mime, bytes.length)) throw new java.io.IOException();
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options(); options.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                if (options.outWidth <= 0 || options.outHeight <= 0 || !mime.equals(options.outMimeType)) throw new java.io.IOException();
                handler.post(() -> {
                    if (cleared) return;
                    if (ProfileRepository.mock()) {
                        // Retain picker permission so a later profile read can display the image.
                        try { resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                        catch (SecurityException ignored) {}
                        repository.mockImage(uri.toString(), result);
                    } else {
                        okhttp3.MultipartBody.Part part = okhttp3.MultipartBody.Part.createFormData("file", "profile", okhttp3.RequestBody.create(okhttp3.MediaType.parse(mime), bytes));
                        imageCall(com.example.footprint.api.ApiClient.create(com.example.footprint.api.UserApi.class).uploadImage(part));
                    }
                });
            } catch (Exception e) { handler.post(() -> { if (!cleared) result.complete(null, "JPEG·PNG·WebP 이미지(5MiB 이하)를 선택해 주세요."); }); }
        });
    }
    public void deleteImage() {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        if (ProfileRepository.mock()) repository.mockImage(null, result);
        else imageCall(com.example.footprint.api.ApiClient.create(com.example.footprint.api.UserApi.class).deleteImageResponse());
    }
    private void imageCall(retrofit2.Call<com.google.gson.JsonObject> call) {
        pending = call;
        call.enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
            public void onResponse(retrofit2.Call<com.google.gson.JsonObject> c, retrofit2.Response<com.google.gson.JsonObject> r) {
                if (!r.isSuccessful() || r.body() == null || !r.body().has("profileImageUrl")) { result.complete(null, "사진 변경 실패. 기존 사진을 유지합니다."); return; }
                busy.setValue(false); load(0);
            }
            public void onFailure(retrofit2.Call<com.google.gson.JsonObject> c, Throwable t) { if (!c.isCanceled()) result.complete(null, "사진 변경 실패. 다시 조회해 상태를 확인해 주세요."); }
        });
    }
    @Override protected void onCleared() { cleared = true; if (pending != null) pending.cancel(); worker.shutdownNow(); handler.removeCallbacksAndMessages(null); }
}
