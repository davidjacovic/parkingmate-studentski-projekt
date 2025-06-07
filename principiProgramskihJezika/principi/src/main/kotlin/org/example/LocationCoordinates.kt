package org.example
import kotlinx.serialization.Serializable

@Serializable
data class LocationCoordinates(
    val type: String = "Point",
    val coordinates: List<Double> // [longitude, latitude]
)
