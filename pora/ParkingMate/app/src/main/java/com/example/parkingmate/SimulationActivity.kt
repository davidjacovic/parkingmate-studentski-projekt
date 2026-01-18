package com.example.parkingmate

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parkingmate.databinding.ActivitySimulationBinding
import java.util.Date
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

        // Učitava sačuvane simulacije (pretpostavka: Simulation sad ima polje total:Int)
        val savedSimulations = dataManager.loadSimulations()
        simulations.addAll(savedSimulations)

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

        binding.btnBack.setOnClickListener { finish() }

        binding.fabAddSimulation.setOnClickListener {
            val intent = Intent(this, SimulationDetailActivity::class.java)
            startActivityForResult(intent, ADD_SIMULATION_REQUEST)
        }
    }

    private fun updateSimulationStatus(simulation: Simulation, isActive: Boolean) {
        val index = simulations.indexOfFirst { it.id == simulation.id }
        if (index == -1) return

        val updated = simulation.copy(isActive = isActive)
        simulations[index] = updated
        adapter.notifyItemChanged(index)
        dataManager.updateSimulation(updated)

        if (isActive) {
            startSimulationInterval(updated)
        } else {
            stopSimulationInterval(updated)
            lastEventTypeBySimulation.remove(updated.id)
            lastEventSentTimeBySimulation.remove(updated.id)
        }

        val msg = if (isActive) "Simulation activated" else "Simulation deactivated"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

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
    }

    private fun stopSimulationInterval(simulation: Simulation) {
        activeSimulationsRunnables[simulation.id]?.let {
            simulationHandler?.removeCallbacks(it)
        }
        activeSimulationsRunnables.remove(simulation.id)
    }

    private fun getIntervalMillis(interval: String): Long {
        val parts = interval.split(":").map { it.toLongOrNull() ?: 0L }
        val hours = parts.getOrElse(0) { 0L }
        val minutes = parts.getOrElse(1) { 0L }
        val seconds = parts.getOrElse(2) { 0L }
        return TimeUnit.HOURS.toMillis(hours) +
                TimeUnit.MINUTES.toMillis(minutes) +
                TimeUnit.SECONDS.toMillis(seconds)
    }

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

    // ✅ NOVO: total je uvek iz simulation.total, type je samo FREE_SPACES ili OCCUPIED_SPACES
    private fun sendSimulatedDataToBackend(simulation: Simulation) {
        val coords = simulation.location.split(",")
        if (coords.size != 2) return

        val lat = coords[0].trim().toDoubleOrNull() ?: 0.0
        val lon = coords[1].trim().toDoubleOrNull() ?: 0.0

        val totalSpots = simulation.total
        if (totalSpots <= 0) {
            Log.e("SIMULATION", "Invalid totalSpots=${simulation.total} for simulation ${simulation.id}")
            return
        }

        val numValue = simulation.value.toIntOrNull() ?: 0
        val clampedValue = numValue.coerceIn(0, totalSpots)

        val freeSpaces: Int
        val occupiedSpaces: Int

        when (simulation.type) {
            SimulationType.FREE_SPACES -> {
                freeSpaces = clampedValue
                occupiedSpaces = (totalSpots - freeSpaces).coerceAtLeast(0)
            }
            SimulationType.OCCUPIED_SPACES -> {
                occupiedSpaces = clampedValue
                freeSpaces = (totalSpots - occupiedSpaces).coerceAtLeast(0)
            }
            else -> {
                // Ako ti u bazi ostanu stare simulacije, ne šalji ih
                Log.w("SIMULATION", "Unsupported type=${simulation.type} (expected FREE/OCCUPIED). Skipping.")
                return
            }
        }

        val eventType: EventType? =
            when {
                freeSpaces == 0 -> EventType.PARKING_FULL
                freeSpaces.toDouble() / totalSpots <= 0.2 -> EventType.LOW_AVAILABILITY
                else -> EventType.PARKING_AVAILABLE
            }

        Log.d(
            "SIMULATION",
            "Simulacija: ${simulation.name}, totalSpots: $totalSpots, freeSpaces: $freeSpaces, occupiedSpaces: $occupiedSpaces"
        )

        if (eventType != null) {
            val simulationId = simulation.id
            val lastEventType = lastEventTypeBySimulation[simulationId]
            val lastSentTime = lastEventSentTimeBySimulation[simulationId] ?: 0L
            val currentTime = System.currentTimeMillis()

            val eventChanged = eventType != lastEventType
            val enoughTimePassed = (currentTime - lastSentTime) >= MIN_EVENT_INTERVAL_MS
            val isExtremeEvent =
                eventType == EventType.PARKING_FULL || eventType == EventType.LOW_AVAILABILITY

            if (isExtremeEvent && (eventChanged || enoughTimePassed)) {
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

                sendEventAutomatically(event, eventType, simulationId)

                lastEventTypeBySimulation[simulationId] = eventType
                lastEventSentTimeBySimulation[simulationId] = currentTime

                Log.d("AUTO_EVENT", "Automatski poslat događaj: ${eventType.name} za simulaciju: ${simulation.name}")
            } else if (eventType != lastEventType) {
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

    private fun sendEventAutomatically(event: Event, eventType: EventType, simulationId: String) {
        ApiClient.sendEvent(event, eventType) { success, errorMessage ->
            if (success) {
                Log.d("AUTO_EVENT", "Dogodak uspešno poslat: ${eventType.name}")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Automatically sent event: ${eventType.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.e("AUTO_EVENT", "Greška pri slanju događaja: $errorMessage")
            }
        }
    }

    private fun showSimulationDetails(simulation: Simulation) {
        Toast.makeText(
            this,
            "${simulation.name}\nTotal: ${simulation.total}\n${simulation.type.name}: ${simulation.value}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_SIMULATION_REQUEST && resultCode == RESULT_OK) {
            val simulation = data?.getSerializableExtra("new_simulation") as? Simulation
            simulation?.let {
                dataManager.addSimulation(it)
                simulations.add(0, it)
                adapter.notifyItemInserted(0)

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
        const val ADD_SIMULATION_REQUEST = 1001
    }
}
