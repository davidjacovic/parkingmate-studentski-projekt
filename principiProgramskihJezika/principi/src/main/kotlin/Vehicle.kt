package org.example

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
