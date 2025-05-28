package org.example

import java.time.LocalDateTime
import java.util.UUID

data class Review(
    val id: UUID = UUID.randomUUID(),
    val rating: Int? = null,
    val review_text: String? = null,
    val review_date: LocalDateTime? = null,
    val hidden: Boolean? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val user: UUID? = null,
    val parking_location: UUID? = null
)
