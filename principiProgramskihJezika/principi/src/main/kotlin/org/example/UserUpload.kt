package org.example

import kotlinx.serialization.Serializable

@Serializable
data class UserUpload(
    val username: String,
    val atribut: String,
    val email: String,
    val password: String,            // Plaintext password (for display only)
    val password_hash: String,       // = same as plaintext password
    val phone_number: String,
    val credit_card_number: String,
    val created_at: String,
    val updated_at: String,
    val user_type: String,
    val hidden: Boolean = false
)
