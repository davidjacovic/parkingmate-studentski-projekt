import java.time.LocalDateTime
import java.util.UUID
import java.util.regex.Pattern

data class User(
    val id: UUID = UUID.randomUUID(),
    val name: String? = null,
    val surname: String? = null,
    val username: String? = null,
    val email: String? = null,
    val password_hash: String? = null,
    val phone_number: String? = null,
    val credit_card_number: String? = null,
    val user_type: String? = null,
    val hidden: Boolean? = null,
    val created_at: LocalDateTime? = null,
    val updated_at: LocalDateTime? = null
) {
    companion object {
        private val EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        private val CREDIT_CARD_REGEX = Pattern.compile("^\\d{13,19}$")
        private val PHONE_REGEX = Pattern.compile("^\\d{7,15}$")
        private val ALLOWED_USER_TYPES = setOf("admin", "user")
    }

    fun isUsernameValid(): Boolean {
        return !username.isNullOrBlank() && username.length >= 3
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

    fun isValidUser(): Boolean {
        return isUsernameValid() &&
                isEmailValid() &&
                isCreditCardValid() &&
                isPhoneNumberValid() &&
                isUserTypeValid()
    }
}
