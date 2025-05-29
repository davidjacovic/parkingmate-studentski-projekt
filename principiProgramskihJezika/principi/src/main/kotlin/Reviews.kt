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
{
    fun isValid(): Boolean {
        return isRatingValid() &&
                isTextValid() &&
                isDateValid() &&
                isRelationsValid()
    }

    private fun isRatingValid(): Boolean {
        return rating != null && rating in 1..5
    }

    private fun isTextValid(): Boolean {
        return review_text == null || review_text.length <= 1000
    }

    private fun isDateValid(): Boolean {
        val now = LocalDateTime.now()
        if (review_date != null && review_date.isAfter(now)) return false
        if (created != null && modified != null && modified.isBefore(created)) return false
        return true
    }

    private fun isRelationsValid(): Boolean {
        return user != null && parking_location != null
    }

}