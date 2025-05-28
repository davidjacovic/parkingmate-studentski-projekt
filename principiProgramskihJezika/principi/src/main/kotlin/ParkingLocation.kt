package org.example

import java.time.LocalDateTime
import java.util.UUID

data class ParkingLocation(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val address: String,
    val location: LocationCoordinates,
    val total_regular_spots: Int,
    val total_invalid_spots: Int,
    val total_bus_spots: Int,
    val available_regular_spots: Int,
    val available_invalid_spots: Int,
    val available_bus_spots: Int,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val description: String? = null,
    val hidden: Boolean? = null,
    val subscriber: UUID? = null
)
