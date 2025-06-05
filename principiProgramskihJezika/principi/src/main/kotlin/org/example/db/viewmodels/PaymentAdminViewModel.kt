package org.example.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.Payment
import org.example.db.PaymentRepository

class PaymentAdminViewModel(
    private val paymentRepository: PaymentRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPayments()
    }

    fun loadPayments() {
        coroutineScope.launch {
            try {
                _payments.value = paymentRepository.findAll()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom učitavanja uplata: ${e.localizedMessage}"
            }
        }
    }

    fun addPayment(payment: Payment) {
        coroutineScope.launch {
            try {
                paymentRepository.insert(payment)
                loadPayments() // refresh list after add
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dodavanja uplate: ${e.localizedMessage}"
            }
        }
    }

    fun updatePayment(payment: Payment) {
        coroutineScope.launch {
            try {
                val success = paymentRepository.update(payment)
                if (success) {
                    loadPayments()
                    _error.value = null
                } else {
                    _error.value = "❌ Neuspešno ažuriranje uplate!"
                }
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom ažuriranja uplate: ${e.localizedMessage}"
            }
        }
    }

    fun deletePayment(paymentId: String) {
        coroutineScope.launch {
            try {
                val success = paymentRepository.deleteById(paymentId)
                if (success) {
                    loadPayments()
                    _error.value = null
                } else {
                    _error.value = "❌ Uplata nije pronađena za brisanje!"
                }
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom brisanja uplate: ${e.localizedMessage}"
            }
        }
    }

    // Opcionalno: filteri po user-u ili lokaciji

    fun findPaymentsByUser(userId: String, onResult: (List<Payment>) -> Unit) {
        coroutineScope.launch {
            try {
                val userPayments = paymentRepository.findByUser(userId)
                onResult(userPayments)
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dohvatanja uplata korisnika: ${e.localizedMessage}"
                onResult(emptyList())
            }
        }
    }

    fun findPaymentsByParkingLocation(locationId: String, onResult: (List<Payment>) -> Unit) {
        coroutineScope.launch {
            try {
                val locationPayments = paymentRepository.findByParkingLocation(locationId)
                onResult(locationPayments)
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dohvatanja uplata za lokaciju: ${e.localizedMessage}"
                onResult(emptyList())
            }
        }
    }
}
