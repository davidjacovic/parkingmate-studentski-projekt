package com.example.parkingmate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivityMessageBinding

class MessagesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dugme za nazad
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}

