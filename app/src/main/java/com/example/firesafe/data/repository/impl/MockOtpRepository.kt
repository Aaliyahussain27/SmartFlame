package com.example.firesafe.data.repository.impl

import com.example.firesafe.data.repository.OtpRepository
import kotlinx.coroutines.delay

class MockOtpRepository : OtpRepository {
    override suspend fun sendOtp(phoneNumber: String): Boolean {
        delay(1500)
        return true
    }

    override suspend fun verifyOtp(phoneNumber: String, code: String): Boolean {
        delay(1500)
        return code == "1234"
    }
}
