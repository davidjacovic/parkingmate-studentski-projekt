package org.example.db

import org.bson.types.ObjectId
import org.example.Tariff
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.findOne
import org.litote.kmongo.ne
import org.litote.kmongo.and


class TariffRepository {

    private val collection = MongoDBConnection.database.getCollection<Tariff>("tariffs")

    // Ubacivanje novog tarife
    fun insert(tariff: Tariff) {
        collection.insertOne(tariff)
        println("✅ Ubacen Tariff: $tariff")
    }

    // Dohvatanje po ID-u (primi string, konvertuj u ObjectId)
    fun findById(id: String): Tariff? {
        return collection.findOne(Tariff::id eq ObjectId(id))
    }

    // Ažuriranje (po ID-u)
    fun update(tariff: Tariff): Boolean {
        val result = collection.replaceOne(Tariff::id eq tariff.id, tariff)
        return result.modifiedCount > 0
    }

    // Soft delete po ID-u
    fun deleteById(id: String): Boolean {
        val result = collection.findOne(Tariff::id eq ObjectId(id))
        if (result != null) {
            val updated = result.copy(hidden = true)
            collection.replaceOne(Tariff::id eq result.id, updated)
            return true
        }
        return false
    }

    // Dohvati sve
    fun findAll(): List<Tariff> {
        return collection.find().toList()
    }

    // Dohvati sve vidljive (hidden != true)
    fun findAllVisible(): List<Tariff> {
        return collection.find(Tariff::hidden ne true).toList()
    }

    // Dohvati po parking lokaciji
    fun findByParkingLocation(locationId: String): List<Tariff> {
        return collection.find(Tariff::parking_location eq ObjectId(locationId)).toList()
    }
    fun findMatching(tariff: Tariff): Tariff? {
        return collection.findOne(
            and(
                Tariff::tariff_type eq tariff.tariff_type,
                Tariff::duration eq tariff.duration,
                Tariff::vehicle_type eq tariff.vehicle_type,
                Tariff::price eq tariff.price,
                Tariff::price_unit eq tariff.price_unit,
                Tariff::parking_location eq tariff.parking_location
            )
        )
    }

}