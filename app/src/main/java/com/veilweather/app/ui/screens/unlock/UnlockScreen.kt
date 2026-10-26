package com.veilweather.app.ui.screens.unlock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
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
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                UnlockStep.CODE -> {
                    var codeInput by remember { mutableStateOf("") }

                    UnlockCard(
                        icon = Icons.Default.Lock,
                        title = "Welcome back",
                        subtitle = "Enter your access code to continue"
                    ) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            label = { Text("Access code") },
                            singleLine = true,
                            isError = error.isNotEmpty(),
                            supportingText = {
                                if (error.isNotEmpty()) {
                                    Text(error)
                                }
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                isWaiting = true
                                scope.launch {
                                    val code = codeInput.trim()
                                    when {
                                        securityManager.verifySecretCode(code) -> {
                                            step = UnlockStep.FINGERPRINT
                                            error = ""
                                        }
                                        securityManager.verifyDuressCode(code) -> {
                                            onUnlocked(false)
                                        }
                                        securityManager.verifyPanicCode(code) -> {
                                            securityManager.panicWipe()
                                            onCancel()
                                        }
                                        else -> error = "That code is not valid"
                                    }
                                    isWaiting = false
                                }
                            },
                            enabled = !isWaiting && codeInput.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isWaiting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Continue")
                            }
                        }

                        TextButton(onClick = onCancel) {
                            Text("Cancel")
                        }
                    }
                }

                UnlockStep.FINGERPRINT -> {
                    UnlockCard(
                        icon = Icons.Default.Fingerprint,
                        title = "Verify your identity",
                        subtitle = "Use your fingerprint to unlock securely"
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    if (biometricManager?.isAvailable() == true) {
                                        securityManager.unlock()
                                        onUnlocked(true)
                                    } else {
                                        securityManager.unlock()
                                        onUnlocked(true)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Authenticate")
                        }

                        TextButton(onClick = { step = UnlockStep.CODE }) {
                            Text("Use a different code")
                        }
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

@Composable
private fun UnlockCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(28.dp))
                content()
            }
        )
    }
}
