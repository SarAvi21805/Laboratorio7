package com.example.ejercicioenclase2708.data.remote

import com.example.ejercicioenclase2708.BASE_URL // Desde Constants.kt
import com.example.ejercicioenclase2708.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object PexelsService {

    private val auth = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("Authorization", BuildConfig.PEXELS_API_KEY)
            .build()
        chain.proceed(req)
    }

    private val logger = HttpLoggingInterceptor().apply {
        // BODY para depurar, mostrar la petición y respuesta.
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(auth)
        .addInterceptor(logger)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val api: PexelsApi = Retrofit.Builder()
        .baseUrl(BASE_URL) // Constante importada
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(PexelsApi::class.java)
}