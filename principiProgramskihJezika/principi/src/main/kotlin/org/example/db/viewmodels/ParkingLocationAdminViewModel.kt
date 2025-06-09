package org.example.db.viewmodels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.ParkingLocation
import org.example.Tariff
import org.example.db.ParkingLocationRepository
import org.example.db.TariffRepository

class ParkingLocationAdminViewModel(
    private val parkingLocationRepo: ParkingLocationRepository,
    private val tariffRepo: TariffRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _parkingLocations = MutableStateFlow<List<ParkingLocation>>(emptyList())
    val parkingLocations: StateFlow<List<ParkingLocation>> = _parkingLocations.asStateFlow()


    private val _tariffs = MutableStateFlow<List<Tariff>>(emptyList())
    val tariffs: StateFlow<List<Tariff>> = _tariffs.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAll()
    }

    private fun loadAll() {
        coroutineScope.launch {
            try {
                _parkingLocations.value = parkingLocationRepo.findAll()
                _tariffs.value = tariffRepo.findAllVisible()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom učitavanja: ${e.localizedMessage}"
            }
        }
    }

    fun addParkingLocation(parkingLocation: ParkingLocation,  tariffs: List<Tariff>) {
        coroutineScope.launch {
            try {


                // 2️⃣ Dodaj parkingLocation (koristi ID subscriber-a)
                val newLocation = parkingLocation.copy()
                parkingLocationRepo.insert(newLocation)

                // 3️⃣ Dodaj tarife
                tariffs.forEach { tariffRepo.insert(it) }

                loadAll()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dodavanja: ${e.localizedMessage}"
            }
        }
    }

    fun updateParkingLocation(updatedLocation: ParkingLocation) {
        coroutineScope.launch {
            try {
                parkingLocationRepo.update(updatedLocation)
                loadAll()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom ažuriranja: ${e.localizedMessage}"
            }
        }
    }

    fun deleteParkingLocation(id: String) {
        coroutineScope.launch {
            try {
                val success = parkingLocationRepo.deleteById(id)
                if (success) {
                    loadAll()
                    _error.value = null
                } else {
                    _error.value = "❌ Parking lokacija nije pronađena za brisanje!"
                }
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom brisanja: ${e.localizedMessage}"
            }
        }
    }

    fun addTariff(tariff: Tariff) {
        coroutineScope.launch {
            try {
                tariffRepo.insert(tariff)
                loadAll()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dodavanja tarife: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTariff(id: String) {
        coroutineScope.launch {
            try {
                val success = tariffRepo.deleteById(id)
                if (success) {
                    loadAll()
                    _error.value = null
                } else {
                    _error.value = "❌ Tarifa nije pronađena za brisanje!"
                }
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom brisanja tarife: ${e.localizedMessage}"
            }
        }
    }
}