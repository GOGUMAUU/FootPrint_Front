package com.example.footprint.data.repository;

import com.example.footprint.BuildConfig;
import com.example.footprint.api.AuthApi;
import com.example.footprint.data.model.AuthTokens;
import com.example.footprint.security.TokenManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class AuthRepository {
    public interface Result { void complete(String error); }
    private final TokenManager store;
    private final AuthApi api = new Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi.class);
    public AuthRepository(TokenManager store) { this.store = store; }
    public Call<?> login(String email, String password, Result result) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email); body.put("password", password);
        Call<AuthTokens> call = api.login(body);
        call.enqueue(new Callback<AuthTokens>() {
            @Override public void onResponse(Call<AuthTokens> c, Response<AuthTokens> response) {
                if (!response.isSuccessful()) { result.complete(error(response.code())); return; }
                if (response.body() == null || !response.body().isValid()) { result.complete("인증 응답 형식을 확인해 주세요."); return; }
                try { store.save(response.body()); result.complete(null); }
                catch (IllegalStateException e) { result.complete("인증 정보를 안전하게 저장하지 못했습니다."); }
            }
            @Override public void onFailure(Call<AuthTokens> c, Throwable t) { if (!c.isCanceled()) result.complete("네트워크 연결을 확인해 주세요."); }
        });
        return call;
    }
    public Call<?> signup(String email, String password, String nickname, boolean marketing, Result result) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email); body.put("password", password); body.put("nickname", nickname);
        body.put("consents", java.util.Collections.emptyList());
        Call<Void> call = api.signup(body);
        call.enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { result.complete(r.isSuccessful() ? null : error(r.code())); }
            @Override public void onFailure(Call<Void> c, Throwable t) { if (!c.isCanceled()) result.complete("네트워크 연결을 확인해 주세요."); }
        });
        return call;
    }
    private static String error(int status) {
        switch (status) {
            case 400: return "입력값을 확인해 주세요.";
            case 401: return "이메일 또는 비밀번호를 확인해 주세요.";
            case 403: return "접근 권한이 없습니다.";
            case 409: return "이미 가입된 이메일 또는 닉네임입니다.";
            default: return "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
        }
    }
}
