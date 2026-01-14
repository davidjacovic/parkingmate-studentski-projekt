package com.example.parkingmate

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    // Konfiguracija HTTP klijenta sa timeout-ovima
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    
    // Gson za JSON serializaciju
    private val gson = Gson()

    // URL adrese za emulator i fizički uređaj
    const val EMULATOR_URL = "http://10.0.2.2:3002"
    const val PHONE_URL = "http://192.168.0.12:3002"

    // Određuje bazni URL na osnovu tipa uređaja
    fun getBaseUrl(): String {
        return if (android.os.Build.FINGERPRINT.contains("generic")) {
            EMULATOR_URL
        } else {
            PHONE_URL
        }
    }

    // Briše simulaciju sa servera
    fun deleteSimulation(simulationId: String) {
        val request = Request.Builder()
            .url("${getBaseUrl()}/api/parking-images/$simulationId")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DELETE_SIMULATION", "Failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("DELETE_SIMULATION", "Simulation deleted successfully")
                } else {
                    Log.e("DELETE_SIMULATION", "Error: ${response.code}")
                }
            }
        })
    }

    // Šalje simulirane podatke o parking mestima na server
    fun uploadSimulatedData(jsonBody: String) {
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("${getBaseUrl()}/api/parking-images/simulated")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SIMULATED_UPLOAD", "Failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("SIMULATED_UPLOAD", "Success")
                } else {
                    Log.e("SIMULATED_UPLOAD", "Error: ${response.code}")
                }
            }
        })
    }

    // Upload-uje sliku parkinga sa meta-podacima (lokacija, timestamp)
    fun uploadParkingImage(
        parkingLocationId: String,
        lat: Double?,
        lon: Double?,
        timestamp: Long?,
        imagePath: String
    ) {
        val file = File(imagePath)
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBodyFile = file.asRequestBody(mediaType)
        val coordinates = "${lon ?: 0},${lat ?: 0}"

        // Kreira multipart zahtev za slanje slike i meta-podataka
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("parkingLocationId", parkingLocationId)
            .addFormDataPart("coordinates", coordinates)
            .addFormDataPart("timestamp", timestamp?.toString() ?: "")
            .addFormDataPart("file", file.name, requestBodyFile)
            .build()

        val request = Request.Builder()
            .url("${getBaseUrl()}/api/parking-images")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UPLOAD", "Failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("UPLOAD", "Success: ${response.body?.string()}")
                } else {
                    Log.e("UPLOAD", "Error: ${response.code}")
                }
            }
        })
    }

    // Šalje dogodak na backend server
    fun sendEvent(event: Event, eventType: EventType, callback: (Boolean, String) -> Unit) {
        // Kreiraj JSON body koristeći Gson za pravilnu serializaciju
        val eventData = hashMapOf<String, Any>(
            "topic" to event.topic,
            "message" to event.message,
            "timestamp" to event.timestamp,
            "location" to event.location,
            "eventType" to eventType.name
        )
        val jsonBody = gson.toJson(eventData)

        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("${getBaseUrl()}/api/events")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SEND_EVENT", "Failed: ${e.message}")
                callback(false, e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.d("SEND_EVENT", "Success: $responseBody")
                    callback(true, "")
                } else {
                    Log.e("SEND_EVENT", "Error: ${response.code} - $responseBody")
                    callback(false, "Server error: ${response.code}")
                }
            }
        })
    }
}
