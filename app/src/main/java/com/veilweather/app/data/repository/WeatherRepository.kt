package com.veilweather.app.data.repository

import com.veilweather.app.data.model.*
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor() {
    
    private val savedLocations = mutableListOf<SavedLocation>()

    suspend fun getWeather(latitude: Double, longitude: Double): WeatherData {
        // Simulated weather data for now
        // In production, this would call Open-Meteo API
        delay(500) // Simulate network delay
        
        return WeatherData(
            current = CurrentWeather(
                temperature = 28.5,
                feelsLike = 30.2,
                humidity = 65,
                pressure = 1013,
                windSpeed = 8.5,
                windDirection = 180,
                weatherCode = 1,
                description = "Mostly Clear",
                uvIndex = 6.5,
                visibility = 10000,
                timestamp = System.currentTimeMillis()
            ),
            hourly = (0..23).map { hour ->
                HourlyWeather(
                    timestamp = System.currentTimeMillis() + (hour * 3600 * 1000),
                    temperature = 25.0 + (hour % 10),
                    weatherCode = 1,
                    windSpeed = 5.0 + (hour % 5),
                    precipitation = 0.0
                )
            },
            daily = (0..6).map { day ->
                DailyWeather(
                    timestamp = System.currentTimeMillis() + (day * 86400 * 1000),
                    maxTemp = 30.0 + (day % 3),
                    minTemp = 20.0 + (day % 3),
                    weatherCode = 1,
                    windSpeed = 8.0,
                    precipitation = 0.0,
                    uvIndex = 6.5
                )
            }
        )
    }

    suspend fun searchLocations(query: String): List<LocationResult> {
        delay(300)
        
        return listOf(
            LocationResult(
                name = "Dhaka",
                country = "Bangladesh",
                latitude = 23.8103,
                longitude = 90.4125,
                admin1 = "Dhaka"
            ),
            LocationResult(
                name = "Chittagong",
                country = "Bangladesh",
                latitude = 22.3569,
                longitude = 91.7832,
                admin1 = "Chittagong"
            )
        ).filter { it.name.contains(query, ignoreCase = true) }
    }

    fun getSavedLocations(): List<SavedLocation> {
        return savedLocations
    }

    fun saveLocation(location: SavedLocation) {
        if (!savedLocations.any { it.id == location.id }) {
            savedLocations.add(location)
        }
    }

    fun deleteLocation(location: SavedLocation) {
        savedLocations.removeAll { it.id == location.id }
    }
}