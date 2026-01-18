package com.example.parkingmate

import com.google.gson.annotations.SerializedName

data class MlAnalyzeResponse(
    val success: Boolean,
    val data: MlResult?
)

data class MlResult(
    @SerializedName("freeSpaces") val free: Int?,
    @SerializedName("occupiedSpaces") val occupied: Int?,
    @SerializedName("totalSpots") val totalSpots: Int? // opciono, ali korisno
)
