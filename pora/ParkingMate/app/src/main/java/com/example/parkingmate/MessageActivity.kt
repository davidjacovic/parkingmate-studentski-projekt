package com.example.parkingmate

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parkingmate.databinding.ActivityMessageBinding

class MessagesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBinding
    private lateinit var adapter: EventAdapter
    private val events = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dugme za nazad
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Postavlja RecyclerView
        adapter = EventAdapter(events)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Postavlja SwipeRefreshLayout listener
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadEvents()
        }

        // Učitava dogodke pri pokretanju
        loadEvents()
    }

    // Učitava dogodke sa servera
    private fun loadEvents() {
        showProgress(true)
        binding.swipeRefreshLayout.isRefreshing = true

        ApiClient.getAllEvents(
            eventType = null,
            status = null,
            startDate = null,
            endDate = null,
            limit = 50,
            skip = 0
        ) { success, eventsList, errorMessage ->
            // OkHttp callback se izvršava na background thread-u, mora se prebaciti na main thread
            runOnUiThread {
                binding.swipeRefreshLayout.isRefreshing = false
                showProgress(false)

                if (success && eventsList != null) {
                    android.util.Log.d("MESSAGES_ACTIVITY", "Received ${eventsList.size} events")
                    // Prosleđujemo direktno eventsList adapteru, ne menjamo events listu
                    adapter.updateEvents(eventsList)
                    // Ažuriramo events listu za empty state proveru
                    events.clear()
                    events.addAll(eventsList)
                    android.util.Log.d("MESSAGES_ACTIVITY", "After updateEvents - Adapter item count: ${adapter.itemCount}, events size: ${events.size}")
                    updateEmptyState()
                    android.util.Log.d("MESSAGES_ACTIVITY", "Empty state - events.isEmpty(): ${events.isEmpty()}")
                } else {
                    Toast.makeText(
                        this,
                        "Failed to load events: $errorMessage",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateEmptyState()
                }
            }
        }
    }

    // Ažurira empty state prikaz
    private fun updateEmptyState() {
        if (events.isEmpty()) {
            binding.tvEmptyList.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmptyList.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    // Prikazuje/sakriva progress bar
    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}

