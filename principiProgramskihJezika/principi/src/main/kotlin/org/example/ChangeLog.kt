package org.example

import java.time.LocalDateTime
import java.util.UUID

data class ChangeLog(
    val id: UUID = UUID.randomUUID(),
    val changed_table_name: String,
    val record_id: Int,
    val type_of_change:String,
    val time_of_change: LocalDateTime = LocalDateTime.now()
)
