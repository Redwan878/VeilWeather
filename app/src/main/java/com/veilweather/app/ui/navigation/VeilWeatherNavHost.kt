package com.veilweather.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.veilweather.app.ui.screens.weather.WeatherScreen
import com.veilweather.app.ui.screens.unlock.UnlockScreen
import com.veilweather.app.ui.screens.messenger.MessengerScreen
import com.veilweather.app.ui.screens.messenger.ChatScreen
import com.veilweather.app.ui.screens.messenger.SettingsScreen
import com.veilweather.app.security.SecurityManager
import androidx.compose.runtime.Composable

sealed class Screen(val route: String) {
    object Weather : Screen("weather")
    object Unlock : Screen("unlock?skipCode={skipCode}") {
        fun createRoute(skipCode: Boolean = false) = "unlock?skipCode=$skipCode"
    }
    object Messenger : Screen("messenger")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object Settings : Screen("settings")
}

@Composable
fun VeilWeatherNavHost() {
    val navController = rememberNavController()
    val securityManager = remember { SecurityManager.getInstance() }
    
    var currentUnlockState by remember { mutableStateOf(securityManager.getUnlockState()) }
    
    // Update state when security manager changes
    LaunchedEffect(Unit) {
        securityManager.unlockStateFlow.collect { state ->
            currentUnlockState = state
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Weather.route
    ) {
        // Weather Screen (always accessible - the disguise)
        composable(Screen.Weather.route) {
            WeatherScreen(
                onSecretCodeEntered = { code ->
                    if (code == "8546") {
                        // Secret code triggers unlock flow - skip code entry, go to fingerprint
                        navController.navigate(Screen.Unlock.route + "?skipCode=true")
                    } else if (code == "0000") {
                        // Panic wipe
                        securityManager.panicWipe()
                    } else if (code == "wipe") {
                        // Remote wipe
                        securityManager.remoteWipe()
                    } else if (code == "1111") {
                        // Duress code - open fake messenger
                        securityManager.unlockWithDuress()
                        navController.navigate(Screen.Messenger.route)
                    }
                }
            )
        }

        // Unlock Screen
        composable(
            route = Screen.Unlock.route,
            arguments = listOf(
                navArgument("skipCode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val skipCode = backStackEntry.arguments?.getBoolean("skipCode") ?: false
            UnlockScreen(
                skipCodeEntry = skipCode,
                onUnlocked = { isRealMessenger ->
                    if (isRealMessenger) {
                        navController.navigate(Screen.Messenger.route) {
                            popUpTo(Screen.Weather.route)
                        }
                    } else {
                        // Duress - go to fake messenger
                        navController.navigate(Screen.Messenger.route) {
                            popUpTo(Screen.Weather.route)
                        }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // Messenger Screen
        composable(Screen.Messenger.route) {
            MessengerScreen(
                onChatSelected = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onBack = {
                    securityManager.lock()
                    navController.popBackStack(Screen.Weather.route, inclusive = false)
                },
                onPairingCode = { code ->
                    if (code == "185423") {
                        // Start pairing process
                        true
                    } else {
                        false
                    }
                }
            )
        }

        // Chat Screen
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() },
                onVoiceCall = { /* WebRTC call */ },
                onVideoCall = { /* WebRTC video call */ }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onChangeSecretCode = { /* Change code */ },
                onChangeDuressCode = { /* Change duress code */ },
                onWipeAll = { securityManager.panicWipe() }
            )
        }
    }
}