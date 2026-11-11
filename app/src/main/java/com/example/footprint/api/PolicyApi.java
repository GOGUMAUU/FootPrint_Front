package com.example.footprint.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

/** Policy types, versions and response envelope must match the backend contract. */
public interface PolicyApi {
    @GET("api/policies") Call<JsonElement> policies();
    @POST("api/users/me/consents") Call<Void> consent(@Body JsonObject body);
    @DELETE("api/users/me/consents/{type}") Call<Void> revoke(@Path("type") String type);
}
