package com.example.firesafe.data.repository.impl

import android.util.Log
import com.example.firesafe.BuildConfig
import com.example.firesafe.data.repository.OtpRepository
import com.example.firesafe.data.repository.impl.network.Msg91ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Msg91OtpRepository(
    private val authKey: String = BuildConfig.MSG91_AUTH_KEY,
    private val templateId: String = BuildConfig.MSG91_TEMPLATE_ID
) : OtpRepository {

    private val apiService: Msg91ApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://control.msg91.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(Msg91ApiService::class.java)
    }

    override suspend fun sendOtp(phoneNumber: String): Boolean {
        val cleanNumber = formatForMsg91(phoneNumber)
        try {
            val response = apiService.sendOtp(
                authKey = authKey,
                templateId = templateId,
                mobile = cleanNumber,
                channel = "sms" // Can be configured later for whatsapp if template approved
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.type == "success") {
                    return true
                } else {
                    Log.e("Msg91OtpRepository", "Send OTP failed with MSG91 response: ${body?.message}")
                }
            } else {
                Log.e("Msg91OtpRepository", "Send OTP request failed: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("Msg91OtpRepository", "Network exception sending OTP", e)
            throw e // Throw so ViewModel can handle network failure vs logic failure
        }
        return false
    }

    override suspend fun verifyOtp(phoneNumber: String, code: String): Boolean {
        val cleanNumber = formatForMsg91(phoneNumber)
        
        try {
            val response = apiService.verifyOtp(
                authKey = authKey,
                mobile = cleanNumber,
                otp = code
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.type == "success") {
                    return true
                } else {
                    Log.e("Msg91OtpRepository", "Verify OTP failed: ${body?.message}")
                }
            } else {
                Log.e("Msg91OtpRepository", "Verify OTP request failed: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("Msg91OtpRepository", "Network exception verifying OTP", e)
            throw e
        }
        return false
    }

    private fun formatForMsg91(phoneNumber: String): String {
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        return if (digitsOnly.length == 10) {
            "91$digitsOnly"
        } else if (digitsOnly.startsWith("91") && digitsOnly.length == 12) {
            digitsOnly
        } else {
            digitsOnly
        }
    }
}
