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
        val simulationTypes = SimulationType.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, simulationTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSimulationType.adapter = adapter
    }

    // Postavlja number pickere za sate, minute i sekunde
    @SuppressLint("SetTextI18n")
    private fun setupTimeIntervalPicker() {
        setupNumberPicker(binding.npHours, 0, 23, 0, "hour")
        setupNumberPicker(binding.npMinutes, 0, 59, 10, "min")
        setupNumberPicker(binding.npSeconds, 0, 59, 0, "sec")
    }

    // Konfiguriše jedan NumberPicker
    private fun setupNumberPicker(picker: NumberPicker, min: Int, max: Int, defaultValue: Int, label: String) {
        picker.minValue = min
        picker.maxValue = max
        picker.value = defaultValue
        picker.setFormatter { value -> String.format("%02d", value) } // Formatira kao dvocifren broj
        picker.wrapSelectorWheel = true
        picker.setOnValueChangedListener { _, _, _ ->
            // Računa interval u milisekundama
            intervalInMillis = TimeUnit.HOURS.toMillis(binding.npHours.value.toLong()) +
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

        val startPoint = GeoPoint(44.7866, 20.4489) // Beograd kao početna lokacija
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)
        binding.mapContainer.addView(map)

        setupMapClickListener()
    }

    // Postavlja listener za klik na mapu
    private fun setupMapClickListener() {
        map.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                if (e != null && mapView != null) {
                    try {
                        // Konvertuje koordinate ekrana u geografske koordinate
                        val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                        // Uklanja prethodni marker
                        currentMarker?.let { map.overlays.remove(it) }

                        // Kreira novi marker
                        currentMarker = Marker(mapView).apply {
                            position = geoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Selected location"
                        }

                        currentMarker?.let { map.overlays.add(it) }

                        // Postavlja koordinate u polje za unos
                        binding.etLocation.setText(String.format(Locale.US, "%.6f, %.6f",
                            geoPoint.latitude, geoPoint.longitude))

                        map.controller.animateTo(geoPoint)
                        map.invalidate()

                    } catch (ex: Exception) {
                        Toast.makeText(this@SimulationDetailActivity,
                            "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        })
    }

    // Postavlja listener-e za dugmad i kontrole
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
                if (binding.etValue.text.toString().trim().isEmpty() ||
                    binding.etLocation.text.toString().trim().isEmpty()) {

                    Toast.makeText(this, "Fill in the value and location before activation",
                        Toast.LENGTH_SHORT).show()
                    binding.switchActivate.isChecked = false
                    return@setOnCheckedChangeListener
                }

                if (intervalInMillis <= 0) {
                    Toast.makeText(this, "Set a valid time interval",
                        Toast.LENGTH_SHORT).show()
                    binding.switchActivate.isChecked = false
                    return@setOnCheckedChangeListener
                }

                startSimulation()
            } else {
                stopSimulation()
            }
        }

        // Čuvanje simulacije
        binding.btnSave.setOnClickListener {
            saveSimulation()
        }
    }

    // Pokreće simulaciju
    @SuppressLint("SetTextI18n")
    private fun startSimulation() {
        isSimulationRunning = true
        simulationCounter = 0

        // Onemogućava kontrole tokom simulacije
        binding.npHours.isEnabled = false
        binding.npMinutes.isEnabled = false
        binding.npSeconds.isEnabled = false
        binding.etValue.isEnabled = false
        binding.etLocation.isEnabled = false
        binding.btnSearch.isEnabled = false
        binding.spinnerSimulationType.isEnabled = false

        // Postavlja periodično izvršavanje
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

        Toast.makeText(this,
            "Simulation activated!",
            Toast.LENGTH_SHORT).show()
    }

    // Zaustavlja simulaciju
    @SuppressLint("SetTextI18n")
    private fun stopSimulation() {
        isSimulationRunning = false

        simulationRunnable?.let {
            simulationHandler?.removeCallbacks(it)
        }
        simulationRunnable = null

        // Ponovo omogućava kontrole
        binding.npHours.isEnabled = true
        binding.npMinutes.isEnabled = true
        binding.npSeconds.isEnabled = true
        binding.etValue.isEnabled = true
        binding.etLocation.isEnabled = true
        binding.btnSearch.isEnabled = true
        binding.spinnerSimulationType.isEnabled = true

        Toast.makeText(this,
            "Simulation deactivated",
            Toast.LENGTH_SHORT).show()
    }

    // Izvršava jedan korak simulacije
    private fun executeSimulationStep() {
        val type = SimulationType.values()[binding.spinnerSimulationType.selectedItemPosition]
        val value = binding.etValue.text.toString()
        val location = binding.etLocation.text.toString()
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // Ispisuje informacije u log
        println("=== SIMULATION $simulationCounter ===")
        println("Time: $timestamp")
        println("Type: ${type.name}")
        println("Value: $value")
        println("Location: $location")
        println("Interval: ${intervalInMillis/1000} secconds")

        sendSimulatedDataToBackend(value, location)

        // Prikazuje Toast svakih 5 koraka
        if (simulationCounter % 5 == 0) {
            runOnUiThread {
                Toast.makeText(this,
                    "Simulation #$simulationCounter\n${type.name}: $value",
                    Toast.LENGTH_SHORT).show()
            }
        }
        animateMarker()
    }

    // Šalje simulirane podatke na server
    private fun sendSimulatedDataToBackend(value: String, location: String) {
        val coords = location.split(",")
        if (coords.size != 2) return

        val lat = coords[0].trim().toDoubleOrNull() ?: 0.0
        val lon = coords[1].trim().toDoubleOrNull() ?: 0.0
        val numValue = value.toIntOrNull() ?: 0

        val type = SimulationType.values()[binding.spinnerSimulationType.selectedItemPosition]
        var totalSpots = 0
        var freeSpaces = 0
        var occupiedSpaces = 0

        // Postavlja vrednosti u zavisnosti od tipa simulacije
        when (type) {
            SimulationType.TOTAL_SPACES -> totalSpots = numValue
            SimulationType.FREE_SPACES -> freeSpaces = numValue
            SimulationType.OCCUPIED_SPACES -> occupiedSpaces = numValue
            SimulationType.ALL -> {
                totalSpots = numValue
                freeSpaces = numValue
                occupiedSpaces = numValue
            }
        }

        val urvrvResultJson = """
    {
        "totalSpots": $totalSpots,
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

    // Animira marker na mapi sa novim naslovom
    private fun animateMarker() {
        runOnUiThread {
            currentMarker?.let { marker ->
                try {
                    marker.title = "Sim #$simulationCounter - ${Date()}"
                    map.invalidate()
                } catch (e: Exception) {
                }
            }
        }
    }

    // Čuva simulaciju i vraća rezultat nazad
    @SuppressLint("SetTextI18n")
    private fun saveSimulation() {
        val type = SimulationType.values()[binding.spinnerSimulationType.selectedItemPosition]
        val value = binding.etValue.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        if (value.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Fill in the value and location", Toast.LENGTH_SHORT).show()
            return
        }

        // Zaustavi lokalnu simulaciju pre čuvanja (ako je pokrenuta u ovoj aktivnosti)
        // Ovo ne deaktivira simulaciju - samo zaustavlja lokalno izvršavanje
        // Simulacija će se pokrenuti u SimulationActivity ako je isActive = true
        if (isSimulationRunning) {
            isSimulationRunning = false
            simulationRunnable?.let {
                simulationHandler?.removeCallbacks(it)
            }
            simulationRunnable = null
        }
        
        // Generiše naziv simulacije
        val simulationName = "${type.name.replace("_", " ")} - $location"
        val interval = String.format("%02d:%02d:%02d",
            binding.npHours.value,
            binding.npMinutes.value,
            binding.npSeconds.value)

        // Kreira novu simulaciju - sačuvaj status switch-a
        val newSimulation = Simulation(
            name = simulationName,
            type = type,
            value = value,
            interval = interval,
            location = location,
            isActive = binding.switchActivate.isChecked
        )

        // Vraća simulaciju nazad u SimulationActivity
        val resultIntent = Intent().apply {
            putExtra("new_simulation", newSimulation)
        }
        setResult(RESULT_OK, resultIntent)

        Toast.makeText(this,
            "Simulation saved!\n${type.name}: $value\nLocation: $location",
            Toast.LENGTH_LONG).show()

        finish()
    }

    // Pretražuje adresu koristeći Geocoder
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

                        binding.etLocation.setText(String.format(Locale.US, "%.6f, %.6f",
                            geoPoint.latitude, geoPoint.longitude))

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

    // Dodaje marker na mapu na određenoj lokaciji
    private fun addMarkerAtLocation(geoPoint: GeoPoint, title: String) {
        currentMarker?.let {
            map.overlays.remove(it)
        }

        currentMarker = Marker(map).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
        }

        currentMarker?.let { marker ->
            map.overlays.add(marker)
        }

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
        // Ne zaustavljaj simulaciju kada se aktivnost pauzira - dozvoli da se sačuva
        // Simulacija će se preneti u SimulationActivity kada se sačuva
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        simulationHandler?.removeCallbacksAndMessages(null)
        map.onDetach()
    }
}