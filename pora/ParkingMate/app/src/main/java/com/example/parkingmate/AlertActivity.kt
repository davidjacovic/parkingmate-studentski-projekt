package com.example.parkingmate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivityAlertBinding

class AlertsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlertBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dugme za nazad
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
