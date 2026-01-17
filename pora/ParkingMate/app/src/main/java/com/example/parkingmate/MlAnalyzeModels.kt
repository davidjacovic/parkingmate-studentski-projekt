package com.example.parkingmate

data class MlAnalyzeResponse(
    val success: Boolean,
    val data: MlResult?
)

data class MlResult(
    val free: Int?,
    val occupied: Int?,
    val status: String?
)
