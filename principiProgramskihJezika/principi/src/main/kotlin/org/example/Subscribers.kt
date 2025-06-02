package org.example

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.LocalDateTime
import java.util.UUID

data class Subscriber(
    @BsonId
    val id: ObjectId = ObjectId.get(),
    val available_spots: Int? = null,
    val total_spots: Int? = null,
    val reserved_spots: Int? = null,
    val waiting_line: Int? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val hidden: Boolean? = null
){
    fun isValid(): Boolean {
        return isSpotDataValid() && isDateValid()
    }

    private fun isSpotDataValid(): Boolean {
        if (available_spots == null || reserved_spots == null || total_spots == null || waiting_line == null)
            return false

        if (available_spots < 0 || reserved_spots < 0 || total_spots < 0 || waiting_line < 0)
            return false

        if ((available_spots + reserved_spots) > total_spots)
            return false

        return true
    }

    private fun isDateValid(): Boolean {
        if (created != null && modified != null) {
            return !modified.isBefore(created)
        }
        return true
    }

}
