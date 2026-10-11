package com.example.firesafe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firesafe.data.model.HistoryItem
import com.example.firesafe.data.repository.impl.SupabaseHistoryRepository
import com.example.firesafe.viewmodel.HistoryUiState
import com.example.firesafe.viewmodel.HistoryViewModel
import com.example.firesafe.theme.*

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyViewModel: HistoryViewModel = viewModel {
        HistoryViewModel(SupabaseHistoryRepository(context.applicationContext))
    }
    val uiState by historyViewModel.state.collectAsState()

    // Reload from the database every time this tab is opened
    LaunchedEffect(Unit) { historyViewModel.load() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "DISPATCH HISTORY",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.0.sp
            ),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        when (val state = uiState) {
            is HistoryUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = InteractiveTeal)
                }
            }

            is HistoryUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium.copy(color = EmergencyRed),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { historyViewModel.load() },
                        colors = ButtonDefaults.buttonColors(containerColor = InteractiveTeal)
                    ) {
                        Text("RETRY", fontWeight = FontWeight.Bold, color = DarkBackground)
                    }
                }
            }

            is HistoryUiState.Loaded -> {
                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No alerts yet.\nReports you send from this phone will show up here.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.items) { item ->
                            HistoryItemCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: HistoryItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElevatedCardBackground, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.alertId,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (item.isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (item.isResolved) SuccessGreen else EmergencyRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (item.isResolved) SuccessGreen else EmergencyRed,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Text(
                text = item.address,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
            }
        }
    }
}
