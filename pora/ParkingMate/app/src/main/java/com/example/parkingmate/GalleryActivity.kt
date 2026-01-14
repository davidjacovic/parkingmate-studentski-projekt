package com.example.parkingmate

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.parkingmate.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private val images = mutableListOf<File>()

    // Aktivnost za prikaz galerije snimljenih fotografija parkinga
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Učitava sve slike iz direktorijuma
        images.addAll(loadImages())
        if (images.isEmpty()) {
            Toast.makeText(this, "No photos saved", Toast.LENGTH_SHORT).show()
        }

        // Inicijalizuje adapter za RecyclerView sa grid rasporedom (2 kolone)
        adapter = GalleryAdapter(images) { file, position ->
            showDeleteConfirmationDialog(file, position)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter

        // Dugme za nazad
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // Prikazuje dialog za potvrdu brisanja
    private fun showDeleteConfirmationDialog(file: File, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Brisanje slike")
            .setMessage("Da li ste sigurni da želite da obrišete ovu sliku?")
            .setPositiveButton("Da") { _, _ ->
                deleteImage(file, position)
            }
            .setNegativeButton("Ne", null)
            .show()
    }

    // Briše sliku iz fajl sistema i iz liste
    private fun deleteImage(file: File, position: Int) {
        try {
            if (file.exists() && file.delete()) {
                adapter.removeItem(position)
                Toast.makeText(this, "Slika je obrisana", Toast.LENGTH_SHORT).show()
                
                // Ako nema više slika, prikaži poruku
                if (images.isEmpty()) {
                    Toast.makeText(this, "No photos saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Greška pri brisanju slike", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Greška: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Vraća direktorijum gde se čuvaju fotografije parkinga
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "ParkingMatePhotos").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    // Učitava sve slike iz direktorijuma, sortirane po datumu (najnovije prvo)
    private fun loadImages(): List<File> {
        val dir = getOutputDirectory()
        return dir.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }
}
