import android.os.Build
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun uploadParkingImage(
        parkingLocationId: String,
        lat: Double?,
        lon: Double?,
        timestamp: String?,
        imagePath: String
    ) {
        val file = File(imagePath)
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBodyFile = file.asRequestBody(mediaType)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("parkingLocationId", parkingLocationId)
            .addFormDataPart("coordinates", "${lat ?: 0},${lon ?: 0}")
            .addFormDataPart("timestamp", timestamp ?: "")
            .addFormDataPart("file", file.name, requestBodyFile)
            .build()

        val request = Request.Builder()
            .url("http://10.0.2.2:3002/api/parking-images")
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
}
