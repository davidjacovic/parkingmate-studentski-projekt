package com.example.parkingmate

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class DataManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("parking_simulations", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_SIMULATIONS = "saved_simulations"
    }

    fun saveSimulations(simulations: List<Simulation>) {
        val json = gson.toJson(simulations)
        sharedPreferences.edit()
            .putString(KEY_SIMULATIONS, json)
            .apply()
    }

    fun loadSimulations(): List<Simulation> {
        val json = sharedPreferences.getString(KEY_SIMULATIONS, null)
        return if (json != null) {
            val type: Type = object : TypeToken<List<Simulation>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun addSimulation(simulation: Simulation) {
        val currentList = loadSimulations().toMutableList()
        currentList.add(0, simulation)
        saveSimulations(currentList)
    }

    fun updateSimulation(updatedSimulation: Simulation) {
        val currentList = loadSimulations().toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedSimulation.id }
        if (index != -1) {
            currentList[index] = updatedSimulation
            saveSimulations(currentList)
        }
    }
}