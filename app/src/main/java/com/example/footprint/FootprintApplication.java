package com.example.footprint;

import android.app.Application;
import com.example.footprint.api.ApiClient;
import com.example.footprint.security.TokenManager;
import com.kakao.vectormap.KakaoMapSdk;

public final class FootprintApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY);
        ApiClient.initialize(new TokenManager(this));
    }
}
