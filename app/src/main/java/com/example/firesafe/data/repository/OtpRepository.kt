package com.example.firesafe.data.repository

interface OtpRepository {
    suspend fun sendOtp(phoneNumber: String): Boolean
    suspend fun verifyOtp(phoneNumber: String, code: String): Boolean
}
