package org.example.db

import org.bson.types.ObjectId
import org.example.Vehicle
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.findOne

class VehicleRepository {

    private val collection = MongoDBConnection.database.getCollection<Vehicle>("vehicles")

    // Ubacivanje vozila
    fun insert(vehicle: Vehicle) {
        collection.insertOne(vehicle)
        println("✅ Ubaceno vozilo: $vehicle")
    }

    // Dohvatanje po ID-u
    fun findById(id: String): Vehicle? {
        return collection.findOne(Vehicle::id eq ObjectId(id))
    }

    // Ažuriranje
    fun update(vehicle: Vehicle): Boolean {
        val result = collection.replaceOne(Vehicle::id eq vehicle.id, vehicle)
        return result.modifiedCount > 0
    }

    // Brisanje po ID-u
    fun deleteById(id: String): Boolean {
        val result = collection.findOne(Vehicle::id eq ObjectId(id))
        if (result != null) {
            result.hidden = true
            collection.replaceOne(Vehicle::id eq result.id, result)
            return true
        }
        return false
    }

    // Dohvati sva vozila
    fun findAll(): List<Vehicle> {
        return collection.find().toList()
    }

    // Dohvati vozila po tipu
    fun findByVehicleType(type: String): List<Vehicle> {
        return collection.find(Vehicle::vehicle_type eq type).toList()
    }

    // Dohvati vozila po korisniku
    fun findByUser(userId: String): List<Vehicle> {
        return collection.find(Vehicle::user eq ObjectId(userId)).toList()
    }

    // Dohvati po registracionom broju
    fun findByRegistrationNumber(registrationNumber: String): Vehicle? {
        return collection.findOne(Vehicle::registration_number eq registrationNumber)
    }
}