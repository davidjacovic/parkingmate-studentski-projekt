package com.example.parkingmate

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
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
    const val PHONE_URL = "http://172.20.10.8:3002"

    // Određuje bazni URL na osnovu tipa uređaja
    fun getBaseUrl(): String = PHONE_URL
    /*fun getBaseUrl(): String {
        return if (android.os.Build.FINGERPRINT.contains("generic")) {
            EMULATOR_URL
        } else {
            PHONE_URL
        }
    }*/

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
                    Log.d("ML_ANALYZE", "Body: ${response.body}")
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

    // Dohvata sve dogodke sa opcionim filtriranjem
    fun getAllEvents(
        eventType: String? = null,
        status: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        limit: Int = 50,
        skip: Int = 0,
        callback: (Boolean, List<Event>?, String) -> Unit
    ) {
        // Kreiraj URL sa query parametrima
        val urlBuilder = "${getBaseUrl()}/api/events".toHttpUrlOrNull()?.newBuilder()
            ?: return callback(false, null, "Invalid URL")

        if (eventType != null) urlBuilder.addQueryParameter("eventType", eventType)
        if (status != null) urlBuilder.addQueryParameter("status", status)
        if (startDate != null) urlBuilder.addQueryParameter("startDate", startDate.toString())
        if (endDate != null) urlBuilder.addQueryParameter("endDate", endDate.toString())
        urlBuilder.addQueryParameter("limit", limit.toString())
        urlBuilder.addQueryParameter("skip", skip.toString())

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GET_EVENTS", "Failed: ${e.message}")
                callback(false, null, e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                try {
                    if (response.isSuccessful) {
                        // Parsiraj JSON odgovor
                        val jsonObject = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
                        if (jsonObject.has("success") && jsonObject.get("success").asBoolean) {
                            val eventsArray = jsonObject.getAsJsonArray("data")
                            val events = mutableListOf<Event>()

                            Log.d("GET_EVENTS", "Parsing ${eventsArray.size()} events from response")
                            eventsArray.forEachIndexed { index, element ->
                                try {
                                    val eventJson = element.asJsonObject
                                    Log.d("GET_EVENTS", "Event $index - _id type: ${eventJson.get("_id")?.javaClass?.simpleName}, value: ${eventJson.get("_id")}")
                                    Log.d("GET_EVENTS", "Event $index - timestamp type: ${eventJson.get("timestamp")?.javaClass?.simpleName}, value: ${eventJson.get("timestamp")}")
                                
                                // Helper funkcija za parsiranje _id (može biti string direktno ili {$oid: string})
                                fun parseId(jsonElement: com.google.gson.JsonElement?): String {
                                    if (jsonElement == null || jsonElement.isJsonNull) return ""
                                    return try {
                                        when {
                                            // Najčešći slučaj: direktno string
                                            jsonElement.isJsonPrimitive && jsonElement.asJsonPrimitive.isString -> 
                                                jsonElement.asString
                                            // MongoDB extended JSON format: {$oid: "..."}
                                            jsonElement.isJsonObject && jsonElement.asJsonObject.has("\$oid") -> 
                                                jsonElement.asJsonObject.get("\$oid").asString
                                            // Fallback: probaj da konvertuješ u string
                                            else -> jsonElement.toString().replace("\"", "")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("GET_EVENTS", "Error parsing _id: ${e.message}")
                                        ""
                                    }
                                }
                                
                                // Helper funkcija za parsiranje timestamp-a (može biti number, ISO string, ili {$date: ISO string})
                                fun parseTimestamp(jsonElement: com.google.gson.JsonElement?): Long? {
                                    if (jsonElement == null || jsonElement.isJsonNull) return null
                                    
                                    // ISO 8601 format parser
                                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }
                                    
                                    return when {
                                        jsonElement.isJsonPrimitive && jsonElement.asJsonPrimitive.isNumber -> 
                                            jsonElement.asLong
                                        jsonElement.isJsonPrimitive && jsonElement.asJsonPrimitive.isString -> {
                                            // ISO string format: "2026-01-14T19:35:58.992Z"
                                            val isoString = jsonElement.asString
                                            try {
                                                isoFormat.parse(isoString)?.time
                                            } catch (e: Exception) {
                                                Log.e("GET_EVENTS", "Error parsing ISO string: $isoString", e)
                                                null
                                            }
                                        }
                                        jsonElement.isJsonObject && jsonElement.asJsonObject.has("\$date") -> {
                                            val dateValue = jsonElement.asJsonObject.get("\$date")
                                            when {
                                                dateValue.isJsonPrimitive && dateValue.asJsonPrimitive.isNumber -> 
                                                    dateValue.asLong
                                                dateValue.isJsonPrimitive && dateValue.asJsonPrimitive.isString -> {
                                                    // ISO string format
                                                    val isoString = dateValue.asString
                                                    try {
                                                        isoFormat.parse(isoString)?.time
                                                    } catch (e: Exception) {
                                                        Log.e("GET_EVENTS", "Error parsing ISO string from \$date: $isoString", e)
                                                        null
                                                    }
                                                }
                                                else -> null
                                            }
                                        }
                                        else -> null
                                    }
                                }
                                
                                val event = Event(
                                    _id = parseId(eventJson.get("_id")),
                                    topic = eventJson.get("topic").asString,
                                    message = eventJson.get("message").asString,
                                    timestamp = parseTimestamp(eventJson.get("timestamp")) ?: System.currentTimeMillis(),
                                    location = eventJson.get("location").asString,
                                    eventType = EventType.valueOf(eventJson.get("eventType").asString),
                                    status = eventJson.get("status").asString,
                                    blockchainHash = if (eventJson.has("blockchainHash") && !eventJson.get("blockchainHash").isJsonNull) {
                                        eventJson.get("blockchainHash").asString
                                    } else null,
                                    blockchainTimestamp = parseTimestamp(eventJson.get("blockchainTimestamp")),
                                    createdAt = parseTimestamp(eventJson.get("createdAt")),
                                    updatedAt = parseTimestamp(eventJson.get("updatedAt"))
                                )
                                events.add(event)
                                Log.d("GET_EVENTS", "Successfully parsed event $index: ${event.topic}")
                                } catch (e: Exception) {
                                    Log.e("GET_EVENTS", "Error parsing event $index: ${e.message}", e)
                                }
                            }

                            Log.d("GET_EVENTS", "Success: ${events.size} events parsed")
                            if (events.isEmpty() && eventsArray.size() > 0) {
                                Log.e("GET_EVENTS", "Warning: ${eventsArray.size()} events in response but 0 parsed. Response: $responseBody")
                            }
                            callback(true, events, "")
                        } else {
                            val message = if (jsonObject.has("message")) jsonObject.get("message").asString else "Unknown error"
                            callback(false, null, message)
                        }
                    } else {
                        Log.e("GET_EVENTS", "Error: ${response.code} - $responseBody")
                        callback(false, null, "Server error: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.e("GET_EVENTS", "Parse error: ${e.message}")
                    callback(false, null, "Parse error: ${e.message}")
                }
            }
        })
    }
    // ✅ Story D: Poziv ML endpoint-a /api/ml/analyze (multipart field name: "image")
    fun analyzeImageMl(
        imageFile: File,
        callback: (Boolean, MlResult?, String) -> Unit
    ) {
        if (!imageFile.exists()) {
            callback(false, null, "File not found: ${imageFile.absolutePath}")
            return
        }

        val mediaType = "image/*".toMediaTypeOrNull()
        val requestBodyFile = imageFile.asRequestBody(mediaType)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            // OBAVEZNO: field name mora biti "image" (upload.single('image'))
            .addFormDataPart("image", imageFile.name, requestBodyFile)
            .build()

        val request = Request.Builder()
            .url("${getBaseUrl()}/api/ml/analyze")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ML_ANALYZE", "Failed: ${e.message}")
                callback(false, null, e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                Log.d("ML_ANALYZE", "HTTP ${response.code} Body: $responseBody")

                if (!response.isSuccessful) {
                    Log.e("ML_ANALYZE", "Error: ${response.code} - $responseBody")
                    callback(false, null, "Server error: ${response.code}")
                    return
                }

                try {
                    // Response format: { success: true, data: { free, occupied, status } }
                    val parsed = gson.fromJson(responseBody, MlAnalyzeResponse::class.java)
                    Log.d("ML_ANALYZE", "Parsed: $parsed")
                    if (parsed.success) {
                        callback(true, parsed.data, "")
                    } else {
                        callback(false, null, "ML returned success=false")
                    }

                } catch (e: Exception) {
                    Log.e("ML_ANALYZE", "Parse error: ${e.message}. Body=$responseBody")
                    callback(false, null, "Parse error: ${e.message}")
                }
            }
        })
    }
    fun saveParkingLocationByAddress(
        name: String,
        address: String,
        lat: Double,
        lon: Double,
        total: Int,
        free: Int,
        // TODO: ako imaš auth token, dodaj parametar token: String
        callback: (Boolean, String) -> Unit
    ) {
        val bodyMap = hashMapOf<String, Any>(
            "name" to name,
            "address" to address,
            "location" to hashMapOf(
                "type" to "Point",
                "coordinates" to listOf(lon, lat) // [lng, lat]
            ),
            "total_regular_spots" to total,
            "available_regular_spots" to free
        )

        val json = gson.toJson(bodyMap)
        val reqBody = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val requestBuilder = Request.Builder()
            .url("${getBaseUrl()}/parkingLocations")
            .post(reqBody)

        // Ako ima auth token:
        // requestBuilder.addHeader("Authorization", "Bearer $token")

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SAVE_PARKING_LOC", "Failed: ${e.message}")
                callback(false, e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                val resp = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.d("SAVE_PARKING_LOC", "Success: $resp")
                    callback(true, resp)
                } else {
                    Log.e("SAVE_PARKING_LOC", "Error ${response.code}: $resp")
                    callback(false, "Server error ${response.code}: $resp")
                }
            }
        })
    }

}
