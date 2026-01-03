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
                adapter.removeSimulation(simulation)
                dataManager.deleteSimulation(simulation)
                updateEmptyState()
                Toast.makeText(this, "Simulation deleted", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        updateEmptyState()

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
            }

            val message = if (isActive) "Simulation activated" else "Simulation deactivated"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Pokreće periodično slanje podataka za simulaciju
    private fun startSimulationInterval(simulation: Simulation) {
        if (simulationHandler == null) simulationHandler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {
                saveSimulationStep(simulation)
                simulationHandler?.postDelayed(this, getIntervalMillis(simulation.interval))
            }
        }

        activeSimulationsRunnables[simulation.id] = runnable
        simulationHandler?.post(runnable)
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
        val index = simulations.indexOfFirst { it.id == simulation.id }
        if (index != -1) {
            val updated = simulation.copy(
                runCount = simulation.runCount + 1,
                lastRun = Date()
            )
            simulations[index] = updated
            dataManager.updateSimulation(updated)
            adapter.notifyItemChanged(index)
        }

        sendSimulatedDataToBackend(simulation.value, simulation.location, simulation.type)
    }

    // Šalje simulirane podatke o parking mestima na server
    private fun sendSimulatedDataToBackend(value: String, location: String, type: SimulationType) {
        val coords = location.split(",")
        if (coords.size != 2) return

        val lat = coords[0].trim().toDoubleOrNull() ?: 0.0
        val lon = coords[1].trim().toDoubleOrNull() ?: 0.0
        val numValue = value.toIntOrNull() ?: 0
        var totalSpots = 0
        var freeSpaces = 0
        var occupiedSpaces = 0

        // Postavlja vrednosti u zavisnosti od tipa simulacije
        when (type) {
            SimulationType.TOTAL_SPACES -> totalSpots = numValue
            SimulationType.FREE_SPACES -> freeSpaces = numValue
            SimulationType.OCCUPIED_SPACES -> occupiedSpaces = numValue
            SimulationType.ALL -> {
                totalSpots = numValue
                freeSpaces = numValue
                occupiedSpaces = numValue
            }
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

        if (eventType == EventType.PARKING_FULL) {
            val event = Event(
                topic = "parking/full",
                message = "Parking is full",
                timestamp = System.currentTimeMillis(),
                location = "$lat,$lon"
            )
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
                adapter.addSimulation(it)
                dataManager.addSimulation(it)

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
