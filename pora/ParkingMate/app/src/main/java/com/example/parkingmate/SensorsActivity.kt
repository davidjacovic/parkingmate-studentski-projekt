package com.example.parkingmate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivitySensorsBinding

class SensorsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensorsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnCamera.setOnClickListener {
            startActivity(Intent(this, SensorPickImageActivity::class.java))
        }

        binding.btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
    }
}
