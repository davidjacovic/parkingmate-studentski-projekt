package org.example

@kotlinx.serialization.Serializable
data class ReviewUpload(
    val rating: Int,
    val review_text: String,
    val review_date: String,
    val created: String,
    val modified: String,
    val hidden: Boolean = false,
    val user: String,
    val parking_location: String
)
