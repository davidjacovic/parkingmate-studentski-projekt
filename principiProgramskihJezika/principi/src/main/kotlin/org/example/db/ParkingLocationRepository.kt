package org.example.db

import org.bson.types.ObjectId
import org.example.ParkingLocation
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.findOne
import java.util.*

class ParkingLocationRepository {

    private val collection = MongoDBConnection.database.getCollection<ParkingLocation>("parking_locations")

    // Ubacivanje novog parkirališta
    fun insert(parkingLocation: ParkingLocation) {
        collection.insertOne(parkingLocation)
        println("✅ Ubaceno ParkingLocation: $parkingLocation")
    }

    // Dohvatanje po ID-u
    fun findById(id: String): ParkingLocation? {
        return collection.findOne(ParkingLocation::id eq ObjectId(id))
    }


    // Ažuriranje (po ID-u)
    fun update(parkingLocation: ParkingLocation): Boolean {
        val result = collection.replaceOne(ParkingLocation::id eq parkingLocation.id, parkingLocation)
        return result.modifiedCount > 0
    }

    // Brisanje po ID-u
    fun deleteById(id: String): Boolean {
        val objectId = ObjectId(id)  // Kreiramo ObjectId iz stringa
        val result = collection.findOne(ParkingLocation::id eq objectId)
        if (result != null) {
            result.hidden = true
            collection.replaceOne(ParkingLocation::id eq result.id, result)
            return true
        }
        return false
    }


    // Dohvati sve parkirališta
    fun findAll(): List<ParkingLocation> {
        return collection.find().toList()
    }

    // Dodatni primer: dohvati parkirališta po nazivu
    fun findByName(name: String): List<ParkingLocation> {
        return collection.find(ParkingLocation::name eq name).toList()
    }
}
