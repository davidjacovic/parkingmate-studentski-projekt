package com.example.parkingmate

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parkingmate.databinding.ActivitySimulationBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class SimulationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationBinding
    private lateinit var adapter: SimulationAdapter
    private lateinit var dataManager: DataManager
    private var simulationHandler: Handler? = null
    private val activeSimulationsRunnables = mutableMapOf<String, Runnable>()

    private val simulations = mutableListOf<Simulation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = DataManager(this)

        val savedSimulations = dataManager.loadSimulations()
        simulations.addAll(savedSimulations)

        adapter = SimulationAdapter(
            simulations = simulations,
            onSwitchChanged = { simulation, isChecked ->
                updateSimulationStatus(simulation, isChecked)
            },
            onItemClicked = { simulation ->
                showSimulationDetails(simulation)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        updateEmptyState()

        binding.fabAddSimulation.setOnClickListener {
            val intent = Intent(this, SimulationDetailActivity::class.java)
            startActivityForResult(intent, ADD_SIMULATION_REQUEST)
        }
    }

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

            val message = if (isActive) "Simulacija aktivirana" else "Simulacija deaktivirana"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
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

    private fun stopSimulationInterval(simulation: Simulation) {
        activeSimulationsRunnables[simulation.id]?.let {
            simulationHandler?.removeCallbacks(it)
        }
        activeSimulationsRunnables.remove(simulation.id)
    }

    private fun getIntervalMillis(interval: String): Long {
        val parts = interval.split(":").map { it.toLongOrNull() ?: 0L }
        val hours = if (parts.size > 0) parts[0] else 0L
        val minutes = if (parts.size > 1) parts[1] else 0L
        val seconds = if (parts.size > 2) parts[2] else 0L
        return TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds)
    }

    private fun saveSimulationStep(simulation: Simulation) {
        val newRecord = simulation.copy(
            id = UUID.randomUUID().toString(),
            createdAt = Date(),
            runCount = simulation.runCount + 1
        )
        dataManager.addSimulation(newRecord)
        sendSimulatedDataToBackend(newRecord.value, newRecord.location)
    }
    private fun sendSimulatedDataToBackend(value: String, location: String) {
        val coords = location.split(",")
        if (coords.size != 2) return

        val lat = coords[0].trim().toDoubleOrNull() ?: 0.0
        val lon = coords[1].trim().toDoubleOrNull() ?: 0.0

        val json = """
        {
            "parkingLocationId": "64a9b8c2f0a5c1234567890b",
            "coordinates": "$lon,$lat",
            "timestamp": ${System.currentTimeMillis()},
            "value": "$value"
        }
    """.trimIndent()

        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://10.0.2.2:3002/api/parking-images/simulated")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Simulated upload failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) println("Simulated upload successful")
                else println("Simulated upload error: ${response.code}")
            }
        })
    }

    private fun showSimulationDetails(simulation: Simulation) {
        Toast.makeText(this,
            "${simulation.name}\n${simulation.type.name}: ${simulation.value}",
            Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_SIMULATION_REQUEST && resultCode == RESULT_OK) {
            val simulation = data?.getSerializableExtra("new_simulation") as? Simulation
            simulation?.let {
                adapter.addSimulation(it)
                dataManager.addSimulation(it)

                binding.recyclerView.smoothScrollToPosition(0)
                Toast.makeText(this, "Simulacija dodata!", Toast.LENGTH_SHORT).show()
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