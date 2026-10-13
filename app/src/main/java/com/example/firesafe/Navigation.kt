package com.example.firesafe

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.firesafe.data.repository.impl.GPSLocationRepository
import com.example.firesafe.data.repository.impl.LiveLocationFireStationRepository
import com.example.firesafe.data.repository.impl.MockOtpRepository
import com.example.firesafe.data.repository.impl.Msg91OtpRepository
import com.example.firesafe.data.repository.impl.SupabaseAlertRepository
import com.example.firesafe.ui.screens.*
import com.example.firesafe.viewmodel.EmergencyEvent
import com.example.firesafe.viewmodel.EmergencyViewModel

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Scoped shared ViewModel across the entire navigation flow
    val viewModel: EmergencyViewModel = viewModel {
        EmergencyViewModel(
            locationRepository = GPSLocationRepository(context.applicationContext),
            fireStationRepository = LiveLocationFireStationRepository(context.applicationContext),
            alertRepository = SupabaseAlertRepository(context.applicationContext),
            otpRepository = if (BuildConfig.USE_MOCK_OTP) MockOtpRepository(context.applicationContext) else Msg91OtpRepository()
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EmergencyEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onEmergencyClick = {
                    viewModel.startEmergency()
                    navController.navigate("location_detecting")
                }
            )
        }
        
        composable("location_detecting") {
            LocationDetectingScreen(
                viewModel = viewModel,
                onResolved = {
                    navController.navigate("alert_sent") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
        
        composable("alert_sent") {
            AlertSentScreen(
                viewModel = viewModel,
                onNext = {
                    navController.navigate("photo_proof")
                },
                onCancelled = {
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }
        
        composable("photo_proof") {
            PhotoProofScreen(
                viewModel = viewModel,
                onNext = {
                    navController.navigate("phone_verification")
                }
            )
        }
        
        composable("phone_verification") {
            PhoneVerificationScreen(
                viewModel = viewModel,
                onVerified = {
                    navController.navigate("final_confirmation") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
        
        composable("final_confirmation") {
            FinalConfirmationScreen(
                viewModel = viewModel,
                onExit = {
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }
    }
}
