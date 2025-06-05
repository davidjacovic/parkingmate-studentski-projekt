package org.example.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.Review
import org.example.db.ReviewsRepository

class ReviewAdminViewModel(
    private val reviewsRepository: ReviewsRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadReviews()
    }

    fun loadReviews() {
        coroutineScope.launch {
            try {
                _reviews.value = reviewsRepository.findAllVisible()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom učitavanja recenzija: ${e.localizedMessage}"
            }
        }
    }

    fun addReview(review: Review) {
        coroutineScope.launch {
            try {
                reviewsRepository.insert(review)
                loadReviews()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dodavanja recenzije: ${e.localizedMessage}"
            }
        }
    }

    fun updateReview(review: Review) {
        coroutineScope.launch {
            try {
                val success = reviewsRepository.update(review)
                if (success) {
                    loadReviews()
                    _error.value = null
                } else {
                    _error.value = "❌ Neuspešno ažuriranje recenzije!"
                }
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom ažuriranja recenzije: ${e.localizedMessage}"
            }
        }
    }

    fun deleteReview(reviewId: String) {
        coroutineScope.launch {
            try {
                val success = reviewsRepository.deleteById(reviewId)
                if (success) {
                    loadReviews()
                    _error.value = null
                } else {
                    _error.value = "❌ Recenzija nije pronađena za brisanje!"
                }
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom brisanja recenzije: ${e.localizedMessage}"
            }
        }
    }

    fun findReviewsByUser(userId: String, onResult: (List<Review>) -> Unit) {
        coroutineScope.launch {
            try {
                val userReviews = reviewsRepository.findByUser(userId)
                onResult(userReviews)
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dohvatanja recenzija korisnika: ${e.localizedMessage}"
                onResult(emptyList())
            }
        }
    }

    fun findReviewsByParkingLocation(locationId: String, onResult: (List<Review>) -> Unit) {
        coroutineScope.launch {
            try {
                val locationReviews = reviewsRepository.findByParkingLocation(locationId)
                onResult(locationReviews)
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dohvatanja recenzija za lokaciju: ${e.localizedMessage}"
                onResult(emptyList())
            }
        }
    }
}
