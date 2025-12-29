package com.example.parkingmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class SimulationAdapter(
    private var simulations: MutableList<Simulation>,
    private val onSwitchChanged: (Simulation, Boolean) -> Unit,
    private val onItemClicked: (Simulation) -> Unit
) : RecyclerView.Adapter<SimulationAdapter.SimulationViewHolder>() {

    class SimulationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.ivIcon)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val type: TextView = itemView.findViewById(R.id.tvType)
        val value: TextView = itemView.findViewById(R.id.tvValue)
        val interval: TextView = itemView.findViewById(R.id.tvInterval)
        val location: TextView = itemView.findViewById(R.id.tvLocation)
        val switch: Switch = itemView.findViewById(R.id.switchActive)
        val card: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardSimulation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimulationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simulation, parent, false)
        return SimulationViewHolder(view)
    }

    override fun onBindViewHolder(holder: SimulationViewHolder, position: Int) {
        val simulation = simulations[position]

        val iconRes = when (simulation.type) {
            SimulationType.TOTAL_SPACES -> R.drawable.ic_parking_total
            SimulationType.FREE_SPACES -> R.drawable.ic_parking_free
            SimulationType.OCCUPIED_SPACES -> R.drawable.ic_parking_occupied
            SimulationType.ALL -> R.drawable.ic_parking_all
            else -> R.drawable.ic_simulation_default
        }
        holder.icon.setImageResource(iconRes)
        holder.name.text = simulation.name
        holder.type.text = "Tip: ${simulation.type.name}"
        holder.value.text = "Vrednost: ${simulation.value}"
        holder.interval.text = "Interval: ${simulation.interval}"
        holder.location.text = "Lokacija: ${simulation.location}"

        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = simulation.isActive

        val context = holder.itemView.context
        if (simulation.isActive) {
            holder.card.setCardBackgroundColor(
                ContextCompat.getColor(context, android.R.color.holo_green_light)
            )
        } else {
            holder.card.setCardBackgroundColor(
                ContextCompat.getColor(context, android.R.color.white)
            )
        }

        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChanged(simulation, isChecked)
        }

        holder.card.setOnClickListener {
            onItemClicked(simulation)
        }
    }

    override fun getItemCount(): Int = simulations.size
    fun addSimulation(simulation: Simulation) {
        simulations.add(0, simulation)
        notifyItemInserted(0)
    }
}