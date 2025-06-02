package org.example.db

import org.bson.types.ObjectId
import org.example.Payment
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.findOne
import java.util.*

class PaymentRepository {

    private val collection = MongoDBConnection.database.getCollection<Payment>("payments")

    // Ubacivanje novog payment-a
    fun insert(payment: Payment) {
        collection.insertOne(payment)
        println("✅ Ubacen Payment: $payment")
    }

    // Dohvatanje po ID-u (primi string, konvertuj u ObjectId)
    fun findById(id: String): Payment? {
        return collection.findOne(Payment::id eq ObjectId(id))
    }

    // Ažuriranje (po ID-u)
    fun update(payment: Payment): Boolean {
        val result = collection.replaceOne(Payment::id eq payment.id, payment)
        return result.modifiedCount > 0
    }

    // Brisanje po ID-u
    fun deleteById(id: String): Boolean {
        val result = collection.findOne(Payment::id eq ObjectId(id))
        if (result != null) {
            result.hidden = true
            collection.replaceOne(Payment::id eq result.id, result)
            return true
        }
        return false
    }

    // Dohvati sve
    fun findAll(): List<Payment> {
        return collection.find().toList()
    }

    // Dohvati po statusu
    fun findByStatus(status: String): List<Payment> {
        return collection.find(Payment::payment_status eq status).toList()
    }

    // Dohvati po user-u
    fun findByUser(userId: String): List<Payment> {
        return collection.find(Payment::user eq ObjectId(userId)).toList()
    }

    // Dohvati po lokaciji
    fun findByParkingLocation(locationId: String): List<Payment> {
        return collection.find(Payment::parking_location eq ObjectId(locationId)).toList()
    }

}
