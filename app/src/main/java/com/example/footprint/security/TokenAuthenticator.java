package com.example.footprint.security;

import com.example.footprint.api.AuthApi;
import com.example.footprint.data.model.AuthTokens;
import java.io.IOException;
import java.util.Collections;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public final class TokenAuthenticator implements Authenticator {
    private final TokenManager store;
    private final AuthApi refreshApi;
    public TokenAuthenticator(TokenManager store, AuthApi refreshApi) {
        this.store = store;
        this.refreshApi = refreshApi;
    }

    @Override public Request authenticate(Route route, Response response) throws IOException {
        if (response.request().header("Authorization") == null) return null;
        String errorCode = AuthErrorPolicy.code(response.peekBody(16 * 1024).string());
        synchronized (store) {
            AuthTokens current = store.read();
            if (current == null) return null;
            boolean sameToken = ("Bearer " + current.accessToken).equals(response.request().header("Authorization"));
            if (AuthErrorPolicy.requiresLogin(errorCode)) {
                if (sameToken) store.clear();
                return null;
            }
            if (!AuthErrorPolicy.canRefresh(errorCode)) return null;
            if (response.priorResponse() != null) {
                if (sameToken) store.clear();
                return null;
            }
            if (!("Bearer " + current.accessToken).equals(response.request().header("Authorization"))) {
                return response.request().newBuilder().header("Authorization", "Bearer " + current.accessToken).build();
            }
            retrofit2.Response<AuthTokens> refreshed;
            long sessionGeneration = Session.generation();
            try {
                refreshed = refreshApi.refresh(Collections.singletonMap("refreshToken", current.refreshToken)).execute();
            } catch (IOException | RuntimeException failure) {
                // The server may already have rotated the token. Never resend this token.
                store.clear();
                return null;
            }
            if (!refreshed.isSuccessful()) {
                if (refreshed.errorBody() != null) refreshed.errorBody().close();
                store.clear();
                return null;
            }
            AuthTokens next = refreshed.body();
            if (next == null || !next.isValid()) { store.clear(); return null; }
            synchronized (Session.class) {
                if (sessionGeneration != Session.generation()) return null;
                try { store.save(next); }
                catch (IllegalStateException e) { store.clear(); return null; }
            }
            return response.request().newBuilder().header("Authorization", "Bearer " + next.accessToken).build();
        }
    }
}
