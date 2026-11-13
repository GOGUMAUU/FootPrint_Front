package com.example.footprint.security;

import com.google.gson.Gson;

public final class AuthErrorPolicy {
    private AuthErrorPolicy() {}
    private static final class ErrorBody { String code; }
    public static String code(String json) {
        try {
            ErrorBody error = new Gson().fromJson(json, ErrorBody.class);
            return error == null ? null : error.code;
        } catch (RuntimeException ignored) { return null; }
    }
    public static boolean canRefresh(String code) { return "ACCESS_TOKEN_EXPIRED".equals(code); }
    public static boolean requiresLogin(String code) {
        return "SESSION_REVOKED".equals(code) || "REFRESH_TOKEN_INVALID".equals(code)
                || "REFRESH_TOKEN_EXPIRED".equals(code) || "REFRESH_TOKEN_REUSED".equals(code);
    }
}
