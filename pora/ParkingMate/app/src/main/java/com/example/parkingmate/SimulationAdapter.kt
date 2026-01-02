package com.example.parkingmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

// Adapter za prikaz liste simulacija u RecyclerView
class SimulationAdapter(
    private var simulations: MutableList<Simulation>,
    private val onSwitchChanged: (Simulation, Boolean) -> Unit, // Callback za promenu switch-a
    private val onItemClicked: (Simulation) -> Unit, // Callback za klik na item
    private val onDeleteClicked: (Simulation) -> Unit // Callback za brisanje
) : RecyclerView.Adapter<SimulationAdapter.SimulationViewHolder>() {

    // ViewHolder koji drži reference na UI elemente jednog item-a
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

    // Kreira novi ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimulationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simulation, parent, false)
        return SimulationViewHolder(view)
    }

    // Povezuje podatke simulacije sa UI elementima
    override fun onBindViewHolder(holder: SimulationViewHolder, position: Int) {
        val simulation = simulations[position]

        // Postavlja ikonu u zavisnosti od tipa simulacije
        val iconRes = when (simulation.type) {
            SimulationType.TOTAL_SPACES -> R.drawable.ic_parking_total
            SimulationType.FREE_SPACES -> R.drawable.ic_parking_free
            SimulationType.OCCUPIED_SPACES -> R.drawable.ic_parking_occupied
            SimulationType.ALL -> R.drawable.ic_parking_all
            else -> R.drawable.ic_simulation_default
        }
        holder.icon.setImageResource(iconRes)
        holder.name.text = simulation.name
        holder.type.text = "Type: ${simulation.type.name}"
        holder.value.text = "Value: ${simulation.value}"
        holder.interval.text = "Interval: ${simulation.interval}"
        holder.location.text = "Location: ${simulation.location}"

        // Postavlja switch bez listenera da bi se izbeglo pozivanje prilikom inicijalizacije
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = simulation.isActive

        // Menja boju kartice u zavisnosti od statusa simulacije
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

        // Postavlja listener za switch
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChanged(simulation, isChecked)
        }

        // Postavlja listener za klik na karticu
        holder.card.setOnClickListener {
            onItemClicked(simulation)
        }

        // Postavlja listener za dugme za brisanje
        holder.delete.setOnClickListener {
            onDeleteClicked(simulation)
        }
    }

    override fun getItemCount(): Int = simulations.size

    // Dodaje novu simulaciju na početak liste
    fun addSimulation(simulation: Simulation) {
        simulations.add(0, simulation)
        notifyItemInserted(0)
    }

    // Uklanja simulaciju iz liste
    fun removeSimulation(simulation: Simulation) {
        val index = simulations.indexOfFirst { it.id == simulation.id }
        if (index != -1) {
            simulations.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
