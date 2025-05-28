import java.time.LocalDateTime
import java.util.UUID
data class User(
    val id: UUID = UUID.randomUUID(),
    val name: String? = null,
    val surname: String? = null,
    val username: String? = null,
    val email: String? = null,
    val password_hash: String? = null,
    val phone_number: String? = null,
    val credit_card_number: String? = null,
    val user_type: String? = null,//admin ili user
    val hidden: Boolean? = null,//ako se user izbrise da se postavi na true
    val created_at: LocalDateTime? = null,
    val updated_at: LocalDateTime? = null
)

