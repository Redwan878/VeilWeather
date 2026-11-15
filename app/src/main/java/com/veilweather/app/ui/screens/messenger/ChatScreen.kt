package com.veilweather.app.ui.screens.messenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veilweather.app.data.model.ChatMessage
import com.veilweather.app.data.model.MessageType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var showDisappearingTimer by remember { mutableStateOf(false) }
    var selectedTimer by remember { mutableStateOf(60000L) } // Default 60s
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onVoiceCall) {
                        Icon(Icons.Default.Phone, contentDescription = "Voice call")
                    }
                    IconButton(onClick = onVideoCall) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video call")
                    }
                    IconButton(onClick = { showDisappearingTimer = true }) {
                        Icon(Icons.Default.Timer, contentDescription = "Disappearing timer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            MessageInputBar(
                value = messageInput,
                onValueChange = { messageInput = it },
                onSend = {
                    if (messageInput.isNotBlank()) {
                        val newMessage = ChatMessage(
                            chatId = chatId,
                            senderId = "me",
                            content = messageInput,
                            type = MessageType.TEXT,
                            timestamp = System.currentTimeMillis(),
                            expiresAt = System.currentTimeMillis() + selectedTimer
                        )
                        messages = messages + newMessage
                        messageInput = ""
                    }
                },
                onAttach = { /* Handle attachment */ },
                onVoiceNote = { /* Record voice note */ }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message)
            }
        }
    }

    if (showDisappearingTimer) {
        DisappearingTimerDialog(
            currentTimer = selectedTimer,
            onTimerSelected = { timer ->
                selectedTimer = timer
                showDisappearingTimer = false
            },
            onDismiss = { showDisappearingTimer = false }
        )
    }
}

@Composable
fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onVoiceNote: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttach) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(22.dp),
                singleLine = false,
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(4.dp))

            FilledIconButton(
                onClick = if (value.isBlank()) onVoiceNote else onSend
            ) {
                Icon(
                    if (value.isBlank()) Icons.Default.Mic else Icons.Default.Send,
                    contentDescription = if (value.isBlank()) "Voice note" else "Send"
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isSent = message.senderId == "me"
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        contentAlignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = if (isSent) {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 4.dp, bottomStart = 18.dp)
            } else {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
            },
            color = if (isSent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSent) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    MessageType.IMAGE -> {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Image",
                            modifier = Modifier.size(200.dp),
                            tint = if (isSent) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    MessageType.VOICE -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isSent) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Voice note",
                                color = if (isSent) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    else -> {
                        Text(
                            "Unsupported message type",
                            color = if (isSent) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSent) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        }
                    )

                    message.expiresAt?.let {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Disappearing",
                            modifier = Modifier.size(12.dp),
                            tint = if (isSent) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    }

                    if (isSent && message.isRead) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Read",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DisappearingTimerDialog(
    currentTimer: Long,
    onTimerSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val timers = listOf(
        30000L to "30 seconds",
        60000L to "1 minute",
        3600000L to "1 hour",
        10800000L to "3 hours",
        32400000L to "9 hours",
        43200000L to "12 hours",
        86400000L to "24 hours"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Text("Disappearing Messages")
            }
        },
        text = {
            Column {
                timers.forEach { (timer, label) ->
                    TextButton(
                        onClick = { onTimerSelected(timer) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                label,
                                color = if (timer == currentTimer) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (timer == currentTimer) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
