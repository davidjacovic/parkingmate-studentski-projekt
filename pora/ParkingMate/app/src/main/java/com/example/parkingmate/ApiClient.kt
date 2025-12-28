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

    private const val EMULATOR_URL = "http://10.0.2.2:3002"
    private const val PHONE_URL = "http://192.168.56.1:3002"

    private fun getBaseUrl(): String {
        return if (Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86")) {
            "http://10.0.2.2:3002"
        } else {
            "http://192.168.56.1:3002"
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun uploadParkingImage(
        parkingLocationId: String,
        lat: Double?,
        lon: Double?,
        imagePath: String
    ) {
        val file = File(imagePath)
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBodyFile = file.asRequestBody(mediaType)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("parkingLocationId", parkingLocationId)
            .addFormDataPart("coordinates[]", lon?.toString() ?: "")
            .addFormDataPart("coordinates[]", lat?.toString() ?: "")
            .addFormDataPart("imageUrl", file.name)
            .addFormDataPart("file", file.name, requestBodyFile)
            .build()

        val request = Request.Builder()
            .url("http://10.0.2.2:3002/api/parking-images") // emulator URL
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
