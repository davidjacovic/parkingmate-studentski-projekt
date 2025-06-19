package org.example

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import org.example.Vehicle
import java.time.LocalDateTime
import java.util.UUID
import java.util.regex.Pattern

data class User(
    @BsonId
    val id: ObjectId = ObjectId.get(),
    val name: String? = null,
    val surname: String? = null,
    val username: String? = null,
    val email: String? = null,
    val password_hash: String? = null,
    val phone_number: String? = null,
    val credit_card_number: String? = null,
    val user_type: String? = null,
    var hidden: Boolean? = null,
    val created_at: LocalDateTime? = null,
    val updated_at: LocalDateTime? = null,
    val vehicles: List<Vehicle> = emptyList()
) {
    companion object {
        private val EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        private val CREDIT_CARD_REGEX = Pattern.compile("^\\d{13,19}$")
        private val PHONE_REGEX = Pattern.compile("^\\d{7,15}$")
        private val USERNAME_REGEX = Pattern.compile("^[A-Za-z0-9._]{8,}$")
        private val ALLOWED_USER_TYPES = setOf("admin", "user")
        private val PASSWORD_REGEX = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&._])[A-Za-z\\d@\$!%*?&._]{8,}$")
    }

    fun isUsernameValid(): Boolean {
        return !username.isNullOrBlank() && USERNAME_REGEX.matcher(username).matches()
    }

    fun isEmailValid(): Boolean {
        if (email.isNullOrBlank()) return false
        return EMAIL_REGEX.matcher(email).matches()
    }

    fun isCreditCardValid(): Boolean {
        if (credit_card_number.isNullOrBlank()) return false
        return CREDIT_CARD_REGEX.matcher(credit_card_number).matches()
    }

    fun isPhoneNumberValid(): Boolean {
        if (phone_number.isNullOrBlank()) return false
        return PHONE_REGEX.matcher(phone_number).matches()
    }

    fun isUserTypeValid(): Boolean {
        if (user_type.isNullOrBlank()) return false
        return ALLOWED_USER_TYPES.contains(user_type.lowercase())
    }

    fun isPasswordValid(): Boolean {
        if (password_hash.isNullOrBlank()) return false

        // Skip validation if this is already a bcrypt-hashed password
        if (password_hash.startsWith("\$2a\$") || password_hash.startsWith("\$2b\$")) return true

        return PASSWORD_REGEX.matcher(password_hash).matches()
    }

}

