package com.example.footprint.util;

import android.content.Context;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.footprint.data.model.AuthTokens;
import com.example.footprint.security.TokenManager;

/** 로그인이 필요한 사진 주소를 토큰과 함께 불러오기 위한 도우미. */
public final class AuthImage {
    private AuthImage() {}

    public static GlideUrl of(Context context, String url) {
        AuthTokens pair = new TokenManager(context).read();
        if (pair == null) return new GlideUrl(url);
        return new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + pair.accessToken).build());
    }
}
