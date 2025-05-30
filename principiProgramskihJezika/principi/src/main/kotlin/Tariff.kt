package org.example

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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

    fun isValid(existingTariffs: List<Tariff> = emptyList()): Boolean {
        return isTypeValid() &&
                isDurationValid() &&
                isVehicleTypeValid() &&
                isPriceValid() &&
                isPriceUnitValid() &&
                isDatesValid() &&
                !isOverlapping(existingTariffs)
    }

    private fun isTypeValid(): Boolean {
        return !tariff_type.isNullOrBlank()
    }

    private fun isDurationValid(): Boolean {
        if (duration.isNullOrBlank()) return false

        val parts = duration.split("-")
        if (parts.size != 2) return false

        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val start: LocalTime
        val end: LocalTime
        try {
            start = LocalTime.parse(parts[0], formatter)
            end = LocalTime.parse(parts[1], formatter)
        } catch (e: DateTimeParseException) {
            return false
        }
        if (!end.isAfter(start)) {
            return false
        }

        return true
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

    private fun isOverlapping(existingTariffs: List<Tariff>): Boolean {
        if (duration.isNullOrBlank() || parking_location == null) return false

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val (startStr, endStr) = duration.split("-")
        val start = LocalTime.parse(startStr, formatter)
        val end = LocalTime.parse(endStr, formatter)

        for (tariff in existingTariffs) {
            if (tariff.id == this.id) continue
            if (tariff.parking_location != this.parking_location) continue
            if (tariff.duration.isNullOrBlank()) continue

            val (otherStartStr, otherEndStr) = tariff.duration.split("-")
            val otherStart = LocalTime.parse(otherStartStr, formatter)
            val otherEnd = LocalTime.parse(otherEndStr, formatter)

            if (start < otherEnd && otherStart < end) {
                return true
            }
        }

        return false
    }
}
