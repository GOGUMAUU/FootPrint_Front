package com.example.footprint.api;

import com.example.footprint.data.model.AuthTokens;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/** Provisional JSON contract; confirm field names and response envelope with backend. */
public interface AuthApi {
    @POST("api/auth/signup") Call<Void> signup(@Body Map<String, Object> body);
    @POST("api/auth/login") Call<AuthTokens> login(@Body Map<String, String> body);
    @POST("api/auth/refresh") Call<AuthTokens> refresh(@Body Map<String, String> body);
    @POST("api/auth/logout") Call<Void> logout(@Body Map<String, String> body);
}
