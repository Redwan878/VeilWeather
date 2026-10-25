# VeilWeather - Disguised Secure Messenger

## Project Overview

**Project Name:** VeilWeather
**Type:** Android Application (Kotlin + Jetpack Compose)
**Core Functionality:** A fully functional weather app that hides a highly secure, end-to-end encrypted messenger behind a secret unlock sequence.

## Technology Stack & Choices

### Framework & Language
- **Language:** Kotlin 1.9.x
- **UI Framework:** Jetpack Compose with Material 3
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)

### Key Libraries
- **Weather API:** Open-Meteo (free, no API key required)
- **Networking:** Retrofit2 + OkHttp4 + Kotlin Coroutines
- **Serialization:** Kotlinx Serialization (JSON + Protobuf)
- **Encryption:** Signal Protocol implementation (libsignal-android)
- **Biometrics:** AndroidX Biometric
- **Security:** AndroidX Security Crypto (EncryptedSharedPreferences), Android Keystore
- **WebSocket:** OkHttp WebSocket for real-time messaging
- **WebRTC:** Google WebRTC for calls
- **Media:** Coil for images, Opus codec for voice
- **DI:** Hilt for dependency injection

### Architecture Pattern
- **Clean Architecture:** Presentation (UI) → Domain (Use Cases) → Data (Repository)
- **MVVM:** ViewModels with StateFlow
- **Single Activity:** Navigation Compose

## Feature List

### Disguise Layer (Layer 1)
- [x] Fully functional weather app with Open-Meteo API
- [x] Location search with autocomplete
- [x] Current conditions display (temp, humidity, wind, etc.)
- [x] Hourly forecast (24 hours)
- [x] 7-day forecast
- [x] Saved locations management
- [x] Weather animations (Material You)
- [x] Secret code "8546" triggers unlock flow (no visible hint)
- [x] All codes stored as salted hashes (PBKDF2)

### Unlock Flow (Layer 2)
- [x] Secret code → biometric (fingerprint) → full messenger
- [x] Duress code "1111" → fake empty messenger
- [x] Panic wipe codes: "0000" or shake gesture
- [x] Re-require biometric after app close/reopen

### Instant Lockdown
- [x] On close/screen-off/app-switch: wipe in-memory chat state
- [x] FLAG_SECURE to block screenshots/recording
- [x] Messages in RAM only (no persistence)
- [x] Keys held in Android Keystore

### Pairing & Stealth
- [x] Decoy search bar in messenger accepts pairing code "185423"
- [x] Disguised notifications ("check app") until first message
- [x] Anonymous accounts (no phone/email required)

### Messaging Features
- [x] Signal Protocol E2E encryption (X3DH + Double Ratchet)
- [x] Text messages
- [x] Image messages (compressed WebP)
- [x] Voice notes with waveform
- [x] Quick reactions
- [x] Reply threading
- [x] Group chats (sender-key distribution)
- [x] Disappearing messages: 30s, 60s, 1h, 3h, 9h, 12h, 24h
- [x] Read-once media (vanish after single view)
- [x] Remote wipe via "wipe" in search bar

### Metadata Protection
- [x] Padded message sizes
- [x] Random delivery jitter
- [x] No "last seen", typing history, read history on server
- [x] Auto-expiring undelivered ciphertext queues

### Calls
- [x] WebRTC voice/video calls
- [x] DTLS-SRTP encryption
- [x] STUN/TURN relay support
- [x] Screen sharing
- [x] Adaptive bitrate
- [x] Audio-only low-data mode
- [x] Disguised ring notifications

### Performance
- [x] Single persistent WebSocket with Protobuf
- [x] FCM data-only pushes for wake-up
- [x] Media compression before encryption
- [x] R8/ProGuard shrinking
- [x] Offline queue with auto-retry
- [x] Target: <300ms message delivery

### Backend (Minimal Relay)
- [x] WebSocket relay for ciphertext
- [x] FCM push notification service
- [x] Rate limiting
- [x] Auto-expiring queues

## UI/UX Design Direction

### Visual Style
- Material Design 3 with dynamic color (Material You)
- Clean, modern weather app aesthetic
- Smooth weather animations

### Color Scheme
- Dynamic colors based on weather conditions
- Primary: Blue tones (weather-appropriate)
- Dark mode support

### Layout Approach
- Single Activity with Compose Navigation
- Bottom navigation in weather mode (Home, Forecast, Settings)
- Full-screen messenger after unlock
- Minimal UI footprint for security

### Disguise Elements
- App name: "VeilWeather"
- App icon: Weather icon (sun/clouds)
- All notifications look like weather notifications
- Recents screen shows weather app