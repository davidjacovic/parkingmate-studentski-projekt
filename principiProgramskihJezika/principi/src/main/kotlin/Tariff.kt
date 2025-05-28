package org.example

import java.time.LocalDateTime
import java.math.BigDecimal
import java.util.UUID

data class Tariff(
    val id: UUID = UUID.randomUUID(),
    val tariff_type: String? = null,
    val duration: String? = null,
    val vehicle_type: String? = null,
    val price: BigDecimal? = null,
    val price_unit: String? = null,
    val hidden: Boolean? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val parking_location: UUID? = null
)
