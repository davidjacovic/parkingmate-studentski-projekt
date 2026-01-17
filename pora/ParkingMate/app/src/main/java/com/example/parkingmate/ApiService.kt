package com.example.parkingmate

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("/api/ml/analyze")
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part
    ): MlAnalyzeResponse
}
