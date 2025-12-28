package com.example.parkingmate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.parkingmate.databinding.ItemSimulationBinding

class SimulationAdapter(private val items: List<String>) :
    RecyclerView.Adapter<SimulationAdapter.SimulationViewHolder>() {

    inner class SimulationViewHolder(val binding: ItemSimulationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimulationViewHolder {
        val binding = ItemSimulationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SimulationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SimulationViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvSimulationName.text = item
    }

    override fun getItemCount(): Int = items.size
}
