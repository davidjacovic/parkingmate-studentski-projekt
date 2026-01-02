package com.example.parkingmate

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivityPhotoDetailBinding
import java.io.File
import org.json.JSONObject

class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding

    // Aktivnost za prikaz detalja o fotografiji i njenih meta-podataka
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dobija putanju do slike iz intenta
        val imagePath = intent.getStringExtra("imagePath") ?: return
        val imageFile = File(imagePath)

        // Prikazuje sliku u ImageView-u
        binding.imageView.setImageURI(Uri.fromFile(imageFile))

        // Pokušava da pronađe JSON fajl sa meta-podacima (istog imena kao slika)
        val jsonFile = File(imageFile.parent, imageFile.nameWithoutExtension + ".json")

        // Ako JSON fajl postoji, parsira i prikazuje meta-podatke
        if (jsonFile.exists()) {
            val json = JSONObject(jsonFile.readText())
            binding.tvInfo.text =
                "Lat: ${json.getDouble("lat")}\n" +
                        "Lon: ${json.getDouble("lon")}\n" +
                        "Time: ${json.getString("time")}"
        } else {
            binding.tvInfo.text = "No data for this image"
        }
    }
}
