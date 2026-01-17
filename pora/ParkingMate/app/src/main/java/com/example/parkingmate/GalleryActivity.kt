package com.example.parkingmate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.parkingmate.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private val images = mutableListOf<File>()

    // ✅ runtime permission za čitanje slika iz /sdcard/Pictures/...
    private val requestReadImagesPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                refreshImages()
            } else {
                Toast.makeText(this, "Permission denied - can't read images", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ postavi adapter odmah (da refresh radi)
        adapter = GalleryAdapter(images) { file, position ->
            showDeleteConfirmationDialog(file, position)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        // ✅ 1) traži permission, pa tek onda kopiraj + učitaj
        ensurePermissionAndLoad()
    }

    private fun ensurePermissionAndLoad() {
        val perm = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            refreshImages()
        } else {
            requestReadImagesPermission.launch(perm)
        }
    }

    private fun refreshImages() {
        // (opciono) kopiraj iz assets ako želiš i dalje to
        copyAssetsUploadsToGallery()

        images.clear()
        images.addAll(loadImages())
        adapter.notifyDataSetChanged()

        Toast.makeText(this, "Loaded images = ${images.size}", Toast.LENGTH_LONG).show()
        if (images.isEmpty()) {
            Toast.makeText(this, "No photos saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(file: File, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete this image?")
            .setPositiveButton("Yes") { _, _ ->
                deleteImage(file, position)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteImage(file: File, position: Int) {
        try {
            if (file.exists() && file.delete()) {
                adapter.removeItem(position)
                Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show()

                if (images.isEmpty()) {
                    Toast.makeText(this, "No photos saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Error deleting image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Čita iz /sdcard/Pictures/ParkingMate (isto kao što si ubacila na emulator)
    private fun getOutputDirectory(): File {
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(pictures, "ParkingMatePhotos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun loadImages(): List<File> {
        val dir = getOutputDirectory()
        return dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in listOf("png","jpg", "jpeg", "webp") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    // ✅ Kopira slike iz assets/uploads u /sdcard/Pictures/ParkingMate
    // (Ako ne koristiš assets više, slobodno izbriši ovu funkciju i poziv u refreshImages())
    private fun copyAssetsUploadsToGallery() {
        try {
            val targetDir = getOutputDirectory()
            val assetManager = assets

            // debug: šta vidi u assets/uploads
            val files = assetManager.list("uploads")
            Toast.makeText(
                this,
                "assets/uploads = ${files?.joinToString() ?: "NULL"}",
                Toast.LENGTH_LONG
            ).show()

            if (files.isNullOrEmpty()) return

            var copied = 0
            files.forEach { fileName ->
                val lower = fileName.lowercase()
                val isImage = lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                        lower.endsWith(".png") || lower.endsWith(".webp")
                if (!isImage) return@forEach

                val outFile = File(targetDir, fileName)
                if (outFile.exists()) return@forEach

                assetManager.open("uploads/$fileName").use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                copied++
            }

            Toast.makeText(
                this,
                "Copied $copied -> ${targetDir.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this, "copy error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
