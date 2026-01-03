package com.example.parkingmate

data class Event (
    val topic: String,
    val message: String,
    val timestamp: Long,
    val location: String
)