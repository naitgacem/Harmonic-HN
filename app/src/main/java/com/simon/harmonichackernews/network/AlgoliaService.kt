package com.simon.harmonichackernews.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface AlgoliaService {
    @GET("search")
    suspend fun searchByRelevance(
        @Query("query") query: String,
        @Query("tags", encoded = true) tags: String? = null,
        @Query("numericFilters") numericFilters: String? = null,
        @Query("page") page: Int? = null,
        @Query("typoTolerance") tolerance: String = "min"
    ): ResponseBody

    @GET("search_by_date")
    suspend fun searchByDate(
        @Query("query") query: String,
        @Query("tags", encoded = true) tags: String? = null,
        @Query("numericFilters") numericFilters: String? = null,
        @Query("page") page: Int? = null,
        @Query("typoTolerance") tolerance: String = "min"
    ): ResponseBody
}