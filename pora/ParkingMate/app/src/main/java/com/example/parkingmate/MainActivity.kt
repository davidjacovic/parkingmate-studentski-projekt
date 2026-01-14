package com.example.parkingmate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSensors.setOnClickListener {
            startActivity(Intent(this, SensorsActivity::class.java))
        }

        binding.btnSimulation.setOnClickListener {
            startActivity(Intent(this, SimulationActivity::class.java))
        }

        binding.btnMessages.setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }

        binding.btnAlerts.setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }

        binding.btnSendEvent.setOnClickListener {
            startActivity(Intent(this, SendEventActivity::class.java))
        }
    }
}
