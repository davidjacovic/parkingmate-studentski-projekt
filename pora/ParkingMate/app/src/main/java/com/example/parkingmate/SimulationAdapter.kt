package com.example.parkingmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

// Adapter za prikaz liste simulacija u RecyclerView
class SimulationAdapter(
    private var simulations: MutableList<Simulation>,
    private val onSwitchChanged: (Simulation, Boolean) -> Unit,
    private val onItemClicked: (Simulation) -> Unit,
    private val onDeleteClicked: (Simulation) -> Unit
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
        val delete: ImageView = itemView.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimulationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simulation, parent, false)
        return SimulationViewHolder(view)
    }

    override fun onBindViewHolder(holder: SimulationViewHolder, position: Int) {
        val simulation = simulations[position]

        // Ikone sad uglavnom FREE/OCCUPIED (ako ti ostanu stare simulacije, i dalje ih pokrivamo)
        val iconRes = when (simulation.type) {
            SimulationType.FREE_SPACES -> R.drawable.ic_parking_free
            SimulationType.OCCUPIED_SPACES -> R.drawable.ic_parking_occupied
            else -> R.drawable.ic_simulation_default
        }

        holder.icon.setImageResource(iconRes)
        holder.name.text = simulation.name
        holder.type.text = "Type: ${simulation.type.name}"

        // Prikaži total + value (value znači free ili occupied, zavisi od type)
        holder.value.text = "Total: ${simulation.total} | Value: ${simulation.value}"

        holder.interval.text = "Interval: ${simulation.interval}"
        holder.location.text = "Location: ${simulation.location}"

        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = simulation.isActive

        val context = holder.itemView.context
        if (simulation.isActive) {
            holder.card.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.primary_blue)
            )
        } else {
            holder.card.setCardBackgroundColor(
                ContextCompat.getColor(context, android.R.color.white)
            )
        }

        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChanged(simulation, isChecked)
        }

        holder.card.setOnClickListener { onItemClicked(simulation) }
        holder.delete.setOnClickListener { onDeleteClicked(simulation) }
    }

    override fun getItemCount(): Int = simulations.size

    fun addSimulation(simulation: Simulation) {
        simulations.add(0, simulation)
        notifyItemInserted(0)
    }

    fun removeSimulation(simulation: Simulation) {
        val index = simulations.indexOfFirst { it.id == simulation.id }
        if (index != -1) {
            simulations.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
