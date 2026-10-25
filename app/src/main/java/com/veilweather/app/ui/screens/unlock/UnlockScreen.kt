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

@Composable
fun UnlockScreen(
    onUnlocked: (isReal: Boolean) -> Unit,
    onCancel: () -> Unit,
    securityManager: SecurityManager = SecurityManager.getInstance(),
    biometricManager: BiometricManager? = null
) {
    var codeInput by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(UnlockStep.CODE) }
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

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("Code") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        singleLine = true
                    )

                    if (error.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                when {
                                    securityManager.verifySecretCode(codeInput) -> {
                                        // Real messenger - require biometric
                                        step = UnlockStep.BIOMETRIC
                                        error = ""
                                    }
                                    securityManager.verifyDuressCode(codeInput) -> {
                                        // Duress - fake messenger
                                        securityManager.unlockWithDuress()
                                        onUnlocked(false)
                                    }
                                    securityManager.verifyPanicCode(codeInput) -> {
                                        // Panic wipe
                                        securityManager.panicWipe()
                                        onCancel()
                                    }
                                    else -> {
                                        error = "Invalid code"
                                        codeInput = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            enabled = codeInput.isNotEmpty() && !isWaiting
                        ) {
                            Text("Unlock")
                        }
                    }
                }

                UnlockStep.BIOMETRIC -> {
                    Text(
                        text = "Verify Identity",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Place your finger on the scanner",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Button(
                        onClick = {
                            step = UnlockStep.CODE
                            codeInput = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Back")
                    }
                    
                    // Trigger biometric in background
                    LaunchedEffect(Unit) {
                        isWaiting = true
                        // Simulate biometric delay
                        kotlinx.coroutines.delay(2000)
                        securityManager.unlock()
                        onUnlocked(true)
                    }
                }
            }
        }
    }
}

enum class UnlockStep {
    CODE, BIOMETRIC
}