package org.example

import java.time.LocalDateTime
import java.math.BigDecimal
import java.util.UUID

data class Tariff(
    val id: UUID = UUID.randomUUID(),
    val tariff_type: String? = null,
    val duration: String? = null,
    val vehicle_type: String? = null,
    val price: BigDecimal? = null,
    val price_unit: String? = null,
    val hidden: Boolean? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val parking_location: UUID? = null
) {

    fun isValid(): Boolean {
        return isTypeValid() &&
                isDurationValid() &&
                isVehicleTypeValid() &&
                isPriceValid() &&
                isPriceUnitValid() &&
                isDatesValid()
    }

    private fun isTypeValid(): Boolean {
        return !tariff_type.isNullOrBlank()
    }

    private fun isDurationValid(): Boolean {
        val regex = Regex("^\\d{2}:\\d{2}-\\d{2}:\\d{2}\$")
        return !duration.isNullOrBlank() && regex.matches(duration)
    }

    private fun isVehicleTypeValid(): Boolean {
        return !vehicle_type.isNullOrBlank()
    }

    private fun isPriceValid(): Boolean {
        return price != null && price > BigDecimal.ZERO
    }

    private fun isPriceUnitValid(): Boolean {
        val allowedUnits = setOf(
            "dan", "ura", "noc", "prvi dve uri", "vsaka naslednja ura",
            "do 1 ure", "od 1 do 3 ure", "od 3 do 5 ur", "od 5 do 8 ur",
            "nad 8 ur", "do 3 ure", "nad 3 ure", "24 ur od nakupa"
        )
        return !price_unit.isNullOrBlank() && price_unit in allowedUnits
    }

    private fun isDatesValid(): Boolean {
        return if (created != null && modified != null) {
            !modified.isBefore(created)
        } else true
    }
}
