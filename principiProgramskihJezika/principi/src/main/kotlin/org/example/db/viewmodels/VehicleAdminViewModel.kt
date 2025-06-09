package org.example.db.viewmodels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.Vehicle
import org.example.db.VehicleRepository

class VehicleAdminViewModel(
    private val vehicleRepository: VehicleRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        coroutineScope.launch {
            try {
                _vehicles.value = vehicleRepository.findAll().filter { it.hidden != true }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Error loading vehicles: ${e.localizedMessage}"
            }
        }
    }

    fun addVehicle(vehicle: Vehicle) {
        coroutineScope.launch {
            try {
                vehicleRepository.insert(vehicle)
                loadVehicles()
            } catch (e: Exception) {
                _error.value = "❌ Error adding vehicle: ${e.localizedMessage}"
            }
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        coroutineScope.launch {
            try {
                val success = vehicleRepository.update(vehicle)
                if (success) loadVehicles()
                else _error.value = "❌ Update failed"
            } catch (e: Exception) {
                _error.value = "❌ Error updating vehicle: ${e.localizedMessage}"
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        coroutineScope.launch {
            try {
                val success = vehicleRepository.deleteById(vehicleId)
                if (success) loadVehicles()
                else _error.value = "❌ Delete failed"
            } catch (e: Exception) {
                _error.value = "❌ Error deleting vehicle: ${e.localizedMessage}"
            }
        }
    }
}
