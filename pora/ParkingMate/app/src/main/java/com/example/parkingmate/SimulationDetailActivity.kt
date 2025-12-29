package com.example.parkingmate

import android.location.Geocoder
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmate.databinding.ActivitySimulationDetailBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Locale

class SimulationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationDetailBinding
    private lateinit var map: MapView
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimulationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = File(cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid/tiles")

        setupSpinner()
        setupMap()
        setupListeners()
    }

    private fun setupSpinner() {
        val simulationTypes = SimulationType.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, simulationTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSimulationType.adapter = adapter
    }

    private fun setupMap() {
        map = MapView(this)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(true)
        val startPoint = GeoPoint(44.7866, 20.4489)
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)
        binding.mapFragment.addView(map)

        setupMapClickListener()
    }

    private fun setupMapClickListener() {
        map.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                if (e != null && mapView != null) {
                    try {
                        val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                        currentMarker?.let {
                            map.overlays.remove(it)
                        }

                        currentMarker = Marker(mapView).apply {
                            position = geoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Izabrana lokacija"
                        }

                        currentMarker?.let { marker ->
                            map.overlays.add(marker)
                        }

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
                        } else {
                            Toast.makeText(this, "Nevalidne koordinate", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: NumberFormatException) {
                    searchAddress(addressStr)
                }
            } else {
                searchAddress(addressStr)
            }
        }

        binding.btnConfirm.setOnClickListener {
            val type = SimulationType.values()[binding.spinnerSimulationType.selectedItemPosition]
            val value = binding.etValue.text.toString()
            val location = binding.etLocation.text.toString()

            if (value.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Popunite sva polja", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this,
                "Simulacija kreirana!\nTip: ${type.name}\nVrednost: $value\nLokacija: $location",
                Toast.LENGTH_LONG).show()
        }
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
        map.onPause()
    }
}