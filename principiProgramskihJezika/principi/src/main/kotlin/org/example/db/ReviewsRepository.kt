package org.example.db

import org.bson.types.ObjectId
import org.example.Review
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.findOne

class ReviewsRepository {

    private val collection = MongoDBConnection.database.getCollection<Review>("reviews")

    // Ubacivanje novog review-a
    fun insert(review: Review) {
        collection.insertOne(review)
        println("✅ Ubacen Review: $review")
    }

    // Dohvatanje po ID-u (prima string, konvertuje u ObjectId)
    fun findById(id: String): Review? {
        return collection.findOne(Review::id eq ObjectId(id))
    }

    // Ažuriranje review-a po ID-u
    fun update(review: Review): Boolean {
        val result = collection.replaceOne(Review::id eq review.id, review)
        return result.modifiedCount > 0
    }

    // Brisanje review-a po ID-u (soft delete: postavlja hidden na true)
    fun deleteById(id: String): Boolean {
        val review = collection.findOne(Review::id eq ObjectId(id))
        if (review != null) {
            val hiddenReview = review.copy(hidden = true)
            collection.replaceOne(Review::id eq review.id, hiddenReview)
            return true
        }
        return false
    }

    // Dohvati sve review-e
    fun findAll(): List<Review> {
        return collection.find().toList()
    }

    // Dohvati sve ne-hidden review-e (ako želiš da filtriraš)
    fun findAllVisible(): List<Review> {
        return collection.find(Review::hidden eq false).toList()
    }

    // Dohvati review-e po user-u
    fun findByUser(userId: String): List<Review> {
        return collection.find(Review::user eq ObjectId(userId)).toList()
    }

    // Dohvati review-e po parking lokaciji
    fun findByParkingLocation(locationId: String): List<Review> {
        return collection.find(Review::parking_location eq ObjectId(locationId)).toList()
    }

    // Dohvati review-e sa određenim ratingom
    fun findByRating(rating: Int): List<Review> {
        return collection.find(Review::rating eq rating).toList()
    }
}
