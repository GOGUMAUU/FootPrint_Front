package com.example.footprint.api;

import com.example.footprint.data.model.Challenge;
import com.example.footprint.data.model.ChallengeListResponse;
import com.example.footprint.data.model.BadgeListResponse;
import com.example.footprint.data.model.Guestbook;
import com.example.footprint.data.model.UserSummary;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("api/maps/guestbooks/nearby")
    Call<List<Guestbook>> getNearbyGuestbooks(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("zoom") double zoom,
            @Query("radiusMeters") double radiusMeters,
            @Query("ageBand") String ageBand,
            @Query("recentDays") Integer recentDays,
            @Query("sort") String sort);

    @POST("api/maps/guestbooks/{guestbookId}/check-distance")
    Call<Map<String, Object>> checkDistance(@Path("guestbookId") String guestbookId,
                                            @Body Map<String, Double> location);

    @GET("api/maps/guestbooks/bound")
    Call<List<Guestbook>> getGuestbooksInBounds(@Query("scope") String scope,
                                                 @Query("userId") String userId,
                                                 @Query("zoom") double zoom,
                                                 @Query("south") double south,
                                                 @Query("west") double west,
                                                 @Query("north") double north,
                                                 @Query("east") double east);

    @GET("api/guestbooks/{guestbookId}")
    Call<Guestbook> getGuestbook(@Path("guestbookId") String guestbookId);

    @POST("api/guestbooks")
    Call<Guestbook> createGuestbook(@Body Map<String, Object> body);

    @Multipart
    @POST("api/guestbook-photos")
    Call<Map<String, String>> uploadPhoto(@Part MultipartBody.Part file,
                                          @Part("purpose") RequestBody purpose);

    @Multipart
    @POST("api/guestbook-photos")
    Call<Map<String, String>> uploadChallengePhoto(
            @Part MultipartBody.Part file
    );

    @POST("api/guestbooks/{guestbookId}/likes")
    Call<Void> like(@Path("guestbookId") String guestbookId);

    @DELETE("api/guestbooks/{guestbookId}/likes")
    Call<Void> unlike(@Path("guestbookId") String guestbookId);

    @POST("api/guestbooks/{guestbookId}/views")
    Call<Void> saveView(@Path("guestbookId") String guestbookId);

    @POST("api/guestbooks/{guestbookId}/reports")
    Call<Void> report(@Path("guestbookId") String guestbookId, @Body Map<String, String> body);

    @GET("api/users/search")
    Call<List<UserSummary>> searchUsers(@Query("q") String query);

    @POST("api/users/{userId}/follow-requests")
    Call<Void> requestFollow(@Path("userId") String userId);

    @POST("api/follow-requests/{followId}/accept")
    Call<Void> acceptFollow(@Path("followId") String followId);

    @POST("api/follow-requests/{followId}/reject")
    Call<Void> rejectFollow(@Path("followId") String followId);


    @GET("api/challenges")
    Call<ChallengeListResponse> getChallenges();

    @POST("api/challenges/{challengeId}/verify-location")
    Call<Map<String, Object>> verifyChallengeLocation(@Path("challengeId") String challengeId,
                                                       @Body Map<String, Double> location);

    @POST("api/challenges/{challengeId}/submissions")
    Call<Void> submitChallenge(@Path("challengeId") String challengeId,
                               @Body Map<String, Object> body);

    // 현재 사용자가 획득한 배지 목록 조회
    @GET("api/badges/my")
    Call<BadgeListResponse> getMyBadges();

}
