package com.example.parkingmate

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parkingmate.databinding.ActivityMessageBinding
import java.util.*

class MessagesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBinding
    private lateinit var adapter: EventAdapter
    private val events = mutableListOf<Event>()
    
    // Filter state
    private var selectedStatus: String? = null
    private var selectedEventType: String? = null
    private var selectedDateRange: Pair<Long?, Long?>? = null
    private var sortOrder: SortOrder = SortOrder.NEWEST_FIRST
    
    enum class SortOrder {
        NEWEST_FIRST,
        OLDEST_FIRST
    }

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

        // Postavlja filter spinner-e
        setupStatusFilter()
        setupEventTypeFilter()
        setupDateFilter()
        setupSortFilter()

        // Učitava dogodke pri pokretanju
        loadEvents()
    }
    
    // Postavlja Status filter spinner
    private fun setupStatusFilter() {
        val statusOptions = arrayOf("All", "PENDING", "PROCESSED", "BLOCKCHAIN_RECORDED")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter
        
        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStatus = if (position == 0) null else statusOptions[position]
                loadEvents()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    // Postavlja Event Type filter spinner
    private fun setupEventTypeFilter() {
        val eventTypeOptions = arrayOf("All", "PARKING_FULL", "LOW_AVAILABILITY", "PARKING_AVAILABLE")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eventTypeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEventType.adapter = adapter
        
        binding.spinnerEventType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedEventType = if (position == 0) null else eventTypeOptions[position]
                loadEvents()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    // Postavlja Date filter spinner
    private fun setupDateFilter() {
        val dateOptions = arrayOf("All", "Today", "Week", "Month")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dateOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDate.adapter = adapter
        
        binding.spinnerDate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDateRange = when (position) {
                    0 -> null // All
                    1 -> getTodayRange() // Today
                    2 -> getWeekRange() // Week
                    3 -> getMonthRange() // Month
                    else -> null
                }
                loadEvents()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    // Vraća današnji datum range (početak dana do sada)
    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = System.currentTimeMillis()
        return Pair(startOfDay, endOfDay)
    }
    
    // Vraća nedeljni datum range (7 dana unazad)
    private fun getWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startOfWeek = calendar.timeInMillis
        val endOfWeek = System.currentTimeMillis()
        return Pair(startOfWeek, endOfWeek)
    }
    
    // Vraća mesečni datum range (30 dana unazad)
    private fun getMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val startOfMonth = calendar.timeInMillis
        val endOfMonth = System.currentTimeMillis()
        return Pair(startOfMonth, endOfMonth)
    }
    
    // Postavlja Sort filter spinner
    private fun setupSortFilter() {
        val sortOptions = arrayOf("Newest First", "Oldest First")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = adapter
        
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortOrder = when (position) {
                    0 -> SortOrder.NEWEST_FIRST
                    1 -> SortOrder.OLDEST_FIRST
                    else -> SortOrder.NEWEST_FIRST
                }
                applySorting()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    // Primena sortiranja na listu dogodkov
    private fun applySorting() {
        val sortedEvents = when (sortOrder) {
            SortOrder.NEWEST_FIRST -> events.sortedByDescending { it.timestamp }
            SortOrder.OLDEST_FIRST -> events.sortedBy { it.timestamp }
        }
        adapter.updateEvents(sortedEvents)
    }

    // Učitava dogodke sa servera
    private fun loadEvents() {
        showProgress(true)
        binding.swipeRefreshLayout.isRefreshing = true

        val (startDate, endDate) = selectedDateRange ?: Pair(null, null)

        ApiClient.getAllEvents(
            eventType = selectedEventType,
            status = selectedStatus,
            startDate = startDate,
            endDate = endDate,
            limit = 50,
            skip = 0
        ) { success, eventsList, errorMessage ->
            // OkHttp callback se izvršava na background thread-u, mora se prebaciti na main thread
            runOnUiThread {
                binding.swipeRefreshLayout.isRefreshing = false
                showProgress(false)

                if (success && eventsList != null) {
                    android.util.Log.d("MESSAGES_ACTIVITY", "Received ${eventsList.size} events")
                    // Ažuriramo events listu
                    events.clear()
                    events.addAll(eventsList)
                    // Primenjujemo sortiranje
                    applySorting()
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

