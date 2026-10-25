package com.veilweather.app.ui.screens.unlock

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.veilweather.app.security.SecurityManager
import com.veilweather.app.security.BiometricManager
import kotlinx.coroutines.launch

enum class UnlockStep {
    CODE,      // Only used when directly opening unlock screen
    FINGERPRINT,
    UNLOCKED
}

@Composable
fun UnlockScreen(
    onUnlocked: (isReal: Boolean) -> Unit,
    onCancel: () -> Unit,
    securityManager: SecurityManager = SecurityManager.getInstance(),
    biometricManager: BiometricManager? = null,
    skipCodeEntry: Boolean = false // New parameter - skip code if entered via search
) {
    var step by remember { 
        mutableStateOf(if (skipCodeEntry) UnlockStep.FINGERPRINT else UnlockStep.CODE) 
    }
    var error by remember { mutableStateOf("") }
    var isWaiting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                UnlockStep.CODE -> {
                    Text(
                        text = "Enter Code",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    var codeInput by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("Code") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (error.isNotEmpty()) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            isWaiting = true
                            scope.launch {
                                val result = securityManager.verifyCode(codeInput)
                                when {
                                    result == SecurityManager.CodeResult.VALID -> {
                                        step = UnlockStep.FINGERPRINT
                                        error = ""
                                    }
                                    result == SecurityManager.CodeResult.DURESS -> {
                                        // Go directly to duress (fake) messenger
                                        onUnlocked(false)
                                    }
                                    result == SecurityManager.CodeResult.INVALID -> {
                                        error = "Invalid code"
                                    }
                                }
                                isWaiting = false
                            }
                        },
                        enabled = !isWaiting && codeInput.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isWaiting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Verify")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }

                UnlockStep.FINGERPRINT -> {
                    Text(
                        text = "Biometric Verification",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Show fingerprint icon
                    Text(
                        text = "Touch the fingerprint sensor",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                // Check if biometric is available
                                if (biometricManager?.isBiometricAvailable() == true) {
                                    val success = biometricManager.authenticate()
                                    if (success) {
                                        securityManager.unlock()
                                        onUnlocked(true)
                                    } else {
                                        error = "Biometric failed"
                                    }
                                } else {
                                    // No biometric - unlock directly
                                    securityManager.unlock()
                                    onUnlocked(true)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Authenticate")
                    }

                    if (error.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { step = UnlockStep.CODE }) {
                        Text("Use Code Instead")
                    }
                }

                UnlockStep.UNLOCKED -> {
                    Text(
                        text = "Unlocked!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}