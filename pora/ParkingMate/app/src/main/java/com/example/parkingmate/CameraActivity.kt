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
import java.text.SimpleDateFormat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

    private fun updateLocationAndTime() {
        getCurrentLocation { success, lat, lon, timestamp ->

            if (success && lat != null && lon != null && timestamp != null) {

                locationAvailable = true
                lastLat = lat
                lastLon = lon
                lastTimestamp = timestamp

                val formattedTime = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date(timestamp))

                binding.tvData.text =
                    "Lat: $lat\nLon: $lon\nVreme: $formattedTime"

            } else {

                locationAvailable = false
                binding.tvData.text =
                    "Lokacija nije dostupna\nUključite GPS ili sačekajte signal"
            }
        }
    }

    private fun takePhoto() {

        if (!locationAvailable) {
            Toast.makeText(
                this,
                "Nije moguće slikati bez dostupne lokacije",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        imageCapture ?: return

        val photoFile = File(
            getOutputDirectory(),
            "parking_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val formattedTime = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date(lastTimestamp!!))

                    binding.tvData.text =
                        "Lat: $lastLat\nLon: $lastLon\nVreme: $formattedTime"

                    //saveMetadata(
                      //  photoFile,
                      //  lastLat!!,
                      //  lastLon!!,
                       // formattedTime
                    //)
                    // ID parking lokacije (za početak možeš hardkodirati)
                    val parkingLocationId = "64a9b8c2f0a5c1234567890a" // primer Mongo ObjectId

// Šalji samo sliku, sve ostalo kao null
                    ApiClient.uploadParkingImage(
                        parkingLocationId,
                        lat = 0.0,      // šaljemo 0 ili može null ako backend podržava
                        lon = 0.0,      // šaljemo 0 ili null
                        imagePath = photoFile.absolutePath
                    )


                    Toast.makeText(
                        this@CameraActivity,
                        "Slikano i sačuvani podaci",
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun getOutputDirectory(): File {
        return externalMediaDirs.firstOrNull()?.let {
            File(it, "ParkingMatePhotos").apply { mkdirs() }
        } ?: filesDir
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

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
                    "Permisije odbijene",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}
