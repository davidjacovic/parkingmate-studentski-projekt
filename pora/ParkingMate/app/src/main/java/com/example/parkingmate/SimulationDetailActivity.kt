package com.example.parkingmate

import android.annotation.SuppressLint
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

class SimulationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationDetailBinding
    private lateinit var map: MapView
    private var currentMarker: Marker? = null
    private var isSimulationRunning = false
    private var simulationHandler: Handler? = null
    private var simulationRunnable: Runnable? = null
    private var intervalInMillis: Long = 600000
    private var simulationCounter = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimulationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = File(cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid/tiles")

        setupSpinner()
        setupTimeIntervalPicker()
        setupMap()
        setupListeners()
    }

    private fun setupSpinner() {
        val simulationTypes = SimulationType.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, simulationTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSimulationType.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    private fun setupTimeIntervalPicker() {
        setupNumberPicker(binding.npHours, 0, 23, 0, "sati")
        setupNumberPicker(binding.npMinutes, 0, 59, 10, "min")
        setupNumberPicker(binding.npSeconds, 0, 59, 0, "sec")
    }

    private fun setupNumberPicker(picker: NumberPicker, min: Int, max: Int, defaultValue: Int, label: String) {
        picker.minValue = min
        picker.maxValue = max
        picker.value = defaultValue
        picker.setFormatter { value -> String.format("%02d", value) }
        picker.wrapSelectorWheel = true
        picker.setOnValueChangedListener { _, _, _ ->
            intervalInMillis = TimeUnit.HOURS.toMillis(binding.npHours.value.toLong()) +
                    TimeUnit.MINUTES.toMillis(binding.npMinutes.value.toLong()) +
                    TimeUnit.SECONDS.toMillis(binding.npSeconds.value.toLong())
        }
    }
    private fun setupMap() {
        map = MapView(this)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(true)

        val startPoint = GeoPoint(44.7866, 20.4489)
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)
        binding.mapContainer.addView(map)

        setupMapClickListener()
    }

    private fun setupMapClickListener() {
        map.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                if (e != null && mapView != null) {
                    try {
                        val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                        currentMarker?.let { map.overlays.remove(it) }

                        currentMarker = Marker(mapView).apply {
                            position = geoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Izabrana lokacija"
                        }

                        currentMarker?.let { map.overlays.add(it) }

                        binding.etLocation.setText(String.format(Locale.US, "%.6f, %.6f",
                            geoPoint.latitude, geoPoint.longitude))

                        map.controller.animateTo(geoPoint)
                        map.invalidate()

                    } catch (ex: Exception) {
                        Toast.makeText(this@SimulationDetailActivity,
                            "Greška: ${ex.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            val addressStr = binding.etLocation.text.toString().trim()
            if (addressStr.isEmpty()) {
                Toast.makeText(this, "Unesite adresu ili koordinate", Toast.LENGTH_SHORT).show()
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
                            addMarkerAtLocation(geoPoint, "Unesena lokacija")
                        }
                    }
                } catch (e: NumberFormatException) {
                    searchAddress(addressStr)
                }
            } else {
                searchAddress(addressStr)
            }
        }

        binding.switchActivate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (binding.etValue.text.toString().trim().isEmpty() ||
                    binding.etLocation.text.toString().trim().isEmpty()) {

                    Toast.makeText(this, "Popunite vrednost i lokaciju pre aktivacije",
                        Toast.LENGTH_SHORT).show()
                    binding.switchActivate.isChecked = false
                    return@setOnCheckedChangeListener
                }

                if (intervalInMillis <= 0) {
                    Toast.makeText(this, "Postavite validan vremenski interval",
                        Toast.LENGTH_SHORT).show()
                    binding.switchActivate.isChecked = false
                    return@setOnCheckedChangeListener
                }

                startSimulation()
            } else {
                stopSimulation()
            }
        }

        binding.btnSave.setOnClickListener {
            saveSimulation()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startSimulation() {
        isSimulationRunning = true
        simulationCounter = 0

        binding.npHours.isEnabled = false
        binding.npMinutes.isEnabled = false
        binding.npSeconds.isEnabled = false
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

        Toast.makeText(this,
            "Simulacija aktivirana!",
            Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun stopSimulation() {
        isSimulationRunning = false

        simulationHandler?.removeCallbacks(simulationRunnable!!)
        simulationRunnable = null

        binding.npHours.isEnabled = true
        binding.npMinutes.isEnabled = true
        binding.npSeconds.isEnabled = true
        binding.etValue.isEnabled = true
        binding.etLocation.isEnabled = true
        binding.btnSearch.isEnabled = true
        binding.spinnerSimulationType.isEnabled = true

        Toast.makeText(this,
            "Simulacija deaktivirana",
            Toast.LENGTH_SHORT).show()
    }

    private fun executeSimulationStep() {
        val type = SimulationType.values()[binding.spinnerSimulationType.selectedItemPosition]
        val value = binding.etValue.text.toString()
        val location = binding.etLocation.text.toString()
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        println("=== SIMULACIJA $simulationCounter ===")
        println("Vreme: $timestamp")
        println("Tip: ${type.name}")
        println("Vrednost: $value")
        println("Lokacija: $location")
        println("Interval: ${intervalInMillis/1000} sekundi")

        if (simulationCounter % 5 == 0) {
            runOnUiThread {
                Toast.makeText(this,
                    "Simulacija #$simulationCounter\n${type.name}: $value",
                    Toast.LENGTH_SHORT).show()
            }
        }
        animateMarker()
    }

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

    private fun saveSimulation() {
        val type = SimulationType.values()[binding.spinnerSimulationType.selectedItemPosition]
        val value = binding.etValue.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        if (value.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Popunite vrednost i lokaciju", Toast.LENGTH_SHORT).show()
            return
        }

        val simulationData = """
            === SAČUVANA SIMULACIJA ===
            Tip: ${type.name}
            Vrednost: $value
            Lokacija: $location
            Interval: ${String.format("%02d:%02d:%02d",
            binding.npHours.value,
            binding.npMinutes.value,
            binding.npSeconds.value)}
            Status: ${if (binding.switchActivate.isChecked) "AKTIVNA" else "NEAKTIVNA"}
        """.trimIndent()

        println(simulationData)

        Toast.makeText(this,
            "Simulacija sačuvana!\n${type.name}: $value\nLokacija: $location",
            Toast.LENGTH_LONG).show()
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

                        binding.etLocation.setText(String.format(Locale.US, "%.6f, %.6f",
                            geoPoint.latitude, geoPoint.longitude))

                        Toast.makeText(this, "Lokacija pronađena", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Lokacija nije pronađena", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Greška pri pretrazi", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

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
        if (isSimulationRunning) {
            stopSimulation()
            binding.switchActivate.isChecked = false
        }
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        simulationHandler?.removeCallbacksAndMessages(null)
        map.onDetach()
    }
}