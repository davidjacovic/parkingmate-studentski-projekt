package com.example.parkingmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parkingmate.databinding.ActivitySimulationBinding

class SimulationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationBinding
    private lateinit var adapter: SimulationAdapter
    private lateinit var dataManager: DataManager

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

            val message = if (isActive) "Simulacija aktivirana" else "Simulacija deaktivirana"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
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