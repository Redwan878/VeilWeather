package com.veilweather.app.ui.screens.messenger

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veilweather.app.security.SecurityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangeSecretCode: (String) -> Unit,
    onChangeDuressCode: (String) -> Unit,
    onWipeAll: () -> Unit,
    securityManager: SecurityManager = SecurityManager.getInstance()
) {
    var secretCodeInput by remember { mutableStateOf("") }
    var duressCodeInput by remember { mutableStateOf("") }
    var confirmWipe by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Security Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Secret Code
            OutlinedTextField(
                value = secretCodeInput,
                onValueChange = { secretCodeInput = it },
                label = { Text("New Secret Code") },
                placeholder = { Text("Enter new code") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (secretCodeInput.length >= 4) {
                        onChangeSecretCode(secretCodeInput)
                        secretCodeInput = ""
                    }
                },
                enabled = secretCodeInput.length >= 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Secret Code")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Duress Code
            OutlinedTextField(
                value = duressCodeInput,
                onValueChange = { duressCodeInput = it },
                label = { Text("New Duress Code") },
                placeholder = { Text("Enter new duress code") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (duressCodeInput.length >= 4) {
                        onChangeDuressCode(duressCodeInput)
                        duressCodeInput = ""
                    }
                },
                enabled = duressCodeInput.length >= 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Duress Code")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Zone
            Text("Danger Zone", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { confirmWipe = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    borderColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Panic Wipe",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Panic Wipe")
                }
            },
            text = {
                Text("This will erase all encryption keys, chats, and media. Are you sure?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onWipeAll()
                        confirmWipe = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, wipe everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}