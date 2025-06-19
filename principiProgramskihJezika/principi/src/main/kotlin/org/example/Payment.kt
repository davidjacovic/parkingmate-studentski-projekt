package org.example

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.LocalDateTime
import java.math.BigDecimal
import java.util.UUID

data class Payment(
    @BsonId
    val id: ObjectId = ObjectId.get(),
    val date: LocalDateTime? = null,
    val amount: BigDecimal? = null,
    val method: String? = null,
    val payment_status: String? = null, // "pending", "completed", "failed"
    val duration: Int? = null,
    var hidden: Boolean? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val user: ObjectId? = null,
    val parking_location: ObjectId? = null
){
    fun isValid(): Boolean {
        return isAmountValid() &&
                isMethodValid() &&
                isStatusValid() &&
                isDurationValid() &&
                isDatesValid()
    }

    private fun isAmountValid(): Boolean {
        return amount != null && amount > BigDecimal.ZERO
    }

    private fun isMethodValid(): Boolean {
        return !method.isNullOrBlank()
    }

    private fun isStatusValid(): Boolean {
        return payment_status in listOf("pending", "completed", "failed")
    }

    private fun isDurationValid(): Boolean {
        return duration != null && duration > 0
    }

    private fun isDatesValid(): Boolean {
        return if (created != null && modified != null) {
            !modified.isBefore(created)
        } else true
    }

}
