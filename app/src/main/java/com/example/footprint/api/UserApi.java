package com.example.footprint.api;

import com.google.gson.JsonObject;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

/** Response schemas are provisional until the backend OpenAPI contract is supplied. */
public interface UserApi {
    @GET("api/users/me") Call<JsonObject> me();
    @GET("api/users/{userId}") Call<JsonObject> profile(@Path("userId") String id);
    @PATCH("api/users/me") Call<JsonObject> update(@Body JsonObject body);
    @PATCH("api/users/me/password") Call<Void> password(@Body JsonObject body);
    @DELETE("api/users/me") Call<Void> deleteAccount();
    @Multipart @POST("api/users/me/profile-image")
    Call<JsonObject> uploadImage(@Part MultipartBody.Part image);
    @DELETE("api/users/me/profile-image") Call<Void> deleteImage();
}
