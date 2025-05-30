package org.example

import User
import java.time.LocalDateTime
import java.util.UUID

data class Vehicle(
    val id: UUID = UUID.randomUUID(),
    val registration_number: String? = null,
    val vehicle_type: String? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val user: UUID? = null
)
{
    fun isValid(): Boolean {
        return isRegistrationNumberValid() && isVehicleTypeValid()
    }

    private fun isRegistrationNumberValid(): Boolean {
        val regex = "^[A-Za-z0-9]{5,10}$".toRegex()
        return registration_number != null && registration_number.matches(regex)
    }

    private fun isVehicleTypeValid(): Boolean {
        val allowedTypes = setOf("car", "truck", "motorcycle", "bus")
        return vehicle_type != null && vehicle_type.lowercase() in allowedTypes
    }
}
