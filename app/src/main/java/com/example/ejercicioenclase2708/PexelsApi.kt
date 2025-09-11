package com.example.ejercicioenclase2708

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface PexelsApi {
    @GET("v1/curated")
    fun getCurated(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Call<PexelsResponse>
}
