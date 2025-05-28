package org.example

import java.time.LocalDateTime
import java.util.UUID

data class Subscriber(
    val id: UUID = UUID.randomUUID(),
    val available_spots: Int? = null,
    val total_spots: Int? = null,
    val reserved_spots: Int? = null,
    val waiting_line: Int? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val hidden: Boolean? = null
)
