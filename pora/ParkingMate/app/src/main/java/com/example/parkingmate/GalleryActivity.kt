package com.example.parkingmate

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.parkingmate.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val images = loadImages()
        if (images.isEmpty()) {
            Toast.makeText(this, "Nema snimljenih slika", Toast.LENGTH_SHORT).show()
        }

        adapter = GalleryAdapter(images)
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "ParkingMatePhotos").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun loadImages(): List<File> {
        val dir = getOutputDirectory()
        return dir.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }
}
