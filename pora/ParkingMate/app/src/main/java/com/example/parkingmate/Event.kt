package com.example.parkingmate

data class Event (
    val _id: String,
    val topic: String,
    val message: String,
    val timestamp: Long,
    val location: String,
    val eventType: EventType,
    val status: String, // "PENDING", "PROCESSED", "BLOCKCHAIN_RECORDED"
    val blockchainHash: String?,
    val blockchainTimestamp: Long?,
    val createdAt: Long?,
    val updatedAt: Long?
)