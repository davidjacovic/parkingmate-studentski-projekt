package com.example.parkingmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

// Adapter za prikaz liste dogodkov u RecyclerView
class EventAdapter(
    private var events: MutableList<Event>
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    // ViewHolder koji drži reference na UI elemente jednog item-a
    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTopic: TextView = itemView.findViewById(R.id.tvTopic)
        val tvEventType: TextView = itemView.findViewById(R.id.tvEventType)
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvBlockchainBadge: TextView = itemView.findViewById(R.id.tvBlockchainBadge)
    }

    // Kreira novi ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    // Povezuje podatke dogodka sa UI elementima
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        if (position >= events.size) {
            android.util.Log.e("EVENT_ADAPTER", "Position $position out of bounds, events size: ${events.size}")
            return
        }
        
        val event = events[position]
        android.util.Log.d("EVENT_ADAPTER", "Binding event at position $position: ${event.topic}")

        // Postavlja topic
        holder.tvTopic.text = event.topic

        // Postavlja event type sa bojom na osnovu tipa dogodka
        holder.tvEventType.text = event.eventType.name
        when (event.eventType) {
            EventType.PARKING_FULL -> {
                // Crvena boja za ekstremne dogodke (parking pun)
                holder.tvEventType.setBackgroundResource(R.drawable.badge_event_full)
            }
            EventType.LOW_AVAILABILITY -> {
                // Narandžasta boja za posebne dogodke (niska dostupnost)
                holder.tvEventType.setBackgroundResource(R.drawable.badge_event_low)
            }
            EventType.PARKING_AVAILABLE -> {
                // Zelena boja za normalne dogodke (parking dostupan)
                holder.tvEventType.setBackgroundResource(R.drawable.badge_event_available)
            }
        }

        // Postavlja message
        holder.tvMessage.text = event.message

        // Postavlja location
        holder.tvLocation.text = "📍 ${event.location}"

        // Formatira i postavlja timestamp
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date = Date(event.timestamp)
        holder.tvTimestamp.text = dateFormat.format(date)

        // Postavlja status badge sa bojom
        holder.tvStatusBadge.text = event.status
        when (event.status) {
            "PENDING" -> {
                holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_status_pending) // Žuto
                holder.tvStatusBadge.visibility = View.VISIBLE
            }
            "PROCESSED" -> {
                holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_status_processed) // Zeleno
                holder.tvStatusBadge.visibility = View.VISIBLE
            }
            "BLOCKCHAIN_RECORDED" -> {
                holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_status_blockchain) // Plavo
                holder.tvStatusBadge.visibility = View.VISIBLE
            }
            else -> {
                holder.tvStatusBadge.visibility = View.GONE
            }
        }

        // Postavlja blockchain badge ako postoji blockchainHash
        if (event.blockchainHash != null && event.blockchainHash.isNotEmpty()) {
            holder.tvBlockchainBadge.visibility = View.VISIBLE
            holder.tvBlockchainBadge.setBackgroundResource(R.drawable.badge_blockchain) // Plavo
        } else {
            holder.tvBlockchainBadge.visibility = View.GONE
        }
    }

    // Vraća broj dogodkov u listi
    override fun getItemCount(): Int = events.size

    // Ažurira listu dogodkov
    fun updateEvents(newEvents: List<Event>) {
        android.util.Log.d("EVENT_ADAPTER", "updateEvents called with ${newEvents.size} events")
        events.clear()
        events.addAll(newEvents)
        android.util.Log.d("EVENT_ADAPTER", "Events list size: ${events.size}, calling notifyDataSetChanged()")
        notifyDataSetChanged()
        android.util.Log.d("EVENT_ADAPTER", "After notifyDataSetChanged - itemCount: $itemCount")
    }

    // Dodaje dogodke u listu
    fun addEvents(newEvents: List<Event>) {
        val startPosition = events.size
        events.addAll(newEvents)
        notifyItemRangeInserted(startPosition, newEvents.size)
    }

    // Briše sve dogodke
    fun clearEvents() {
        val size = events.size
        events.clear()
        notifyItemRangeRemoved(0, size)
    }
}

