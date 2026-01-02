package com.example.parkingmate

import java.io.Serializable
import java.util.*

// Data klasa koja predstavlja simulaciju parking podataka
data class Simulation(
    val id: String = UUID.randomUUID().toString(), // Jedinstveni identifikator
    val name: String, // Naziv simulacije
    val type: SimulationType, // Tip simulacije (TOTAL_SPACES, FREE_SPACES, itd.)
    val value: String, // Vrednost koja se šalje (broj parking mesta)
    val interval: String, // Vremenski interval između slanja (HH:MM:SS)
    val location: String, // GPS koordinate (lat,lon)
    val isActive: Boolean = false, // Da li je simulacija aktivna
    val createdAt: Date = Date(), // Vreme kreiranja
    val lastRun: Date? = null, // Vreme poslednjeg izvršenja
    val runCount: Int = 0 // Broj puta koliko je simulacija izvršena
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L // Za serijalizaciju
    }
}
