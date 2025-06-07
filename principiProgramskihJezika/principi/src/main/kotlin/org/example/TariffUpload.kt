package org.example
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class TariffUploadDTO(
    val tariff_type: String,
    val duration: String? = null,
    val vehicle_type: String? = null,
    val price: String,
    val price_unit: String,
    val hidden: Boolean = false,
    val created: String,
    val modified: String,
    val parking_location: String
)

fun Tariff.toUploadDTO(parkingLocationId: String): TariffUploadDTO {
    val now = LocalDateTime.now().toString()
    return TariffUploadDTO(
        tariff_type = this.tariff_type ?: "",
        duration = this.duration,
        vehicle_type = this.vehicle_type,
        price = this.price?.toPlainString() ?: "0.00",
        price_unit = this.price_unit ?: "EUR",
        hidden = this.hidden ?: false,
        created = this.created?.toString() ?: now,
        modified = this.modified?.toString() ?: now,
        parking_location = parkingLocationId
    )
}