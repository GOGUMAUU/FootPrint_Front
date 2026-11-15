package com.example.footprint.api;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

public interface RelationshipApi {
    @POST("api/follows/{id}") Call<JsonObject> follow(@Path("id") long id);
    @DELETE("api/follows/{id}") Call<Void> unfollow(@Path("id") long id);
    @POST("api/users/{id}/blocks") Call<Void> block(@Path("id") long id);
    @DELETE("api/users/{id}/blocks") Call<Void> unblock(@Path("id") long id);
    @POST("api/follow-requests/{id}/accept") Call<JsonObject> accept(@Path("id") long id);
    @POST("api/follow-requests/{id}/reject") Call<JsonObject> reject(@Path("id") long id);
    @GET("api/users/me/follow-requests") Call<JsonObject> requests(@Query("page") int page, @Query("size") int size);
    @GET("api/users/me/blocks") Call<JsonObject> blocks(@Query("page") int page, @Query("size") int size);
    @GET("api/users/{id}/followers") Call<JsonObject> followers(@Path("id") long id, @Query("page") int page, @Query("size") int size);
    @GET("api/users/{id}/following") Call<JsonObject> following(@Path("id") long id, @Query("page") int page, @Query("size") int size);
}
