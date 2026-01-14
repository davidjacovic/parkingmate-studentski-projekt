package com.example.parkingmate

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.parkingmate.databinding.ActivitySendEventBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Locale

class SendEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendEventBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: MapView
    private var currentMarker: Marker? = null
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    private val REQUEST_CODE_LOCATION = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Konfiguracija OpenStreetMap biblioteke
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = File(cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid/tiles")

        // Postavi adapter za EventType spinner
        val eventTypes = EventType.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eventTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEventType.adapter = adapter

        // Postavi mapu
        setupMap()

        // Listener za pretragu adrese
        binding.btnSearch.setOnClickListener {
            searchAddress()
        }

        // Dugme za nazad
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Dugme za slanje dogodka
        binding.btnSendEvent.setOnClickListener {
            sendEvent()
        }

        // Automatski dohvati lokaciju pri otvaranju
        getCurrentLocation()
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

                        // Ažurira lokaciju
                        updateLocation(geoPoint.latitude, geoPoint.longitude)

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
                        Toast.makeText(this@SendEventActivity,
                            "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        })
    }

    // Ažurira lokaciju
    private fun updateLocation(lat: Double, lon: Double) {
        currentLat = lat
        currentLon = lon
    }

    // Pretražuje adresu koristeći Geocoder
    private fun searchAddress() {
        val addressStr = binding.etLocation.text.toString().trim()
        if (addressStr.isEmpty()) {
            Toast.makeText(this, "Unesite adresu ili koordinate", Toast.LENGTH_SHORT).show()
            return
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
                        updateLocation(lat, lon)
                    } else {
                        Toast.makeText(this, "Nevažeće koordinate", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: NumberFormatException) {
                searchAddressWithGeocoder(addressStr)
            }
        } else {
            searchAddressWithGeocoder(addressStr)
        }
    }

    // Pretražuje adresu koristeći Geocoder
    private fun searchAddressWithGeocoder(addressStr: String) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(addressStr, 1)

                runOnUiThread {
                    if (!addresses.isNullOrEmpty()) {
                        val location = addresses[0]
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        addMarkerAtLocation(geoPoint, addressStr)
                        updateLocation(location.latitude, location.longitude)
                        binding.etLocation.setText(String.format(Locale.US, "%.6f, %.6f",
                            location.latitude, location.longitude))
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

    @Suppress("DEPRECATION")
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
            return
        }

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                addMarkerAtLocation(geoPoint, "Current location")
                updateLocation(location.latitude, location.longitude)
                binding.etLocation.setText(String.format(Locale.US, "%.6f, %.6f",
                    location.latitude, location.longitude))
            } else {
                Toast.makeText(this, "Lokacija nije dostupna", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Greška pri dohvatanju lokacije: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEvent() {
        // Validacija
        val selectedType = binding.spinnerEventType.selectedItem as? String
        if (selectedType == null) {
            Toast.makeText(this, "Izaberite tip dogodka", Toast.LENGTH_SHORT).show()
            return
        }

        val message = binding.etMessage.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "Unesite poruku", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentLat == null || currentLon == null) {
            Toast.makeText(this, "Dohvatite lokaciju pre slanja", Toast.LENGTH_SHORT).show()
            return
        }

        // Kreiraj EventType enum
        val eventType = try {
            EventType.valueOf(selectedType)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Nevažeći tip dogodka", Toast.LENGTH_SHORT).show()
            return
        }

        // Kreiraj topic na osnovu tipa
        val topic = when (eventType) {
            EventType.PARKING_FULL -> "parking/full"
            EventType.PARKING_AVAILABLE -> "parking/available"
            EventType.LOW_AVAILABILITY -> "parking/low-availability"
        }

        // Kreiraj Event objekat
        val event = Event(
            topic = topic,
            message = message,
            timestamp = System.currentTimeMillis(),
            location = "$currentLat,$currentLon"
        )

        // Pošalji dogodak
        binding.btnSendEvent.isEnabled = false
        binding.btnSendEvent.text = "Slanje..."

        ApiClient.sendEvent(event, eventType) { success, errorMessage ->
            runOnUiThread {
                binding.btnSendEvent.isEnabled = true
                binding.btnSendEvent.text = "Pošalji dogodak"

                if (success) {
                    Toast.makeText(this, "Dogodak uspešno poslat!", Toast.LENGTH_SHORT).show()
                    // Vrati se na main screen
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Greška pri slanju: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Dozvola za lokaciju je potrebna", Toast.LENGTH_SHORT).show()
            }
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

