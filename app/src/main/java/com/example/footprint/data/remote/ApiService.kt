package com.example.footprint.data.remote

import Quest
import retrofit2.Response
import retrofit2.http.*
import com.example.footprint.data.model.Guestbook

interface ApiService {
    // 방명록 조회
    @GET("api/guestbooks")
    suspend fun getGuestbooks(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double
    ): Response<List<Guestbook>>

    // 방명록 작성
    @POST("api/guestbooks")
    suspend fun createGuestbook(@Body guestbook: Guestbook): Response<Guestbook>

    // 퀘스트 목록 조회
    @GET("api/quests")
    suspend fun getQuests(@Query("region") region: String): Response<List<Quest>>
}