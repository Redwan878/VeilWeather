package com.veilweather.app.ui.screens.messenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veilweather.app.data.model.Chat
import com.veilweather.app.data.model.ChatType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen(
    onChatSelected: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    onPairingCode: (String) -> Boolean
) {
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewChat by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChat = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New chat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search/Pairing Bar (Secret code "185423" triggers pairing)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newValue ->
                    searchQuery = newValue
                    if (newValue == "185423") {
                        // Pairing code entered
                        if (onPairingCode(newValue)) {
                            searchQuery = ""
                        }
                    } else if (newValue == "wipe") {
                        // Remote wipe
                        searchQuery = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search or enter pairing code...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            // Chat List
            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Mail,
                            contentDescription = "No chats",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No messages yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(chats) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onChatSelected(chat.id) }
                        )
                    }
                }
            }
        }
    }

    // New Chat Dialog
    if (showNewChat) {
        AlertDialog(
            onDismissRequest = { showNewChat = false },
            title = { Text("Start New Chat") },
            text = {
                Column {
                    Text("Direct Message")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Enter user ID or pairing code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChat = false }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChat = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge
                )
                chat.lastMessage?.let {
                    Text(
                        text = it.content.take(50),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (chat.type == ChatType.GROUP) {
                Icon(Icons.Default.Group, contentDescription = "Group chat")
            }
        }
    }
}