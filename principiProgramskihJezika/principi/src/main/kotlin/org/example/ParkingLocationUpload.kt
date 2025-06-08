package org.example
import kotlinx.serialization.Serializable
import org.example.LocationCoordinates
import java.time.LocalDateTime

@Serializable
data class ParkingLocationUploadDTO(
    val name: String,
    val address: String,
    val location: LocationCoordinates,
    val total_regular_spots: Int,
    val total_invalid_spots: Int,
    val total_bus_spots: Int,
    val available_regular_spots: Int,
    val available_invalid_spots: Int,
    val available_bus_spots: Int,
    val created: String,
    val modified: String,
    val description: String? = null,
    val hidden: Boolean = false,
)

fun ParkingLocation.toUploadDTO(): ParkingLocationUploadDTO {
    val now = LocalDateTime.now().toString()
    return ParkingLocationUploadDTO(
        name = this.name,
        address = this.address,
        location = this.location,
        total_regular_spots = this.total_regular_spots,
        total_invalid_spots = this.total_invalid_spots,
        total_bus_spots = this.total_bus_spots,
        available_regular_spots = this.available_regular_spots,
        available_invalid_spots = this.available_invalid_spots,
        available_bus_spots = this.available_bus_spots,
        created = this.created?.toString() ?: now,
        modified = this.modified?.toString() ?: now,
        description = this.description,
        hidden = this.hidden ?: false,
    )
}