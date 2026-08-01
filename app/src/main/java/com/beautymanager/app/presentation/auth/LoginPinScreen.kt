package com.beautymanager.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beautymanager.app.core.util.rememberBiometricAuthenticator

private val KEYPAD_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("bio", "0", "back")
)

@Composable
fun LoginPinScreen(
    onAuthenticated: () -> Unit,
    viewModel: LoginPinViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val authenticateWithBiometrics = rememberBiometricAuthenticator(
        onSuccess = { viewModel.onBiometricSuccess(onAuthenticated) }
    )

    // Envia automaticamente quando o PIN atinge 4 dígitos (login) — evita um toque extra.
    LaunchedEffect(state.pin) {
        if (!state.needsFirstTimeSetup && state.pin.length == 4) {
            viewModel.onSubmitLogin(onAuthenticated)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BeautyManager",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (state.needsFirstTimeSetup) "Crie um PIN para o administrador" else "Digite seu PIN",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        PinDots(filledCount = state.pin.length)

        if (state.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(40.dp))

        KEYPAD_ROWS.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    when (key) {
                        "back" -> KeypadIconButton(Icons.Filled.Backspace, "Apagar") { viewModel.onBackspace() }
                        "bio" -> if (state.biometricAvailableAndEnabled) {
                            KeypadIconButton(Icons.Filled.Fingerprint, "Biometria") {
                                authenticateWithBiometrics()
                            }
                        } else {
                            Spacer(Modifier.size(64.dp))
                        }
                        else -> KeypadDigitButton(key) { viewModel.onDigitPressed(key) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (state.needsFirstTimeSetup) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.onConfirmFirstTimeSetup(onAuthenticated) },
                enabled = state.pin.length >= 4,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) { Text("Confirmar PIN") }
        }
    }
}

@Composable
private fun PinDots(filledCount: Int, total: Int = 6) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(total) { index ->
            val filled = index < filledCount
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun KeypadDigitButton(digit: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(digit, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun KeypadIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = androidx.compose.ui.graphics.Color.Transparent, modifier = Modifier.size(64.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description)
        }
    }
}
