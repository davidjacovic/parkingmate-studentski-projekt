package org.example

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private val json = Json { ignoreUnknownKeys = true }
private val client = OkHttpClient()

fun uploadParkingLocation(
    parkingLocation: ParkingLocation,
    tariffs: List<Tariff>,
    onComplete: (Boolean, String?) -> Unit
) {
    val dto = parkingLocation.toUploadDTO()
    val requestBody = json.encodeToString(dto).toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url("http://localhost:3002/parkingLocations")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onComplete(false, "Error uploading parking location: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    onComplete(false, "Failed with code ${response.code}")
                    return
                }

                val locationId = json.parseToJsonElement(response.body!!.string()).jsonObject["_id"]?.jsonPrimitive?.content
                if (locationId == null) {
                    onComplete(false, "No ID returned from backend.")
                    return
                }

                // Now upload all tariffs
                var successCount = 0
                tariffs.forEach { tariff ->
                    uploadTariff(tariff.toUploadDTO(locationId)) { success, message ->
                        if (!success) onComplete(false, message)
                        successCount++
                        if (successCount == tariffs.size) onComplete(true, null)
                    }
                }
            }
        }
    })
}

fun uploadTariff(dto: TariffUploadDTO, onComplete: (Boolean, String?) -> Unit) {
    val requestBody = json.encodeToString(dto).toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("http://localhost:3002/tariffs")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onComplete(false, "Error uploading tariff: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                onComplete(true, null)
            } else {
                onComplete(false, "Tariff upload failed with code ${response.code}")
            }
        }
    })
}
suspend fun uploadParkingLocationWithTariffs(
    location: ParkingLocation,
    tariffs: List<Tariff>
): Boolean {
    val client = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true }

    val locationDTO = location.toUploadDTO()
    val locationJson = json.encodeToString(ParkingLocationUploadDTO.serializer(), locationDTO)
    val requestBody = locationJson.toRequestBody("application/json".toMediaType())

    val locationRequest = Request.Builder()
        .url("http://localhost:3002/parkingLocations")
        .post(requestBody)
        .build()

    return withContext(Dispatchers.IO) {
        client.newCall(locationRequest).execute().use { response ->
            if (!response.isSuccessful) return@withContext false

            val body = response.body?.string() ?: return@withContext false
            val savedLocation = json.decodeFromString<JsonObject>(body)
            val locationId = savedLocation["_id"]?.jsonPrimitive?.contentOrNull ?: return@withContext false

            if (tariffs.isEmpty()) return@withContext true

            for (tariff in tariffs) {
                val tariffDTO = tariff.toUploadDTO(locationId)
                val tariffJson = json.encodeToString(TariffUploadDTO.serializer(), tariffDTO)
                val tariffRequest = Request.Builder()
                    .url("http://localhost:3002/tariffs")
                    .post(tariffJson.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(tariffRequest).execute().use { tariffResponse ->
                    if (!tariffResponse.isSuccessful) return@withContext false
                }
            }

            return@withContext true
        }
    }
}


