package com.veilweather.app.ui.screens.weather

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    onSecretCodeEntered: (String) -> Unit,
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VeilWeather") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar (Secret Code Entry Point)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newValue ->
                    searchQuery = newValue
                    viewModel.onSearchQueryChanged(newValue)
                    
                    // Check for secret codes
                    when (newValue) {
                        "8546" -> {
                            onSecretCodeEntered(newValue)
                            searchQuery = ""
                        }
                        "1111" -> {
                            onSecretCodeEntered(newValue)
                            searchQuery = ""
                        }
                        "0000" -> {
                            onSecretCodeEntered(newValue)
                            searchQuery = ""
                        }
                        "wipe" -> {
                            onSecretCodeEntered(newValue)
                            searchQuery = ""
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search location...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            // Search Results
            if (uiState.searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    items(uiState.searchResults) { location ->
                        TextButton(
                            onClick = { viewModel.selectLocation(location) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${location.name}, ${location.country}",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }

            // Current Weather Display
            uiState.currentWeather?.let { weather ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${weather.temperature.toInt()}°",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = weather.description,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Weather details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            WeatherDetail("Feels", "${weather.feelsLike.toInt()}°")
                            WeatherDetail("Humidity", "${weather.humidity}%")
                            WeatherDetail("Wind", "${weather.windSpeed.toInt()} km/h")
                        }
                    }
                }
            }

            // Hourly Forecast
            if (uiState.hourlyForecast.isNotEmpty()) {
                Text(
                    text = "Hourly Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(uiState.hourlyForecast.take(24)) { hourly ->
                        HourlyWeatherCard(hourly)
                    }
                }
            }

            // Daily Forecast
            if (uiState.dailyForecast.isNotEmpty()) {
                Text(
                    text = "7-Day Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(uiState.dailyForecast) { daily ->
                        DailyWeatherCard(daily)
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun WeatherDetail(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HourlyWeatherCard(hourly: com.veilweather.app.data.model.HourlyWeather) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(hourly.timestamp))
    
    Card(
        modifier = Modifier
            .width(80.dp)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(time, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${hourly.temperature.toInt()}°",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun DailyWeatherCard(daily: com.veilweather.app.data.model.DailyWeather) {
    val day = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(daily.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(day, style = MaterialTheme.typography.bodyLarge)
            Row {
                Text(
                    "${daily.maxTemp.toInt()}°",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    " / ${daily.minTemp.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}