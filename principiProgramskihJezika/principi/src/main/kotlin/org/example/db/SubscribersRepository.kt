package org.example.db

import org.bson.types.ObjectId
import org.example.Subscriber
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.findOne
import org.litote.kmongo.ne

class SubscribersRepository {

    private val collection = MongoDBConnection.database.getCollection<Subscriber>("subscribers")

    // Ubacivanje novog subscriber-a
    fun insert(subscriber: Subscriber) {
        collection.insertOne(subscriber)
        println("✅ Ubacen Subscriber: $subscriber")
    }

    // Dohvatanje po ID-u (string konvertovan u ObjectId)
    fun findById(id: String): Subscriber? {
        return collection.findOne(Subscriber::id eq ObjectId(id))
    }

    // Ažuriranje (po ID-u)
    fun update(subscriber: Subscriber): Boolean {
        val result = collection.replaceOne(Subscriber::id eq subscriber.id, subscriber)
        return result.modifiedCount > 0
    }

    // Soft delete po ID-u (postavi hidden na true)
    fun deleteById(id: String): Boolean {
        val subscriber = findById(id)
        return if (subscriber != null) {
            val updated = subscriber.copy(hidden = true)
            collection.replaceOne(Subscriber::id eq subscriber.id, updated)
            true
        } else {
            false
        }
    }

    // Dohvati sve koje nisu sakrivene (hidden != true)
    fun findAllVisible(): List<Subscriber> {
        return collection.find(Subscriber::hidden ne true).toList()
    }
}
