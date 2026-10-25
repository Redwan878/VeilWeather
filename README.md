# VeilWeather - Disguised Secure Messenger

A production-grade Android application that functions as a fully operational weather app while hiding a highly secure, end-to-end encrypted messenger behind a multi-layer unlock system.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer (UI)                  │
│  Weather Screen │ Unlock Flow │ Messenger │ Calls & Settings │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────┴────────────────────────────────────┐
│                  Domain Layer (Use Cases)                   │
│  Security Manager │ Biometric │ E2E Encryption │ WebSocket  │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────┴────────────────────────────────────┐
│                   Data Layer (Repository)                   │
│  Weather API │ Messenger State │ Encrypted Storage │ Network │
└─────────────────────────────────────────────────────────────┘
```

## Key Features

### 1. Disguise Layer (Weather App)
- ✅ Real weather data from Open-Meteo API
- ✅ Location search with autocomplete
- ✅ Current conditions, hourly, and 7-day forecasts
- ✅ Saved locations management
- ✅ Material Design 3 UI

### 2. Secret Code Unlock (8546)
- ✅ PBKDF2-SHA256 hash with salt storage
- ✅ No visible hints or transitions
- ✅ Biometric (fingerprint) as second factor
- ✅ Re-require biometric on app reopen

### 3. Duress Code (1111)
- ✅ Opens fake messenger with empty chats
- ✅ No access to real conversations
- ✅ Behaves identically to real messenger

### 4. Panic Wipe
- ✅ Code "0000" or accelerometer shake gesture
- ✅ Instantly erases all keys, chats, media
- ✅ Reverts to plain weather app

### 5. Instant Lockdown
- ✅ FLAG_SECURE blocks screenshots/recording
- ✅ On-close/pause: wipe in-memory state
- ✅ Messages live in RAM only
- ✅ Keys in Android Keystore

### 6. E2E Encryption
- ✅ Signal Protocol (X3DH + Double Ratchet)
- ✅ AES-256-GCM for message encryption
- ✅ Forward secrecy
- ✅ Perfect forward secrecy on key rotation

### 7. Messaging Features
- ✅ Text, images, voice notes
- ✅ Quick reactions
- ✅ Reply threading
- ✅ Group chats with sender-key
- ✅ Disappearing messages (30s - 24h)
- ✅ Read-once media (photos/voice vanish after view)
- ✅ Remote wipe via "wipe" command

### 8. Pairing & Stealth
- ✅ Pairing code "185423" in messenger search
- ✅ Disguised notifications until first message
- ✅ Anonymous accounts (no phone/email)
- ✅ Other device unlocks only on first incoming message

### 9. WebRTC Calls
- ✅ Voice and video calls
- ✅ Screen sharing
- ✅ DTLS-SRTP encryption
- ✅ STUN/TURN relay
- ✅ Adaptive bitrate
- ✅ Audio-only mode for low bandwidth

### 10. Performance
- ✅ Single persistent WebSocket
- ✅ Protobuf message serialization
- ✅ FCM data-only pushes
- ✅ Media compression (WebP/Opus)
- ✅ Offline queue with auto-retry
- ✅ Target: <300ms delivery

## Project Structure

```
VeilWeather/
├── app/
│   ├── src/main/
│   │   ├── java/com/veilweather/app/
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── weather/           # Weather app UI
│   │   │   │   │   ├── unlock/            # Secret code + biometric
│   │   │   │   │   └── messenger/         # Chat, calls, settings
│   │   │   │   ├── navigation/            # Compose Nav Graph
│   │   │   │   └── theme/                 # Material 3 design
│   │   │   ├── data/
│   │   │   │   ├── model/                 # Data classes
│   │   │   │   ├── repository/            # Business logic
│   │   │   │   ├── network/               # WebSocket + HTTP
│   │   │   │   └── service/               # Foreground services
│   │   │   ├── security/
│   │   │   │   ├── SecurityManager.kt     # Code hash + unlock state
│   │   │   │   ├── BiometricManager.kt    # Fingerprint auth
│   │   │   │   └── encryption/            # Signal Protocol
│   │   │   └── di/                        # Hilt DI setup
│   │   └── res/
│   │       ├── values/
│   │       │   ├── strings.xml
│   │       │   ├── colors.xml
│   │       │   └── themes.xml
│   │       └── xml/
│   │           └── data_extraction_rules.xml
│   ├── build.gradle                       # Dependencies
│   └── AndroidManifest.xml
├── .github/
│   └── workflows/
│       └── build.yml                      # GitHub Actions CI/CD
├── build.gradle                           # Root build config
├── settings.gradle                        # Module config
└── README.md
```

## Security Considerations

### Threat Model

| Threat | Mitigation |
|--------|-----------|
| Screenshot/Screen Recording | FLAG_SECURE prevents all captures |
| Recents Exposure | App name/icon look like weather |
| Notification Spoofing | Disguised as "check app" |
| On-Device Persistence | Messages in RAM only |
| Key Compromise | Android Keystore (TEE/SE) |
| Code Brute-force | PBKDF2 120k iterations + salt |
| Metadata Leakage | Padded messages + jitter |
| Network Interception | TLS 1.3 + Protobuf encryption |

### Privacy-First Design

- **No Analytics:** Zero telemetry SDKs
- **No Tracking:** Anonymous user IDs
- **No Metadata:** Server doesn't store last-seen, typing, read history
- **No Cache:** Undelivered messages auto-delete after TTL
- **No Logs:** Production builds strip all debug info
- **Minimal Dependencies:** <20 libraries, all auditable

## Tech Stack

- **Language:** Kotlin 1.9.x
- **Framework:** Jetpack Compose + Material 3
- **Min SDK:** 26 (Android 8.0)
- **DI:** Hilt
- **Networking:** OkHttp + WebSocket
- **Encryption:** Signal Protocol (X3DH + Double Ratchet)
- **Storage:** Android Keystore + Encrypted SharedPrefs
- **Biometric:** AndroidX Biometric
- **Build:** Gradle 8.x with R8 shrinking

## Building & Deployment

### GitHub Actions CI/CD

```bash
# Push to main branch - automatically builds APK
git push origin main

# Download signed APK from workflow artifacts
```

### Local Build (requires Android SDK)

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

### APK Installation

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Backend Infrastructure

### Relay Server (Minimal)
- WebSocket for ciphertext relay
- FCM push notifications
- Rate limiting (100 msgs/min per user)
- Auto-expiring queues (24hr TTL)
- No user identification or tracking

### Database
- Redis: Offline message queues
- PostgreSQL: Session metadata (optional)
- No plaintext storage

## Secret Codes

| Code | Action |
|------|--------|
| `8546` | Unlock real messenger (requires biometric) |
| `1111` | Unlock fake messenger (duress mode) |
| `0000` | Panic wipe (all data erased) |
| `185423` | Pairing code (in messenger search bar) |
| `wipe` | Remote wipe (in messenger search bar) |

## Customization

### Change Secret Code
Settings → Security Settings → Change Secret Code

### Change Duress Code
Settings → Security Settings → Change Duress Code

### Configure Disappearing Messages
Chat screen → Timer icon → Select duration (30s - 24h)

## Compliance

- ✅ GDPR: No data retention
- ✅ E2E: Full forward secrecy
- ✅ Authentication: Biometric + Code
- ✅ Transport: TLS 1.3
- ✅ Storage: Keystore encryption
- ⚠️ Jurisdictional: Use responsibly in your legal context

## Development Notes

### Version Management
- Current: 1.0.0
- Target: Production ready

### Known Limitations
- Duress mode is detectable by deep inspection
- Local forensic recovery possible with superuser access
- Backend anonymity depends on network provider

### Future Enhancements
- [ ] Decoy chat generation for duress mode
- [ ] On-device ML for threat detection
- [ ] Multi-signature authorization
- [ ] Hardware security key support
- [ ] Stealth mode (hidden app icon)

## License

Closed-source. Use at your own legal risk.

## Support

For issues, security concerns, or feature requests: [encrypted channel]