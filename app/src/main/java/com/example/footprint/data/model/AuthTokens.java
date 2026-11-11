package com.example.footprint.data.model;

public final class AuthTokens {
    public String accessToken;
    public String refreshToken;

    public boolean isValid() {
        return accessToken != null && !accessToken.trim().isEmpty()
                && refreshToken != null && !refreshToken.trim().isEmpty();
    }
}
