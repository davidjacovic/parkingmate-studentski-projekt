package com.example.parkingmate

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivityPhotoDetailBinding
import java.io.File
import org.json.JSONObject

class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagePath = intent.getStringExtra("imagePath") ?: return
        val imageFile = File(imagePath)

        binding.imageView.setImageURI(Uri.fromFile(imageFile))

        val jsonFile = File(imageFile.parent, imageFile.nameWithoutExtension + ".json")

        if (jsonFile.exists()) {
            val json = JSONObject(jsonFile.readText())
            binding.tvInfo.text =
                "Lat: ${json.getDouble("lat")}\n" +
                        "Lon: ${json.getDouble("lon")}\n" +
                        "Vreme: ${json.getString("time")}"
        } else {
            binding.tvInfo.text = "Nema podataka za ovu sliku"
        }
    }
}
