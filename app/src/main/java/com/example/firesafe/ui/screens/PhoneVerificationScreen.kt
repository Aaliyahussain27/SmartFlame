package com.example.firesafe.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.firesafe.theme.*
import com.example.firesafe.ui.components.OtpInputField
import com.example.firesafe.viewmodel.EmergencyFlowState
import com.example.firesafe.viewmodel.EmergencyViewModel

@Composable
fun PhoneVerificationScreen(
    viewModel: EmergencyViewModel,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val state = uiState as? EmergencyFlowState.AwaitingOTP ?: return

    var enteredOtpCode by remember { mutableStateOf("") }

    // Telephony Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val phone = getLine1Number(context)
            if (phone.isNotEmpty()) {
                viewModel.updatePhoneNumber(phone)
            }
        }
    }

    // Auto-detect phone number on entry if permitted
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val phone = getLine1Number(context)
            if (phone.isNotEmpty()) {
                viewModel.updatePhoneNumber(phone)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Text Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "IDENTITY VERIFICATION",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = InteractiveTeal,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verify Phone Number",
                    style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Emergency protocols require phone validation to prevent false reports and coordinate rescue.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // Input fields Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElevatedCardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Phone OutlinedTextField
                    OutlinedTextField(
                        value = state.phoneNumber,
                        onValueChange = { viewModel.updatePhoneNumber(it) },
                        label = { Text("Phone Number", color = TextSecondary) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = InteractiveTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.phoneError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.phoneError!!,
                            color = EmergencyRed,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.sendOtp(state.phoneNumber) },
                        enabled = !state.isSendingOtp && state.phoneNumber.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InteractiveTeal,
                            contentColor = DarkBackground,
                            disabledContainerColor = InteractiveTeal.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (state.isSendingOtp) {
                            CircularProgressIndicator(
                                color = DarkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text("SEND OTP", fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "ENTER 6-DIGIT OTP",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.50.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6-Box OTP input
                    OtpInputField(
                        otpLength = 6,
                        onOtpComplete = { enteredOtpCode = it }
                    )

                    // Error Message
                    if (state.otpError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.otpError!!,
                            color = EmergencyRed,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Verify button and countdown
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Countdown timer link
                if (state.countdownSeconds > 0) {
                    Text(
                        text = "Resend code in ${state.countdownSeconds}s",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                } else {
                    Text(
                        text = "Resend OTP Code",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = InteractiveTeal,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable {
                            viewModel.resendOtp()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Verify Button (EmergencyRed as it's part of the critical path)
                Button(
                    onClick = {
                        viewModel.verifyOtp(enteredOtpCode, onVerified)
                    },
                    enabled = enteredOtpCode.length == 6 && !state.isVerifying && state.phoneNumber.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed,
                        disabledContainerColor = EmergencyRed.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (state.isVerifying) {
                        CircularProgressIndicator(
                            color = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "VERIFY & FORWARD REPORT",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun getLine1Number(context: Context): String {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    return try {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.line1Number ?: ""
        } else {
            ""
        }
    } catch (e: SecurityException) {
        ""
    }
}
