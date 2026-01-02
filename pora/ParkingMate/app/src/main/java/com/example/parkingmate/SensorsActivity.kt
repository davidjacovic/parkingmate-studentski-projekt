package com.example.parkingmate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivitySensorsBinding

class SensorsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensorsBinding

    // Glavna aktivnost za senzore - omogućava navigaciju ka kameri ili galeriji
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dugme za pokretanje aktivnosti kamere
        binding.btnCamera.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        // Dugme za pokretanje aktivnosti galerije
        binding.btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
    }
}
