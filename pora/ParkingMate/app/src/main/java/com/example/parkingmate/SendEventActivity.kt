package com.example.parkingmate

import android.Manifest
import android.content.pm.PackageManager
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

class SendEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendEventBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    private val REQUEST_CODE_LOCATION = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Postavi adapter za EventType spinner
        val eventTypes = EventType.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eventTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEventType.adapter = adapter

        // Dugme za dobijanje trenutne lokacije
        binding.btnGetLocation.setOnClickListener {
            getCurrentLocation()
        }

        // Dugme za slanje dogodka
        binding.btnSendEvent.setOnClickListener {
            sendEvent()
        }

        // Automatski dohvati lokaciju pri otvaranju
        getCurrentLocation()
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

        binding.btnGetLocation.isEnabled = false
        binding.btnGetLocation.text = "Dohvatanje lokacije..."

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
                binding.tvLocation.text = "Lat: ${String.format("%.6f", currentLat)}\nLon: ${String.format("%.6f", currentLon)}"
                binding.btnGetLocation.isEnabled = true
                binding.btnGetLocation.text = "Osveži lokaciju"
            } else {
                Toast.makeText(this, "Lokacija nije dostupna", Toast.LENGTH_SHORT).show()
                binding.btnGetLocation.isEnabled = true
                binding.btnGetLocation.text = "Dohvati lokaciju"
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Greška pri dohvatanju lokacije: ${it.message}", Toast.LENGTH_SHORT).show()
            binding.btnGetLocation.isEnabled = true
            binding.btnGetLocation.text = "Dohvati lokaciju"
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
                    // Očisti polja
                    binding.etMessage.text.clear()
                    binding.spinnerEventType.setSelection(0)
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
}

