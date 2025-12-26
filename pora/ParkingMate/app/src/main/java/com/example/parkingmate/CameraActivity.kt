package com.example.parkingmate

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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
import com.google.android.gms.location.*
import java.io.File
import java.io.FileOutputStream

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var logFile: File

    private val REQUEST_CODE_PERMISSIONS = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        logFile = File(filesDir, "parking_data.txt")

        checkGpsEnabled()

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

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
            } catch (e: Exception) {
                Log.e("CameraX", "Camera start failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
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
                    getLocation { lat, lon ->
                        val timestamp = System.currentTimeMillis()

                        val line = "${photoFile.name}, $lat, $lon, $timestamp\n"
                        FileOutputStream(logFile, true).bufferedWriter().use {
                            it.append(line)
                        }

                        binding.tvData.text =
                            "Lat: $lat\nLon: $lon\nVreme: $timestamp"

                        Toast.makeText(
                            this@CameraActivity,
                            "Slika + lokacija sačuvani",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.d("PARKING_DATA", "Upisano: $line")
                        Log.d("PARKING_DATA", "Fajl: ${logFile.absolutePath}")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Greška pri slikanju",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
    @SuppressLint("MissingPermission")
    private fun getLocation(onLocation: (lat: Double, lon: Double) -> Unit) {
        val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager

        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null

        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }

        if (bestLocation != null) {
            onLocation(bestLocation.latitude, bestLocation.longitude)
        } else {
            Toast.makeText(this, "Lokacija nije dostupna", Toast.LENGTH_SHORT).show()
            onLocation(0.0, 0.0)
        }
    }

    private fun checkGpsEnabled() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(
                this,
                "UKLJUČI GPS (Location) za tačne podatke",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "ParkingMatePhotos").apply { mkdirs() }

        }
        return mediaDir ?: filesDir
        Log.d("FILE", "Data written to: ${logFile.absolutePath}")

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permisije odbijene", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

}
