package com.example.parkingmate
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parkingmate.databinding.ActivitySimulationBinding
import java.util.*
import java.util.concurrent.TimeUnit

// Glavna aktivnost za prikaz i upravljanje simulacijama
class SimulationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationBinding
    private lateinit var adapter: SimulationAdapter
    private lateinit var dataManager: DataManager
    private var simulationHandler: Handler? = null
    private val activeSimulationsRunnables = mutableMapOf<String, Runnable>() // Mapa aktivnih simulacija

    private val simulations = mutableListOf<Simulation>()

    // Mapa za praćenje poslednjeg dogodka po simulaciji (sprečava duplikate)
    private val lastEventTypeBySimulation = mutableMapOf<String, EventType?>()
    
    // Mapa za praćenje vremena poslednjeg slanja dogodka po simulaciji (sprečava previše česta slanja)
    private val lastEventSentTimeBySimulation = mutableMapOf<String, Long>()
    
    // Minimalno vreme između slanja istog tipa dogodka (5 minuta)
    private val MIN_EVENT_INTERVAL_MS = 5 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = DataManager(this)

        // Učitava sačuvane simulacije
        val savedSimulations = dataManager.loadSimulations()
        simulations.addAll(savedSimulations)

        // Kreira adapter za RecyclerView
        adapter = SimulationAdapter(
            simulations = simulations,
            onSwitchChanged = { simulation, isChecked ->
                updateSimulationStatus(simulation, isChecked)
            },
            onItemClicked = { simulation ->
                showSimulationDetails(simulation)
            },
            onDeleteClicked = { simulation ->
                if (simulation.isActive) stopSimulationInterval(simulation)
                // Očisti praćenje dogodaka kada se simulacija obriše
                lastEventTypeBySimulation.remove(simulation.id)
                lastEventSentTimeBySimulation.remove(simulation.id)
                adapter.removeSimulation(simulation)
                dataManager.deleteSimulation(simulation)
                updateEmptyState()
                Toast.makeText(this, "Simulation deleted", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        updateEmptyState()

        // Dugme za nazad
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Dugme za dodavanje nove simulacije
        binding.fabAddSimulation.setOnClickListener {
            val intent = Intent(this, SimulationDetailActivity::class.java)
            startActivityForResult(intent, ADD_SIMULATION_REQUEST)
        }
    }

    // Ažurira status simulacije (aktivna/neaktivna)
    private fun updateSimulationStatus(simulation: Simulation, isActive: Boolean) {
        val index = simulations.indexOfFirst { it.id == simulation.id }
        if (index != -1) {
            val updated = simulation.copy(isActive = isActive)
            simulations[index] = updated
            adapter.notifyItemChanged(index)
            dataManager.updateSimulation(updated)

            if (isActive) {
                startSimulationInterval(updated)
            } else {
                stopSimulationInterval(updated)
                // Očisti praćenje dogodaka kada se simulacija zaustavi
                lastEventTypeBySimulation.remove(updated.id)
                lastEventSentTimeBySimulation.remove(updated.id)
            }

            val message = if (isActive) "Simulation activated" else "Simulation deactivated"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Pokreće periodično slanje podataka za simulaciju
    private fun startSimulationInterval(simulation: Simulation) {
        if (simulationHandler == null) simulationHandler = Handler(Looper.getMainLooper())

        Log.d("SIMULATION", "Pokretanje simulacije: ${simulation.name}, interval: ${simulation.interval}")

        val runnable = object : Runnable {
            override fun run() {
                Log.d("SIMULATION", "Izvršavanje koraka simulacije: ${simulation.name}")
                saveSimulationStep(simulation)
                simulationHandler?.postDelayed(this, getIntervalMillis(simulation.interval))
            }
        }

        activeSimulationsRunnables[simulation.id] = runnable
        simulationHandler?.post(runnable)
        Log.d("SIMULATION", "Simulacija pokrenuta: ${simulation.name}")
    }

    // Zaustavlja periodično slanje podataka za simulaciju
    private fun stopSimulationInterval(simulation: Simulation) {
        activeSimulationsRunnables[simulation.id]?.let {
            simulationHandler?.removeCallbacks(it)
        }
        activeSimulationsRunnables.remove(simulation.id)
    }

    // Pretvara vremenski interval iz stringa (HH:MM:SS) u milisekunde
    private fun getIntervalMillis(interval: String): Long {
        val parts = interval.split(":").map { it.toLongOrNull() ?: 0L }
        val hours = if (parts.size > 0) parts[0] else 0L
        val minutes = if (parts.size > 1) parts[1] else 0L
        val seconds = if (parts.size > 2) parts[2] else 0L
        return TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds)
    }

    // Izvršava jedan korak simulacije - ažurira brojač i šalje podatke
    private fun saveSimulationStep(simulation: Simulation) {
        Log.d("SIMULATION", "saveSimulationStep za: ${simulation.name}")
        
        val index = simulations.indexOfFirst { it.id == simulation.id }
        if (index != -1) {
            val updated = simulation.copy(
                runCount = simulation.runCount + 1,
                lastRun = Date()
            )
            simulations[index] = updated
            dataManager.updateSimulation(updated)
            adapter.notifyItemChanged(index)
        } else {
            Log.e("SIMULATION", "Simulacija nije pronađena u listi: ${simulation.name}, ID: ${simulation.id}")
        }

        sendSimulatedDataToBackend(simulation)
    }

    // Šalje simulirane podatke o parking mestima na server
    private fun sendSimulatedDataToBackend(simulation: Simulation) {
        val coords = simulation.location.split(",")
        if (coords.size != 2) return

        val lat = coords[0].trim().toDoubleOrNull() ?: 0.0
        val lon = coords[1].trim().toDoubleOrNull() ?: 0.0
        val numValue = simulation.value.toIntOrNull() ?: 0
        var totalSpots = 0
        var freeSpaces = 0
        var occupiedSpaces = 0

        // Postavlja vrednosti u zavisnosti od tipa simulacije
        when (simulation.type) {
            SimulationType.TOTAL_SPACES -> {
                totalSpots = numValue
                // Ako nije eksplicitno postavljeno, pretpostavi da su sva mesta slobodna
                if (freeSpaces == 0 && occupiedSpaces == 0) {
                    freeSpaces = totalSpots
                }
            }
            SimulationType.FREE_SPACES -> {
                freeSpaces = numValue
                // Ako totalSpots nije postavljen, postavi ga na realističan broj
                // Za detekciju LOW_AVAILABILITY (<=20%), totalSpots treba biti barem 5x freeSpaces
                // Primer: ako je freeSpaces=2, totalSpots treba biti barem 10 da bi bilo 20% dostupnosti
                if (totalSpots == 0) {
                    // Postavi totalSpots na minimum 10 ili 5x freeSpaces (šta je veće)
                    totalSpots = maxOf(10, freeSpaces * 5)
                    Log.d("SIMULATION", "FREE_SPACES: postavljen totalSpots=$totalSpots za freeSpaces=$freeSpaces")
                }
            }
            SimulationType.OCCUPIED_SPACES -> {
                occupiedSpaces = numValue
                // Ako totalSpots nije postavljen, koristi occupiedSpaces kao minimum
                if (totalSpots == 0) {
                    totalSpots = occupiedSpaces
                    freeSpaces = 0 // Ako su sva mesta zauzeta
                }
            }
            SimulationType.ALL -> {
                totalSpots = numValue
                freeSpaces = numValue
                occupiedSpaces = numValue
            }
        }
        
        // Ako totalSpots još uvek nije postavljen, postavi ga na osnovu freeSpaces + occupiedSpaces
        if (totalSpots == 0 && (freeSpaces > 0 || occupiedSpaces > 0)) {
            totalSpots = freeSpaces + occupiedSpaces
        }

        val eventType: EventType? =
            when {
                totalSpots > 0 && freeSpaces == 0 ->
                    EventType.PARKING_FULL

                totalSpots > 0 &&
                        freeSpaces > 0 &&
                        freeSpaces.toDouble() / totalSpots <= 0.2 ->
                    EventType.LOW_AVAILABILITY

                totalSpots > 0 ->
                    EventType.PARKING_AVAILABLE

                else -> null
            }

        // Debug logovi
        Log.d("SIMULATION", "Simulacija: ${simulation.name}, totalSpots: $totalSpots, freeSpaces: $freeSpaces, occupiedSpaces: $occupiedSpaces")
        
        // Automatsko slanje dogodka za ekstremne situacije
        if (eventType != null) {
            val simulationId = simulation.id
            val lastEventType = lastEventTypeBySimulation[simulationId]
            val lastSentTime = lastEventSentTimeBySimulation[simulationId] ?: 0L
            val currentTime = System.currentTimeMillis()
            
            Log.d("SIMULATION", "Detektovan dogodak: ${eventType.name} za simulaciju: ${simulation.name}")
            
            // Proveri da li se dogodak promenio ili je prošlo dovoljno vremena
            val eventChanged = eventType != lastEventType
            val enoughTimePassed = (currentTime - lastSentTime) >= MIN_EVENT_INTERVAL_MS
            
            // Definiši ekstremne dogodke koji se automatski šalju
            val isExtremeEvent = eventType == EventType.PARKING_FULL || 
                                 eventType == EventType.LOW_AVAILABILITY
            
            Log.d("SIMULATION", "isExtremeEvent: $isExtremeEvent, eventChanged: $eventChanged, enoughTimePassed: $enoughTimePassed")
            
            // Pošalji dogodak ako:
            // 1. Dogodak se promenio (npr. iz AVAILABLE u FULL)
            // 2. ILI je ekstremni dogodak i prošlo je dovoljno vremena (sprečava spam)
            if (isExtremeEvent && (eventChanged || enoughTimePassed)) {
                Log.d("SIMULATION", "Uslovi ispunjeni - šalje se dogodak!")
                val event = when (eventType) {
                    EventType.PARKING_FULL -> Event(
                        _id = "",
                        topic = "parking/full",
                        message = "Parking is full at location $lat,$lon. Total spots: $totalSpots, Free: $freeSpaces",
                        timestamp = currentTime,
                        location = "$lat,$lon",
                        eventType = eventType,
                        status = "PENDING",
                        blockchainHash = null,
                        blockchainTimestamp = null,
                        createdAt = null,
                        updatedAt = null
                    )

                    EventType.LOW_AVAILABILITY -> {
                        val availabilityPercent = (freeSpaces.toDouble() / totalSpots * 100).toInt()
                        Event(
                            _id = "",
                            topic = "parking/low-availability",
                            message = "Low parking availability at location $lat,$lon. Only $availabilityPercent% free ($freeSpaces/$totalSpots spots available)",
                            timestamp = currentTime,
                            location = "$lat,$lon",
                            eventType = eventType,
                            status = "PENDING",
                            blockchainHash = null,
                            blockchainTimestamp = null,
                            createdAt = null,
                            updatedAt = null
                        )
                    }

                    EventType.PARKING_AVAILABLE -> Event(
                        _id = "",
                        topic = "parking/available",
                        message = "Parking is available at location $lat,$lon",
                        timestamp = currentTime,
                        location = "$lat,$lon",
                        eventType = eventType,
                        status = "PENDING",
                        blockchainHash = null,
                        blockchainTimestamp = null,
                        createdAt = null,
                        updatedAt = null
                    )
                }

                // Automatski pošalji dogodak na backend
                sendEventAutomatically(event, eventType, simulationId)
                
                // Ažuriraj praćenje
                lastEventTypeBySimulation[simulationId] = eventType
                lastEventSentTimeBySimulation[simulationId] = currentTime
                
                Log.d("AUTO_EVENT", "Automatski poslat dogodak: ${eventType.name} za simulaciju: ${simulation.name}")
            } else if (eventType != lastEventType) {
                // Ažuriraj poslednji dogodak čak i ako ga ne šaljemo (za praćenje promena)
                lastEventTypeBySimulation[simulationId] = eventType
            }
        }


        val urvrvResultJson = """
        {
            "totalSpots": $totalSpots,
            "freeSpaces": $freeSpaces,
            "occupiedSpaces": $occupiedSpaces,
            "spotsCoordinates": []
        }
        """.trimIndent()

        val json = """
        {
            "parkingLocationId": "64a9b8c2f0a5c1234567890b",
            "coordinates": "$lon,$lat",
            "timestamp": ${System.currentTimeMillis()},
            "imageUrl": "simulated.jpg",
            "urvrvResult": $urvrvResultJson
        }
        """.trimIndent()

        ApiClient.uploadSimulatedData(json)

    }

    // Automatski šalje dogodak na backend server
    private fun sendEventAutomatically(event: Event, eventType: EventType, simulationId: String) {
        ApiClient.sendEvent(event, eventType) { success, errorMessage ->
            if (success) {
                Log.d("AUTO_EVENT", "Dogodak uspešno poslat: ${eventType.name}")
                // Opciono: prikaži notifikaciju korisniku
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Automatically sent event: ${eventType.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.e("AUTO_EVENT", "Greška pri slanju dogodka: $errorMessage")
                // Ne prikazuj grešku korisniku za automatske dogodke (da ne smeta)
            }
        }
    }

    // Prikazuje detalje simulacije u Toast poruci
    private fun showSimulationDetails(simulation: Simulation) {
        Toast.makeText(this,
            "${simulation.name}\n${simulation.type.name}: ${simulation.value}",
            Toast.LENGTH_SHORT).show()
    }

    // Obrada rezultata iz SimulationDetailActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_SIMULATION_REQUEST && resultCode == RESULT_OK) {
            val simulation = data?.getSerializableExtra("new_simulation") as? Simulation
            simulation?.let {
                // Sačuvaj u DataManager prvo
                dataManager.addSimulation(it)
                
                // Dodaj u listu (adapter koristi istu listu, tako da će se automatski ažurirati)
                simulations.add(0, it)
                adapter.notifyItemInserted(0)

                // Ako je simulacija aktivna, pokreni interval automatski
                if (it.isActive) {
                    startSimulationInterval(it)
                    Log.d("SIMULATION", "Automatski pokrenuta simulacija: ${it.name}")
                }

                binding.recyclerView.smoothScrollToPosition(0)
                Toast.makeText(this, "Simulation added!", Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
        }
    }

    // Ažurira prikaz kada je lista prazna
    private fun updateEmptyState() {
        if (simulations.isEmpty()) {
            binding.tvEmptyList.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmptyList.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    companion object {
        const val ADD_SIMULATION_REQUEST = 1001 // Request kod za dodavanje simulacije
    }
}
