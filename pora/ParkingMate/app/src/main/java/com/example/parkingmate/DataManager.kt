package com.example.parkingmate

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

// Klasa za upravljanje podacima simulacija korišćenjem SharedPreferences
class DataManager(private val context: Context) {

    // SharedPreferences za perzistentno čuvanje podataka
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("parking_simulations", Context.MODE_PRIVATE)
    private val gson = Gson() // JSON parser

    companion object {
        private const val KEY_SIMULATIONS = "saved_simulations"
    }

    // Čuva listu simulacija u SharedPreferences kao JSON string
    fun saveSimulations(simulations: List<Simulation>) {
        val json = gson.toJson(simulations)
        sharedPreferences.edit()
            .putString(KEY_SIMULATIONS, json)
            .apply()
    }

    // Učitava listu simulacija iz SharedPreferences
    fun loadSimulations(): List<Simulation> {
        val json = sharedPreferences.getString(KEY_SIMULATIONS, null)
        return if (json != null) {
            val type: Type = object : TypeToken<List<Simulation>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Dodaje novu simulaciju na početak liste i čuva je
    fun addSimulation(simulation: Simulation) {
        val currentList = loadSimulations().toMutableList()
        currentList.add(0, simulation) // Dodaje na početak
        saveSimulations(currentList)
    }

    // Briše simulaciju iz lokalnog skladišta i sa servera
    fun deleteSimulation(simulation: Simulation) {
        val simulations = loadSimulations().toMutableList()
        val index = simulations.indexOfFirst { it.id == simulation.id }
        if (index != -1) {
            simulations.removeAt(index)
            saveSimulations(simulations)
        }
        // Takođe briše sa servera
        ApiClient.deleteSimulation(simulation.id)
    }

    // Ažurira postojeću simulaciju
    fun updateSimulation(updatedSimulation: Simulation) {
        val currentList = loadSimulations().toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedSimulation.id }
        if (index != -1) {
            currentList[index] = updatedSimulation
            saveSimulations(currentList)
        }
    }
}
