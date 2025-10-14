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

    @GET("v1/search")
    fun searchPhotos(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Call<PexelsResponse>

    // Pantalla "Detalles"
    @GET("v1/photos/{id}")
    fun getPhoto(
        @retrofit2.http.Path("id") id: String
    ): Call<PexelsPhoto>
}
