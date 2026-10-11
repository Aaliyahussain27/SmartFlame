package com.example.firesafe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firesafe.theme.InteractiveTeal
import com.example.firesafe.theme.TextPrimary
import com.example.firesafe.theme.ElevatedCardBackground

@Composable
fun OtpInputField(
    otpLength: Int = 6,
    onOtpComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var otpValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Hidden BasicTextField to capture input
        BasicTextField(
            value = otpValue,
            onValueChange = { newValue ->
                val digitsOnly = newValue.filter { it.isDigit() }
                if (digitsOnly.length <= otpLength) {
                    otpValue = digitsOnly
                    onOtpComplete(digitsOnly)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .size(1.dp)
                .alpha(0f)
        )

        // Visual Row of boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() }
        ) {
            for (i in 0 until otpLength) {
                val char = if (i < otpValue.length) otpValue[i].toString() else ""

                // Highlight only the currently active slot when focused
                // The active slot is the first empty box, or the last box if filled
                val isBoxFocused = isFocused && (
                        (i == otpValue.length) || (i == otpLength - 1 && otpValue.length == otpLength)
                        )

                Box(
                    modifier = Modifier
                        // Every box takes an equal share of the row, so all 6 are the same size on any screen
                        .weight(1f)
                        .height(56.dp)
                        .background(ElevatedCardBackground, RoundedCornerShape(8.dp))
                        .border(
                            width = if (isBoxFocused) 2.dp else 1.dp,
                            color = if (isBoxFocused) InteractiveTeal else Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        style = TextStyle(
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }

    // Auto-focus on start
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}