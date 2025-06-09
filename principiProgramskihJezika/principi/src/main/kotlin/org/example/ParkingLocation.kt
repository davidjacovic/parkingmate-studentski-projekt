package org.example

import java.util.Date
import java.util.UUID
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.LocalDateTime

data class ParkingLocation(
    @BsonId
    val id: ObjectId = ObjectId.get(),
    val name: String,
    val address: String,
    val location: LocationCoordinates,
    val total_regular_spots: Int,
    val total_invalid_spots: Int,
    val total_bus_spots: Int,
    val available_regular_spots: Int,
    val available_invalid_spots: Int,
    val available_bus_spots: Int,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val description: String? = null,
    var hidden: Boolean? = null,
) {
    fun isValid(): Boolean {
        return isNameAndAddressValid() &&
                isSpotCountsValid() &&
                isDatesValid()
    }

    private fun isNameAndAddressValid(): Boolean {
        return name.length in 2..100 && address.length in 2..100
    }

    private fun isSpotCountsValid(): Boolean {
        return total_regular_spots >= 0 &&
                total_invalid_spots >= 0 &&
                total_bus_spots >= 0 &&
                available_regular_spots in 0..total_regular_spots &&
                available_invalid_spots in 0..total_invalid_spots &&
                available_bus_spots in 0..total_bus_spots
    }

    private fun isDatesValid(): Boolean {
        return if (created != null && modified != null) {
            !modified.isBefore(created)
        } else true
    }

}
