package com.example.firesafe.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.firesafe.theme.CardBorder
import com.example.firesafe.theme.DarkBackground
import com.example.firesafe.theme.ElevatedCardBackground
import com.example.firesafe.theme.EmergencyRed
import com.example.firesafe.theme.InteractiveTeal
import com.example.firesafe.theme.TextPrimary
import com.example.firesafe.theme.TextSecondary
import com.example.firesafe.viewmodel.EmergencyFlowState
import com.example.firesafe.viewmodel.EmergencyViewModel

@Composable
fun LocationDetectingScreen(
    viewModel: EmergencyViewModel,
    onResolved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Pulse animation for the Pin Icon
    val infiniteTransition = rememberInfiniteTransition(label = "PinPulse")
    val pinScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PinScale"
    )

    // Check permissions and resolve location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.detectLocation()
        } else {
            viewModel.detectLocation() // Triggers fallback in ViewModel
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.detectLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Determine state values
    val isResolved = uiState is EmergencyFlowState.AlertSent
    val addressText = if (uiState is EmergencyFlowState.AlertSent) {
        (uiState as EmergencyFlowState.AlertSent).location.address
    } else {
        ""
    }

    // Progress bar animation once resolved
    var progressTarget by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
        label = "ResolutionProgress"
    )

    LaunchedEffect(isResolved) {
        if (isResolved) {
            progressTarget = 1f
        }
    }

    // Auto-navigate once progress hits 100%
    LaunchedEffect(animatedProgress) {
        if (animatedProgress >= 1.0f) {
            onResolved()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()
        ) {
            // Pulsing location pin icon
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location Pin",
                tint = if (uiState is EmergencyFlowState.LocationResolved && (uiState as EmergencyFlowState.LocationResolved).permissionDenied) Color(0xFFFF9800) else InteractiveTeal,
                modifier = Modifier
                    .size(80.dp)
                    .scale(pinScale)
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (val state = uiState) {
                is EmergencyFlowState.DetectingLocation -> {
                    Text(
                        text = "Detecting your location...",
                        style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Securing GPS coordinates and finding nearby stations...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = InteractiveTeal,
                        trackColor = Color(0xFF2A2A2A)
                    )
                }

                is EmergencyFlowState.LocationResolved -> {
                    val title = if (state.permissionDenied) "Location Access Required" else "Location Detected"
                    val desc = if (state.permissionDenied) {
                        "GPS permissions were denied. Please enter your address manually to dispatch emergency responders."
                    } else {
                        "Double-check and verify the detected address below. Adjust details if necessary before dispatching."
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = if (state.permissionDenied) Color(0xFFFF9800) else TextPrimary
                        ),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = state.editedAddress,
                        onValueChange = { viewModel.updateEditedAddress(it) },
                        label = { Text("Emergency Response Address", color = TextSecondary) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = InteractiveTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = ElevatedCardBackground,
                            unfocusedContainerColor = ElevatedCardBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = EmergencyRed),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.resolveLocationAndDispatch(onResolved = {}) },
                        enabled = state.editedAddress.trim().isNotEmpty() && !state.isDispatching,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyRed,
                            disabledContainerColor = EmergencyRed.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (state.isDispatching) {
                            CircularProgressIndicator(
                                color = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "CONFIRM & DISPATCH ALERT",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    if (state.permissionDenied) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Text("GRANT LOCATION PERMISSIONS", color = InteractiveTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is EmergencyFlowState.AlertSent -> {
                    Text(
                        text = "Location Resolved",
                        style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = addressText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = InteractiveTeal,
                            trackColor = Color(0xFF2A2A2A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Broadcasting Emergency Signal... ${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(color = InteractiveTeal),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
