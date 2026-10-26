package com.veilweather.app.data.network

import com.veilweather.app.BuildConfig

object BackendConfig {
    val baseUrl: String
        get() = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

    val healthUrl: String
        get() = "$baseUrl/healthz"

    val webSocketUrl: String
        get() = baseUrl.replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") + "/ws"
}
