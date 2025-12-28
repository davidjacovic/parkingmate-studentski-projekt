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

        binding.spinnerSimulationType.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val type = SimulationType.values()[position]
                    updateHint(type)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }
    private fun updateHint(type: SimulationType) {
        binding.etValue.text.clear()

        binding.etValue.hint = when (type) {
            SimulationType.TOTAL_SPACES -> "Unesi ukupan broj mesta"
            SimulationType.FREE_SPACES -> "Unesi broj slobodnih mesta"
            SimulationType.OCCUPIED_SPACES -> "Unesi broj zauzetih mesta"
            SimulationType.ALL -> "Unesi simuliranu vrednost"
        }
    }


    private fun setupListeners() {

        binding.btnConfirm.setOnClickListener {

            val type = SimulationType.values()[
                binding.spinnerSimulationType.selectedItemPosition
            ]

            val value = binding.etValue.text.toString()

            if (value.isEmpty()) {
                Toast.makeText(this, "Unesi vrednost", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Tip: ${type.name}, vrednost: $value",
                Toast.LENGTH_SHORT
            ).show()
        }


        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
}
