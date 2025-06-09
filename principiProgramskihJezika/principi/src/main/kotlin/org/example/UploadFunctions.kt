package org.example

import org.bson.types.ObjectId
import org.example.ParkingLocation
import org.example.Tariff
import org.example.db.ParkingLocationRepository
import org.example.db.TariffRepository
import java.time.LocalDateTime

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


            tariffRepo.findByParkingLocation(finalLoc.id.toHexString()).forEach {
                tariffRepo.deleteById(it.id.toHexString())
            }

            tariffs.forEach { tariff ->
                val full = tariff.copy(parking_location = finalLoc.id)
                if (full.isValid()) {
                    tariffRepo.insert(full)
                }
            }
        }
    }
}
