package org.example

import kotlinx.serialization.Serializable

@Serializable
data class VehicleUpload(
    val registration_number: String,
    val vehicle_type: String,
    val created: String,
    val modified: String,
    val user: String,
    val hidden: Boolean = false,
    val isValid: Boolean = true
)
