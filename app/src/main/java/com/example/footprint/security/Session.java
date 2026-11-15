package com.example.footprint.security;

import android.app.Activity;
import android.content.Intent;
import com.example.footprint.data.repository.ProfileRepository;
import com.example.footprint.ui.LoginActivity;

public final class Session {
    private static boolean loggingOut;
    private static volatile long generation;
    public static long generation() { return generation; }
    public static boolean locationConsent;
    private static boolean mockLoggedIn;
    public static boolean signedIn(android.content.Context context) {
        return ProfileRepository.mock() ? mockLoggedIn : new TokenManager(context).read() != null;
    }
    public static void mockLogin() { generation++; mockLoggedIn = true; }
    public static synchronized void clear(android.content.Context context) { generation++; mockLoggedIn = false; locationConsent = false; new TokenManager(context).clear(); }
    public static void logout(Activity activity) {
        if (loggingOut) return;
        if (ProfileRepository.mock()) { clear(activity); login(activity); return; }
        com.example.footprint.data.model.AuthTokens tokens = new TokenManager(activity).read();
        if (tokens == null) { clear(activity); login(activity); return; }
        loggingOut = true;
        com.example.footprint.api.ApiClient.create(com.example.footprint.api.AuthApi.class)
                .logout(java.util.Collections.singletonMap("refreshToken", tokens.refreshToken))
                .enqueue(new retrofit2.Callback<Void>() {
                    private void finish(boolean serverSuccess) {
                        loggingOut = false; clear(activity);
                        if (!serverSuccess) android.widget.Toast.makeText(activity.getApplicationContext(), "기기에서 로그아웃했습니다. 서버 세션 폐기는 확인되지 않았습니다.", android.widget.Toast.LENGTH_LONG).show();
                        if (!activity.isDestroyed()) login(activity);
                    }
                    public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) { finish(response.isSuccessful()); }
                    public void onFailure(retrofit2.Call<Void> call, Throwable error) { finish(false); }
                });
    }
    public static void login(Activity activity) {
        activity.startActivity(new Intent(activity, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
    }
}
