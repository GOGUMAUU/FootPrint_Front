package com.example.footprint;

import android.app.Application;
import com.example.footprint.api.ApiClient;
import com.example.footprint.security.TokenManager;

public final class FootprintApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        ApiClient.initialize(new TokenManager(this));
    }
}
