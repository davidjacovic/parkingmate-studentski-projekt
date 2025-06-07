import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.db.UserRepository
import org.example.db.VehicleRepository
import org.example.Vehicle
import User

class UserAdminViewModel(
    private val userRepository: UserRepository,
    private val vehicleRepository: VehicleRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        coroutineScope.launch {
            try {
                _users.value = userRepository.findVisible()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom učitavanja korisnika: ${e.localizedMessage}"
            }
        }
    }

    fun addUser(user: User) {
        coroutineScope.launch {
            try {
                userRepository.insert(user)
                user.vehicles.forEach { vehicle ->
                    vehicleRepository.insert(vehicle.copy(user = user.id))
                }
                loadUsers()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dodavanja korisnika: ${e.localizedMessage}"
            }
        }
    }

    fun updateUser(user: User) {
        coroutineScope.launch {
            try {
                val success = userRepository.update(user)
                if (success) {
                    // 1️⃣ Dohvati stara vozila
                    val oldVehicles = vehicleRepository.findByUser(user.id.toHexString())

                    // 2️⃣ Obrisi izbrisana
                    val newVehicleIds = user.vehicles.mapNotNull { it.id }
                    oldVehicles.forEach { oldVehicle ->
                        if (oldVehicle.id !in newVehicleIds) {
                            vehicleRepository.deleteById(oldVehicle.id.toHexString())
                        }
                    }

                    // 3️⃣ Dodaj ili ažuriraj nova
                    user.vehicles.forEach { vehicle ->
                        val vehicleInDb = vehicle.id?.let { vehicleRepository.findById(it.toHexString()) }
                        if (vehicleInDb == null) {
                            val newVehicle = vehicle.copy(user = user.id)
                            vehicleRepository.insert(newVehicle)
                            println("✅ Ubacio novo vozilo: ${newVehicle.registration_number}")
                        } else {
                            val updatedVehicle = vehicle.copy(user = user.id)  // 🔥 dodaj user-a
                            vehicleRepository.update(updatedVehicle)
                            println("✅ Ažurirano vozilo: ${vehicle.registration_number}")
                        }
                    }


                    loadUsers()
                    _error.value = null
                } else {
                    _error.value = "❌ Neuspešno ažuriranje korisnika!"
                }
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom ažuriranja: ${e.localizedMessage}"
            }
        }
    }


    fun deleteUser(user: User) {
        coroutineScope.launch {
            try {
                // 1️⃣ Prvo obriši sva vozila tog korisnika
                vehicleRepository.findByUser(user.id.toHexString()).forEach { vehicle ->
                    vehicleRepository.deleteById(vehicle.id.toHexString())
                }
                // 2️⃣ Onda obriši korisnika
                val success = userRepository.deleteById(user.id.toHexString())
                if (success) {
                    loadUsers()
                    _error.value = null
                } else {
                    _error.value = "❌ Korisnik nije pronađen za brisanje!"
                }
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom brisanja: ${e.localizedMessage}"
            }
        }
    }


    fun getVehiclesForUser(userId: String, onResult: (List<Vehicle>) -> Unit) {
        coroutineScope.launch {
            try {
                val vehicles = vehicleRepository.findByUser(userId)
                onResult(vehicles)
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dohvatanja vozila: ${e.localizedMessage}"
                onResult(emptyList())
            }
        }
    }

    fun addVehicleToUser(userId: String, vehicle: Vehicle) {
        coroutineScope.launch {
            try {
                vehicleRepository.insert(vehicle.copy(user = org.bson.types.ObjectId(userId)))
                loadUsers()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "❌ Greška prilikom dodavanja vozila: ${e.localizedMessage}"
            }
        }
    }
}
