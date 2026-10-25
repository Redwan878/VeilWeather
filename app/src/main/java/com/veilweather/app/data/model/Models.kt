package com.veilweather.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherData(
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val weatherCode: Int,
    val description: String,
    val uvIndex: Double,
    val visibility: Int,
    val timestamp: Long
)

@Serializable
data class HourlyWeather(
    val timestamp: Long,
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val precipitation: Double
)

@Serializable
data class DailyWeather(
    val timestamp: Long,
    val maxTemp: Double,
    val minTemp: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val precipitation: Double,
    val uvIndex: Double
)

@Serializable
data class LocationResult(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val admin1: String? = null
)

@Serializable
data class SavedLocation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis()
)

// Chat & Messaging Models
@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chatId: String,
    val senderId: String,
    val content: String,
    val encryptedContent: String = "",
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val expiresAt: Long? = null,
    val replyTo: String? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val mediaUrl: String? = null
)

enum class MessageType {
    TEXT, IMAGE, VOICE, VIDEO, LOCATION
}

@Serializable
data class Chat(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ChatType = ChatType.DIRECT,
    val participants: List<String> = emptyList(),
    val name: String? = null,
    val lastMessage: ChatMessage? = null,
    val disappearAfter: Long? = null, // milliseconds
    val createdAt: Long = System.currentTimeMillis(),
    val isPaired: Boolean = false,
    val isFake: Boolean = false // True for duress mode fake chats
)

enum class ChatType {
    DIRECT, GROUP
}

@Serializable
data class EncryptionKeys(
    val identityKeyPair: String = "", // Base64-encoded
    val signedPreKeyPair: String = "",
    val preKeys: List<String> = emptyList(),
    val oneTimePreKeys: List<String> = emptyList()
)

@Serializable
data class PairingInfo(
    val pairingCode: String,
    val remotePublicKey: String,
    val pairedAt: Long = System.currentTimeMillis(),
    val remoteUserId: String? = null
)