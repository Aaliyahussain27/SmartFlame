package com.example.firesafe.data.repository.impl

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.firesafe.data.repository.OtpRepository
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * PROTOTYPE / DEMO ONLY (turned on with USE_MOCK_OTP=true in local.properties).
 * No real SMS is sent. When "SEND OTP" is tapped, a random 6-digit code is created and shown on
 * the phone screen as a pop-up, like an incoming SMS. "123456" also always works as a backup.
 */
class MockOtpRepository(private val context: Context? = null) : OtpRepository {

    private val codes = mutableMapOf<String, String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override suspend fun sendOtp(phoneNumber: String): Boolean {
        delay(1200) // feels like a real network call
        val code = Random.nextInt(100000, 1000000).toString()
        codes[key(phoneNumber)] = code
        Log.d("MockOtpRepository", "DEMO OTP for ${key(phoneNumber)} is $code")

        context?.let { ctx ->
            mainHandler.post {
                val msg = "SmartFlame (demo SMS): Your verification code is $code"
                // Two long pop-ups back to back so the code stays on screen for about 7 seconds
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
            }
        }
        return true
    }

    override suspend fun verifyOtp(phoneNumber: String, code: String): Boolean {
        delay(800)
        val entered = code.trim()
        return entered == codes[key(phoneNumber)] || entered == "123456"
    }

    // The same number typed with spaces or +91 must map to the same key
    private fun key(phone: String): String = phone.filter { it.isDigit() }.takeLast(10)
}