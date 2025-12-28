package com.example.parkingmate

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivitySimulationDetailBinding

class SimulationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimulationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupListeners()
    }

    private fun setupSpinner() {
        val simulationTypes = SimulationType.values().map { it.name }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            simulationTypes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerSimulationType.adapter = adapter
    }

    private fun setupListeners() {

        binding.btnConfirm.setOnClickListener {
            val selectedType =
                binding.spinnerSimulationType.selectedItem.toString()

            Toast.makeText(
                this,
                "Izabran tip: $selectedType",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
}
