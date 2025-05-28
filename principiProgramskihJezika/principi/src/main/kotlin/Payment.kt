package org.example

import java.time.LocalDateTime
import java.math.BigDecimal
import java.util.UUID

data class Payment(
    val id: UUID = UUID.randomUUID(),
    val date: LocalDateTime? = null,
    val amount: BigDecimal? = null,
    val method: String? = null,
    val payment_status: String? = null, // "pending", "completed", "failed"
    val duration: Int? = null,
    val hidden: Boolean? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val user: UUID? = null,
    val parking_location: UUID? = null
)
