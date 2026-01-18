package com.example.parkingmate

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.parkingmate.databinding.ActivitySensorPickImageBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Locale

class SensorPickImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensorPickImageBinding

    // Map
    private lateinit var map: MapView
    private var currentMarker: Marker? = null
    private var selectedLat: Double? = null
    private var selectedLon: Double? = null

    // Image
    private var selectedImageUri: Uri? = null
    private var cameraTempFile: File? = null

    private var mlFree: Int? = null
    private var mlOccupied: Int? = null
    private var mlTotal: Int? = null


    // Permissions
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* ignore - user može opet kliknuti */ }

    // Gallery picker
    private val pickFromGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.setImageURI(uri)
            runMlAnalyzeAndShow(uri)
        }
    }

    // Camera capture (via URI)
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let {
                binding.ivPreview.setImageURI(it)
                runMlAnalyzeAndShow(it)
            }
        } else {
            // korisnik odustao
            selectedImageUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorPickImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupOsmDroid()
        setupMap()
        setupListeners()

        // Permissions koje realno trebaju za ovaj ekran:
        requestPermissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    private fun setupOsmDroid() {
        Configuration.getInstance().userAgentValue = packageName
        // cache folder default je ok; možeš dodati kao u simulaciji kasnije
    }

    private fun setupMap() {
        map = MapView(this)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(true)

        val startPoint = GeoPoint(44.7866, 20.4489) // BG default
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)

        binding.mapContainer.addView(map)
        setupMapClickListener()
    }

    private fun setupMapClickListener() {
        map.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                if (e == null || mapView == null) return false

                val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                setSelectedLocation(geoPoint.latitude, geoPoint.longitude, tryReverseGeocode = true)
                return true
            }
        })
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            val addressStr = binding.etAddress.text.toString().trim()
            if (addressStr.isEmpty()) {
                Toast.makeText(this, "Enter address first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            geocodeAddress(addressStr)
        }

        binding.btnPickGallery.setOnClickListener {
            pickFromGallery.launch("image/*")
        }

        binding.btnPickCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions.launch(arrayOf(Manifest.permission.CAMERA))
                Toast.makeText(this, "Allow Camera permission then tap Camera again", Toast.LENGTH_SHORT).show()
            } else {
                openCamera()
            }
        }


        binding.btnSave.setOnClickListener {
            validateAndFakeSave()
        }
    }

    private fun validateAndFakeSave() {
        val name = binding.etName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (address.isEmpty()) {
            Toast.makeText(this, "Address is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedLat == null || selectedLon == null) {
            Toast.makeText(this, "Select location on map or via address", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "Select an image (camera or gallery) before Save", Toast.LENGTH_SHORT).show()
            return
        }

        val free = mlFree
        val total = mlTotal
        if (free == null || total == null) {
            Toast.makeText(this, "Pick image and wait for ML analysis first", Toast.LENGTH_SHORT).show()
            return
        }

        //  PRAVI SAVE NA BACKEND
        ApiClient.saveParkingLocationByAddress(
            name = name,
            address = address,
            lat = selectedLat!!,
            lon = selectedLon!!,
            total = total,
            free = free
        ) { ok, msg ->
            runOnUiThread {
                if (ok) {
                    Toast.makeText(this, "Saved ✅ (created / updated)", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Save failed: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun setSelectedLocation(lat: Double, lon: Double, tryReverseGeocode: Boolean) {
        selectedLat = lat
        selectedLon = lon
        binding.tvLatLon.text = "Lat: %.6f   Lon: %.6f".format(Locale.US, lat, lon)

        val geoPoint = GeoPoint(lat, lon)
        addMarker(geoPoint, "Selected")

        if (tryReverseGeocode) {
            reverseGeocodeToAddress(lat, lon)
        }
    }

    private fun addMarker(geoPoint: GeoPoint, title: String) {
        currentMarker?.let { map.overlays.remove(it) }
        currentMarker = Marker(map).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
        }
        map.overlays.add(currentMarker)
        map.controller.animateTo(geoPoint)
        map.controller.setZoom(15.0)
        map.invalidate()
    }

    private fun geocodeAddress(address: String) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val results = geocoder.getFromLocationName(address, 1)

                runOnUiThread {
                    if (!results.isNullOrEmpty()) {
                        val r = results[0]
                        setSelectedLocation(r.latitude, r.longitude, tryReverseGeocode = false)
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Geocoder error", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun reverseGeocodeToAddress(lat: Double, lon: Double) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val results = geocoder.getFromLocation(lat, lon, 1)

                runOnUiThread {
                    if (!results.isNullOrEmpty()) {
                        val addr = results[0].getAddressLine(0) ?: ""
                        if (addr.isNotBlank()) binding.etAddress.setText(addr)
                    }
                }
            } catch (_: Exception) {
                // ignoriši ako reverse geocode failuje
            }
        }.start()
    }

    private fun openCamera() {
        try {
            val dir = File(cacheDir, "camera").apply { mkdirs() }
            val file = File(dir, "sensor_${System.currentTimeMillis()}.jpg")
            cameraTempFile = file

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            selectedImageUri = uri
            takePicture.launch(uri)

        } catch (e: Exception) {
            Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
            selectedImageUri = null
        }
    }

    private fun uriToCacheFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream from URI")

        val file = File(cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }
        return file
    }
    private fun runMlAnalyzeAndShow(uri: Uri) {
        binding.tvMlResult.text = "ML: analyzing..."

        try {
            val file = uriToCacheFile(uri)

            ApiClient.analyzeImageMl(file) { ok, result, err ->
                runOnUiThread {
                    if (ok && result != null) {
                        val free = result.free ?: 0
                        val occupied = result.occupied ?: 0

                        val total = free + occupied
                        val status = if (ok) "ok" else "error"

                        mlFree = free
                        mlOccupied = occupied
                        mlTotal = total


                        binding.tvMlResult.text =
                            "Free: $free\nOccupied: $occupied\nTotal: $total\nStatus: $status"
                    } else {
                        binding.tvMlResult.text = "ML error: $err"
                        Toast.makeText(this, "ML error: $err", Toast.LENGTH_LONG).show()
                    }
                }
            }

        } catch (e: Exception) {
            binding.tvMlResult.text = "ML error: ${e.message}"
            Toast.makeText(this, "ML error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDetach()
    }
}
