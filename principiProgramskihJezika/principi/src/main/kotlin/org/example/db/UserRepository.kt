package org.example.db

import org.bson.types.ObjectId
import org.example.User
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.findOne

class UserRepository {

    private val collection = MongoDBConnection.database.getCollection<User>("users")

    // Ubacivanje novog user-a
    fun insert(user: User) {
        collection.insertOne(user)
        println("✅ Ubacen User: $user")
    }

    // Dohvatanje po ID-u
    fun findById(id: String): User? {
        return collection.findOne(User::id eq ObjectId(id))
    }

    // Ažuriranje user-a
    fun update(user: User): Boolean {
        val result = collection.replaceOne(User::id eq user.id, user)
        return result.modifiedCount > 0
    }

    // Brisanje po ID-u
    fun deleteById(id: String): Boolean {
        val result = collection.findOne(User::id eq ObjectId(id))
        if (result != null) {
            result.hidden = true
            collection.replaceOne(User::id eq result.id, result)
            return true
        }
        return false
    }

    // Dohvati sve
    fun findAll(): List<User> {
        return collection.find().toList()
    }

    // Dohvati po username-u
    fun findByUsername(username: String): User? {
        return collection.findOne(User::username eq username)
    }

    // Dohvati po email-u
    fun findByEmail(email: String): User? {
        return collection.findOne(User::email eq email)
    }

    // Dohvati po user_type
    fun findByUserType(userType: String): List<User> {
        return collection.find(User::user_type eq userType).toList()
    }

    // Dohvati sve koji nisu skriveni
    fun findVisible(): List<User> {
        return collection.find(User::hidden eq false).toList()
    }
}