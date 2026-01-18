package com.example.parkingmate

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivitySimulationDetailBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Aktivnost za kreiranje i konfigurisanje nove simulacije
class SimulationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationDetailBinding
    private lateinit var map: MapView
    private var currentMarker: Marker? = null
    private var isSimulationRunning = false
    private var simulationHandler: Handler? = null
    private var simulationRunnable: Runnable? = null
    private var intervalInMillis: Long = 600000 // Podrazumevani interval (10 minuta)
    private var simulationCounter = 0 // Brojač izvršenih koraka

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimulationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Konfiguracija OpenStreetMap biblioteke
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = File(cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid/tiles")

        setupSpinner()
        setupTimeIntervalPicker()
        setupMap()
        setupListeners()
    }

    // Postavlja spinner za izbor tipa simulacije
    private fun setupSpinner() {
        val simulationTypes = listOf(
            SimulationType.FREE_SPACES.name,
            SimulationType.OCCUPIED_SPACES.name
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, simulationTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSimulationType.adapter = adapter
    }

    // Postavlja number pickere za sate, minute i sekunde
    @SuppressLint("SetTextI18n")
    private fun setupTimeIntervalPicker() {
        setupNumberPicker(binding.npHours, 0, 23, 0)
        setupNumberPicker(binding.npMinutes, 0, 59, 10)
        setupNumberPicker(binding.npSeconds, 0, 59, 0)
    }

    // Konfiguriše jedan NumberPicker
    private fun setupNumberPicker(picker: NumberPicker, min: Int, max: Int, defaultValue: Int) {
        picker.minValue = min
        picker.maxValue = max
        picker.value = defaultValue
        picker.setFormatter { value -> String.format("%02d", value) }
        picker.wrapSelectorWheel = true
        picker.setOnValueChangedListener { _, _, _ ->
            intervalInMillis =
                TimeUnit.HOURS.toMillis(binding.npHours.value.toLong()) +
                        TimeUnit.MINUTES.toMillis(binding.npMinutes.value.toLong()) +
                        TimeUnit.SECONDS.toMillis(binding.npSeconds.value.toLong())
        }
    }

    // Inicijalizuje mapu i postavlja početni prikaz
    private fun setupMap() {
        map = MapView(this)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(true)

        val startPoint = GeoPoint(44.7866, 20.4489) // Beograd
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)
        binding.mapContainer.addView(map)

        setupMapClickListener()
    }

    // Postavlja listener za klik na mapu
    private fun setupMapClickListener() {
        map.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(
                e: android.view.MotionEvent?,
                mapView: MapView?
            ): Boolean {
                if (e != null && mapView != null) {
                    try {
                        val geoPoint =
                            mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                        currentMarker?.let { map.overlays.remove(it) }

                        currentMarker = Marker(mapView).apply {
                            position = geoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Selected location"
                        }

                        currentMarker?.let { map.overlays.add(it) }

                        binding.etLocation.setText(
                            String.format(
                                Locale.US,
                                "%.6f, %.6f",
                                geoPoint.latitude,
                                geoPoint.longitude
                            )
                        )

                        map.controller.animateTo(geoPoint)
                        map.invalidate()

                    } catch (ex: Exception) {
                        Toast.makeText(
                            this@SimulationDetailActivity,
                            "Error: ${ex.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                return true
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun setupListeners() {
        // Pretraga adrese ili koordinata
        binding.btnSearch.setOnClickListener {
            val addressStr = binding.etLocation.text.toString().trim()
            if (addressStr.isEmpty()) {
                Toast.makeText(this, "Enter address or coordinates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (addressStr.contains(",")) {
                try {
                    val parts = addressStr.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].trim().toDouble()
                        val lon = parts[1].trim().toDouble()

                        if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                            val geoPoint = GeoPoint(lat, lon)
                            addMarkerAtLocation(geoPoint, "Entered location")
                        }
                    }
                } catch (e: NumberFormatException) {
                    searchAddress(addressStr)
                }
            } else {
                searchAddress(addressStr)
            }
        }

        // Aktivacija/deaktivacija simulacije
        binding.switchActivate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // mora total, value, location
                val totalStr = binding.etTotal.text.toString().trim()
                val valueStr = binding.etValue.text.toString().trim()
                val locationStr = binding.etLocation.text.toString().trim()
                val type = SimulationType.valueOf(binding.spinnerSimulationType.selectedItem.toString())

                val total = totalStr.toIntOrNull()
                if (total == null || total <= 0) {
                    Toast.makeText(this, "Enter valid total spots", Toast.LENGTH_SHORT).show()
                    binding.switchActivate.isChecked = false
                    return@setOnCheckedChangeListener
                }

                val valueNum = valueStr.toIntOrNull()
                if (valueNum == null || valueNum < 0) {
                    Toast.makeText(this, "Enter valid value", Toast.LENGTH_SHORT).show()
                    binding.switchActivate.isChecked = false
                    return@setOnCheckedChangeListener
                }

                if (locationStr.isEmpty()) {
                    Toast.makeText(this, "Fill in location before activation", Toast.LENGTH_SHORT)
                        .show()
                    binding.switchActivate.isChecked = false
                    return@setOnCheckedChangeListener
                }

                // value <= total
                when (type) {
                    SimulationType.FREE_SPACES -> {
                        if (valueNum > total) {
                            Toast.makeText(this, "Free must be <= Total", Toast.LENGTH_SHORT).show()
                            binding.switchActivate.isChecked = false
                            return@setOnCheckedChangeListener
                        }
                    }

                    SimulationType.OCCUPIED_SPACES -> {
                        if (valueNum > total) {
                            Toast.makeText(this, "Occupied must be <= Total", Toast.LENGTH_SHORT)
                                .show()
                            binding.switchActivate.isChecked = false
                            return@setOnCheckedChangeListener
                        }
                    }

                    else -> {
                        // ne bi trebalo da se desi jer spinner nudi samo FREE/OCCUPIED
                    }
                }

                if (intervalInMillis <= 0) {
                    Toast.makeText(this, "Set a valid time interval", Toast.LENGTH_SHORT).show()
                    binding.switchActivate.isChecked = false
                    return@setOnCheckedChangeListener
                }

                startSimulation()
            } else {
                stopSimulation()
            }
        }

        // Nazad
        binding.btnBack.setOnClickListener { finish() }

        // Save
        binding.btnSave.setOnClickListener { saveSimulation() }
    }

    @SuppressLint("SetTextI18n")
    private fun startSimulation() {
        isSimulationRunning = true
        simulationCounter = 0

        // Onemogućava kontrole tokom simulacije
        binding.npHours.isEnabled = false
        binding.npMinutes.isEnabled = false
        binding.npSeconds.isEnabled = false
        binding.etTotal.isEnabled = false
        binding.etValue.isEnabled = false
        binding.etLocation.isEnabled = false
        binding.btnSearch.isEnabled = false
        binding.spinnerSimulationType.isEnabled = false

        simulationHandler = Handler(Looper.getMainLooper())
        simulationRunnable = object : Runnable {
            override fun run() {
                if (isSimulationRunning) {
                    simulationCounter++
                    executeSimulationStep()
                    simulationHandler?.postDelayed(this, intervalInMillis)
                }
            }
        }

        simulationHandler?.post(simulationRunnable!!)
        Toast.makeText(this, "Simulation activated!", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun stopSimulation() {
        isSimulationRunning = false

        simulationRunnable?.let { simulationHandler?.removeCallbacks(it) }
        simulationRunnable = null

        // Ponovo omogućava kontrole
        binding.npHours.isEnabled = true
        binding.npMinutes.isEnabled = true
        binding.npSeconds.isEnabled = true
        binding.etTotal.isEnabled = true
        binding.etValue.isEnabled = true
        binding.etLocation.isEnabled = true
        binding.btnSearch.isEnabled = true
        binding.spinnerSimulationType.isEnabled = true

        Toast.makeText(this, "Simulation deactivated", Toast.LENGTH_SHORT).show()
    }

    private fun executeSimulationStep() {
        val type = SimulationType.valueOf(binding.spinnerSimulationType.selectedItem.toString())
        val totalStr = binding.etTotal.text.toString().trim()
        val valueStr = binding.etValue.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val total = totalStr.toIntOrNull() ?: 0
        val valueNum = valueStr.toIntOrNull() ?: 0

        println("=== SIMULATION $simulationCounter ===")
        println("Time: $timestamp")
        println("Type: ${type.name}")
        println("Total: $total")
        println("Value: $valueNum")
        println("Location: $location")
        println("Interval: ${intervalInMillis / 1000} seconds")

        // Ako je total nevalidan, preskoči slanje da se ne šalje smeće
        if (total > 0 && location.isNotEmpty()) {
            sendSimulatedDataToBackend(total, type, valueNum, location)
        }

        if (simulationCounter % 5 == 0) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Simulation #$simulationCounter\n${type.name}: $valueNum / total $total",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        animateMarker()
    }

    // Šalje simulirane podatke na server
    private fun sendSimulatedDataToBackend(
        total: Int,
        type: SimulationType,
        valueNum: Int,
        location: String
    ) {
        val coords = location.split(",")
        if (coords.size != 2) return

        val lat = coords[0].trim().toDoubleOrNull() ?: 0.0
        val lon = coords[1].trim().toDoubleOrNull() ?: 0.0

        // clamp value <= total
        val clamped = valueNum.coerceIn(0, total)

        val freeSpaces: Int
        val occupiedSpaces: Int

        when (type) {
            SimulationType.FREE_SPACES -> {
                freeSpaces = clamped
                occupiedSpaces = (total - freeSpaces).coerceAtLeast(0)
            }

            SimulationType.OCCUPIED_SPACES -> {
                occupiedSpaces = clamped
                freeSpaces = (total - occupiedSpaces).coerceAtLeast(0)
            }

            else -> return // ne bi trebalo
        }

        val urvrvResultJson = """
        {
            "totalSpots": $total,
            "freeSpaces": $freeSpaces,
            "occupiedSpaces": $occupiedSpaces,
            "spotsCoordinates": []
        }
        """.trimIndent()

        val json = """
        {
            "parkingLocationId": "64a9b8c2f0a5c1234567890b",
            "coordinates": "$lon,$lat",
            "timestamp": ${System.currentTimeMillis()},
            "imageUrl": "simulated.jpg",
            "urvrvResult": $urvrvResultJson
        }
        """.trimIndent()

        ApiClient.uploadSimulatedData(json)
    }

    private fun animateMarker() {
        runOnUiThread {
            currentMarker?.let { marker ->
                try {
                    marker.title = "Sim #$simulationCounter - ${Date()}"
                    map.invalidate()
                } catch (_: Exception) {
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun saveSimulation() {
        val totalStr = binding.etTotal.text.toString().trim()
        val valueStr = binding.etValue.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val type = SimulationType.valueOf(binding.spinnerSimulationType.selectedItem.toString())

        val total = totalStr.toIntOrNull()
        if (total == null || total <= 0) {
            Toast.makeText(this, "Enter valid total spots", Toast.LENGTH_SHORT).show()
            return
        }

        val valueNum = valueStr.toIntOrNull()
        if (valueNum == null || valueNum < 0) {
            Toast.makeText(this, "Enter valid value", Toast.LENGTH_SHORT).show()
            return
        }

        if (location.isEmpty()) {
            Toast.makeText(this, "Fill in the location", Toast.LENGTH_SHORT).show()
            return
        }

        when (type) {
            SimulationType.FREE_SPACES -> {
                if (valueNum > total) {
                    Toast.makeText(this, "Free must be <= Total", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            SimulationType.OCCUPIED_SPACES -> {
                if (valueNum > total) {
                    Toast.makeText(this, "Occupied must be <= Total", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            else -> {
                // ne bi trebalo da se desi
            }
        }

        // Zaustavi lokalnu simulaciju pre čuvanja
        if (isSimulationRunning) {
            isSimulationRunning = false
            simulationRunnable?.let { simulationHandler?.removeCallbacks(it) }
            simulationRunnable = null
        }

        val simulationName = "${type.name.replace("_", " ")} - $location"
        val interval = String.format(
            "%02d:%02d:%02d",
            binding.npHours.value,
            binding.npMinutes.value,
            binding.npSeconds.value
        )

        // NOTE: Simulation data class mora imati total:Int polje (dodaj ga u model)
        val newSimulation = Simulation(
            name = simulationName,
            type = type,
            total = total,
            value = valueStr,
            interval = interval,
            location = location,
            isActive = binding.switchActivate.isChecked
        )

        val resultIntent = Intent().apply {
            putExtra("new_simulation", newSimulation)
        }
        setResult(RESULT_OK, resultIntent)

        Toast.makeText(
            this,
            "Simulation saved!\nTotal: $total\n${type.name}: $valueStr\nLocation: $location",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun searchAddress(addressStr: String) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(addressStr, 1)

                runOnUiThread {
                    if (!addresses.isNullOrEmpty()) {
                        val location = addresses[0]
                        val geoPoint = GeoPoint(location.latitude, location.longitude)

                        addMarkerAtLocation(geoPoint, addressStr)

                        binding.etLocation.setText(
                            String.format(
                                Locale.US,
                                "%.6f, %.6f",
                                location.latitude,
                                location.longitude
                            )
                        )

                        Toast.makeText(this, "Location found", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun addMarkerAtLocation(geoPoint: GeoPoint, title: String) {
        currentMarker?.let { map.overlays.remove(it) }

        currentMarker = Marker(map).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
        }

        currentMarker?.let { map.overlays.add(it) }

        map.controller.animateTo(geoPoint)
        map.controller.setZoom(15.0)
        map.invalidate()
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
        simulationHandler?.removeCallbacksAndMessages(null)
        map.onDetach()
    }
}
