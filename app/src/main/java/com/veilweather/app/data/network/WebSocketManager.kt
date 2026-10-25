package com.veilweather.app.data.network

import okhttp3.*
import okio.ByteString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Serializable
data class WebSocketMessage(
    val type: String = "",
    val chatId: String = "",
    val content: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L,
    val messageId: String = "",
    val readAt: Long = 0L,
    val isTyping: Boolean = false,
    val sdp: String = "",
    val ice: String = "",
    val userId: String = "",
    val token: String = ""
)

@Singleton
class WebSocketManager @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val TAG = "WebSocketManager"
    private val json = Json { ignoreUnknownKeys = true }
    
    private var webSocket: WebSocket? = null
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _incomingMessages = MutableStateFlow<List<Any>>(emptyList())
    val incomingMessages: StateFlow<List<Any>> = _incomingMessages
    
    private val _offlineQueue = mutableListOf<WebSocketMessage>()
    
    fun connect(serverUrl: String, userId: String, token: String) {
        if (webSocket != null) {
            return
        }
        
        val request = Request.Builder()
            .url(serverUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        webSocket = httpClient.newWebSocket(request, MyWebSocketListener(userId))
        _connectionState.value = ConnectionState.CONNECTING
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        // On disconnect, all in-memory messages are wiped
        _incomingMessages.value = emptyList()
    }
    
    fun sendMessage(message: WebSocketMessage) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            try {
                val json = json.encodeToString(WebSocketMessage.serializer(), message)
                webSocket?.send(json)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                _offlineQueue.add(message)
            }
        } else {
            // Queue for later
            _offlineQueue.add(message)
        }
    }
    
    private fun flushOfflineQueue() {
        val queue = _offlineQueue.toList()
        _offlineQueue.clear()
        queue.forEach { message ->
            sendMessage(message)
        }
    }
    
    private inner class MyWebSocketListener(private val userId: String) : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            _connectionState.value = ConnectionState.CONNECTED
            flushOfflineQueue()
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                // Parse and handle message
                Log.d(TAG, "Received: $text")
                val messages = _incomingMessages.value.toMutableList()
                messages.add(text)
                _incomingMessages.value = messages
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse message", e)
            }
        }
        
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(TAG, "Received binary message")
        }
        
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $reason")
            webSocket.close(1000, "OK")
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $reason")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure", t)
            _connectionState.value = ConnectionState.FAILED
            // Attempt reconnection with exponential backoff
        }
    }
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}