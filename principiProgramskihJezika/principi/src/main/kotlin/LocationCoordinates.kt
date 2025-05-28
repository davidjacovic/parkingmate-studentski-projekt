package org.example

import java.util.UUID

data class LocationCoordinates(
    val id: UUID = UUID.randomUUID(),
    val type: String = "Point",
    val coordinates: List<Double>
)
