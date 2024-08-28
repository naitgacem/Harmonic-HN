package com.simon.harmonichackernews.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface FirebaseService {
    @GET("v0/topstories.json")
    suspend fun getTopStories(): Response<List<Int>>

    @GET("v0/newstories.json")
    suspend fun getNewStories(): Response<List<Int>>

    @GET("v0/beststories.json")
    suspend fun getBestStories(): Response<List<Int>>

    @GET("v0/askstories.json")
    suspend fun getAskStories(): Response<List<Int>>

    @GET("v0/showstories.json")
    suspend fun getShowStories(): Response<List<Int>>

    @GET("v0/jobstories.json")
    suspend fun getJobStories(): Response<List<Int>>

    @GET("v0/item/{id}.json")
    suspend fun getStory(@Path("id") id: Int) : Response<ResponseBody>
}