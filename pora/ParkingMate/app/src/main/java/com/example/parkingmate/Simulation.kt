package com.example.parkingmate

import java.io.Serializable
import java.util.*

data class Simulation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: SimulationType,
    val value: String,
    val interval: String,
    val location: String,
    val isActive: Boolean = false,
    val createdAt: Date = Date(),
    val lastRun: Date? = null,
    val runCount: Int = 0
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}