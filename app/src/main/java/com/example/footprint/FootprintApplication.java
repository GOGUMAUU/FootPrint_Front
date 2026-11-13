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
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            public void onActivityCreated(android.app.Activity a, android.os.Bundle b) {
                if (a instanceof androidx.lifecycle.LifecycleOwner && !(a instanceof com.example.footprint.ui.LoginActivity)) {
                    TokenManager.sessionInvalidated.observe((androidx.lifecycle.LifecycleOwner) a, invalid -> {
                        if (Boolean.TRUE.equals(invalid) && !com.example.footprint.security.Session.signedIn(a)) com.example.footprint.security.Session.login(a);
                    });
                }
            }
            public void onActivityStarted(android.app.Activity a) {}
            public void onActivityResumed(android.app.Activity a) {
                if (!(a instanceof com.example.footprint.ui.LoginActivity) && !com.example.footprint.security.Session.signedIn(a)) com.example.footprint.security.Session.login(a);
            }
            public void onActivityPaused(android.app.Activity a) {}
            public void onActivityStopped(android.app.Activity a) {}
            public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) {}
            public void onActivityDestroyed(android.app.Activity a) {}
        });
    }
}
