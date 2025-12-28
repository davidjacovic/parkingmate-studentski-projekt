import android.os.Build
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
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

    fun register(
        username: String,
        email: String,
        password: String,
        phone: String,
        creditCard: String,
        registrationNumber: String,
        callback: (success: Boolean, response: String?) -> Unit
    ) {
        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
            put("phone_number", phone)
            put("credit_card_number", creditCard)
            put("registration_number", registrationNumber)
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("${getBaseUrl()}/users")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful, response.body?.string())
            }
        })
    }

    fun login(
        username: String,
        password: String,
        callback: (success: Boolean, response: String?) -> Unit
    ) {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("${getBaseUrl()}/users/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful, response.body?.string())
            }
        })
    }
}
