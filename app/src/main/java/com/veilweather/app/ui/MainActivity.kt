package com.veilweather.app.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.veilweather.app.ui.navigation.VeilWeatherNavHost
import com.veilweather.app.ui.theme.VeilWeatherTheme
import com.veilweather.app.security.SecurityManager
import com.veilweather.app.data.repository.MessengerStateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var messengerStateRepository: MessengerStateRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // FLAG_SECURE: Block screenshots, screen recording, recents
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            VeilWeatherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VeilWeatherNavHost()
                }
            }
        }

        // Handle lifecycle for secure lockdown
        setupLifecycleObservers()
    }

    private fun setupLifecycleObservers() {
        lifecycleScope.launch {
            // Wipe chat state when app goes to background
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // App is visible - do nothing extra
            }
        }
        
        lifecycleScope.launch {
            // Additional lockdown when paused (app in background)
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                if (!securityManager.isUnlocked()) {
                    messengerStateRepository.clearSensitiveData()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Lock immediately on pause
        if (securityManager.isUnlocked()) {
            securityManager.lock()
        }
    }

    override fun onStop() {
        super.onStop()
        // Ensure full lockdown
        messengerStateRepository.lockdown()
    }
}