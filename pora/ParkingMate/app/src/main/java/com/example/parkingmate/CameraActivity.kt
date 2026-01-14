package com.example.parkingmate

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.parkingmate.databinding.ActivityCameraBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.util.*

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var locationAvailable = false
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var lastTimestamp: Long? = null

    private val REQUEST_CODE_PERMISSIONS = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Inicijalizacija aktivnosti i UI elementata
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Dugme za nazad
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCapture.setOnClickListener { takePhoto() }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    // Pokreće kameru i postavlja preview
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                updateLocationAndTime()

            } catch (e: Exception) {
                Log.e("CameraX", "Camera start failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Ažurira trenutnu lokaciju i vreme prikazano na ekranu
    private fun updateLocationAndTime() {
        getCurrentLocation { success, lat, lon, timestamp ->

            if (success && lat != null && lon != null && timestamp != null) {

                locationAvailable = true
                lastLat = lat
                lastLon = lon
                lastTimestamp = timestamp

                val timestampMillis = lastTimestamp ?: System.currentTimeMillis()

                binding.tvData.text =
                    "Lat: $lat\nLon: $lon\nVreme: $timestampMillis"

            } else {

                locationAvailable = false
                binding.tvData.text =
                    "Location not available\\nTurn on GPS or wait for signal"
            }
        }
    }

    // Snima fotografiju parkinga
    private fun takePhoto() {

        if (!locationAvailable) {
            Toast.makeText(
                this,
                "Cannot take a photo without an available location",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        imageCapture ?: return

        val photoFile = File(
            getOutputDirectory(),
            "parking_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val timestampMillis = lastTimestamp ?: System.currentTimeMillis()

                    binding.tvData.text =
                        "Lat: $lastLat\nLon: $lastLon\nVreme: ${Date(timestampMillis)}"
                    val parkingLocationId = "64a9b8c2f0a5c1234567890b"

                    // Šalje sliku i meta-podatke na server
                    ApiClient.uploadParkingImage(
                        parkingLocationId = parkingLocationId,
                        lat = lastLat ?: 0.0,
                        lon = lastLon ?: 0.0,
                        timestamp = timestampMillis,
                        imagePath = photoFile.absolutePath
                    )


                    Toast.makeText(
                        this@CameraActivity,
                        "Photo taken and data saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Error while taking photo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Dobavlja trenutnu GPS lokaciju uređaja
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(
        onResult: (Boolean, Double?, Double?, Long?) -> Unit
    ) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(false, null, null, null)
            return
        }

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->

            if (location != null) {
                val timestamp = System.currentTimeMillis()
                onResult(true, location.latitude, location.longitude, timestamp)
            } else {
                onResult(false, null, null, null)
            }
        }
    }

    // Sačuva meta-podatke (lokacija, vreme) u JSON fajl (trenutno nekorisćeno)
    private fun saveMetadata(
        imageFile: File,
        lat: Double,
        lon: Double,
        time: String
    ) {
        val jsonFile = File(
            imageFile.parent,
            imageFile.nameWithoutExtension + ".json"
        )

        val content = """
        {
          "lat": $lat,
          "lon": $lon,
          "time": "$time"
        }
        """.trimIndent()

        jsonFile.writeText(content)
    }

    // Vraća direktorijum za čuvanje fotografija
    private fun getOutputDirectory(): File {
        return externalMediaDirs.firstOrNull()?.let {
            File(it, "ParkingMatePhotos").apply { mkdirs() }
        } ?: filesDir
    }

    // Proverava da li su sve potrebne permisije odobrene
    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    // Obrada rezultata zahteva za permisije
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions denied",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}