package com.veilweather.app.ui.screens.messenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message)
            }
        }
    }

    // Disappearing Timer Dialog
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
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach")
            }
            
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                placeholder = { Text("Message") },
                singleLine = false,
                maxLines = 4
            )

            if (value.isBlank()) {
                IconButton(onClick = onVoiceNote) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice note")
                }
            } else {
                IconButton(onClick = onSend) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
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
            .padding(vertical = 4.dp),
        contentAlignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isSent) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSent) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    MessageType.IMAGE -> {
                        // Image placeholder
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Image",
                            modifier = Modifier.size(200.dp)
                        )
                    }
                    MessageType.VOICE -> {
                        // Voice note waveform placeholder
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voice note")
                        }
                    }
                    else -> {
                        Text("Unsupported message type")
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSent) 
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    // Disappearing timer indicator
                    message.expiresAt?.let {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Disappearing",
                            modifier = Modifier.size(12.dp),
                            tint = if (isSent) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    // Read indicator
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
        title = { Text("Disappearing Messages") },
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
                            Text(label)
                            if (timer == currentTimer) {
                                Icon(Icons.Default.Check, "Selected")
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