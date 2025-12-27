import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:3002"
    private val client = OkHttpClient()

    fun register(
        username: String,
        email: String,
        password: String,
        phone: String,
        creditCard: String,
        registrationNumber: String,
        callback: (success: Boolean, response: String?) -> Unit
    ) {
        val json = JSONObject()
        json.put("username", username)
        json.put("email", email)
        json.put("password", password)
        json.put("phone_number", phone)
        json.put("credit_card_number", creditCard)
        json.put("registration_number", registrationNumber)

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/users")
            .post(body)
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
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
        val json = JSONObject()
        json.put("username", username)
        json.put("password", password)

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/users/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful, response.body?.string())
            }
        })
    }
}
