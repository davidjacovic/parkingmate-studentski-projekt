package org.example

import org.bson.types.ObjectId
import org.example.ParkingLocation
import org.example.Tariff
import org.example.db.ParkingLocationRepository
import org.example.db.TariffRepository
import java.time.LocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode


class ScraperDataUploader(
    private val locationRepo: ParkingLocationRepository = ParkingLocationRepository(),
    private val tariffRepo: TariffRepository = TariffRepository()
) {
    fun bulkInsertOrUpdate(pairs: List<Pair<ParkingLocation, List<Tariff>>>) {
        val existing = locationRepo.findAll()

        pairs.forEach { (newLoc, tariffs) ->
            val existingLoc = existing.find {
                it.name.trim().equals(newLoc.name.trim(), ignoreCase = true) &&
                        it.address.trim().equals(newLoc.address.trim(), ignoreCase = true)
            }

            val finalLoc = if (existingLoc != null) {
                val updated = newLoc.copy(id = existingLoc.id, created = existingLoc.created, modified = LocalDateTime.now())
                locationRepo.update(updated)
                updated
            } else {
                locationRepo.insert(newLoc)
                newLoc
            }

            tariffs.forEach { tariff ->
                val full = tariff.copy(
                    price = (tariff.price ?: BigDecimal.ZERO).divide(BigDecimal(100), 2, RoundingMode.HALF_UP),
                    parking_location = finalLoc.id,
                    modified = LocalDateTime.now()
                )

                val existingTariff = tariffRepo.findMatching(full)

                if (existingTariff != null) {
                    val updated = full.copy(id = existingTariff.id, created = existingTariff.created)
                    tariffRepo.update(updated)
                    println("📝 Updated tariff: ${updated.id}")
                } else {
                    tariffRepo.insert(full.copy(id = ObjectId(), created = LocalDateTime.now()))
                    println("✅ Inserted new tariff")
                }
            }

        }
    }

}
