package com.veilweather.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.veilweather.app.data.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessengerStateRepository @Inject constructor() {
    
    private val _messagesInMemory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messagesInMemory: StateFlow<List<ChatMessage>> = _messagesInMemory

    private val _chatsInMemory = MutableStateFlow<Map<String, Any>>(emptyMap())
    val chatsInMemory: StateFlow<Map<String, Any>> = _chatsInMemory

    /**
     * Lockdown: wipe all in-memory chat state
     * Called on app close, screen off, or app switch
     */
    fun lockdown() {
        _messagesInMemory.value = emptyList()
        _chatsInMemory.value = emptyMap()
    }

    /**
     * Clear sensitive data from memory
     */
    fun clearSensitiveData() {
        _messagesInMemory.value = emptyList()
        _chatsInMemory.value = emptyMap()
    }

    /**
     * Add message to in-memory cache (not persisted to disk)
     */
    fun addMessageToMemory(message: ChatMessage) {
        val current = _messagesInMemory.value.toMutableList()
        current.add(message)
        _messagesInMemory.value = current
    }

    /**
     * Get all messages (from RAM only)
     */
    fun getMessagesFromMemory(): List<ChatMessage> {
        return _messagesInMemory.value
    }
}