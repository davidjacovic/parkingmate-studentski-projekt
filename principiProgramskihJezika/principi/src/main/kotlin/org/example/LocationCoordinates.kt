package org.example

data class LocationCoordinates(
    val type: String = "Point",
    val coordinates: List<Double> // [longitude, latitude]
)
