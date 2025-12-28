package com.example.parkingmate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parkingmate.databinding.ActivitySimulationBinding

class SimulationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationBinding
    private lateinit var adapter: SimulationAdapter

    private val simulationList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SimulationAdapter(simulationList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabAddSimulation.setOnClickListener {
            val intent = Intent(this, SimulationDetailActivity::class.java)
            startActivity(intent)
        }
    }
}
