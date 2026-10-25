package com.veilweather.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VeilWeatherApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}