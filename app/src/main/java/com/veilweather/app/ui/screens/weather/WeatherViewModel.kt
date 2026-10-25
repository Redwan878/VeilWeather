package com.veilweather.app.ui.screens.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veilweather.app.data.model.*
import com.veilweather.app.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeatherUiState(
    val isLoading: Boolean = false,
    val currentWeather: CurrentWeather? = null,
    val hourlyForecast: List<HourlyWeather> = emptyList(),
    val dailyForecast: List<DailyWeather> = emptyList(),
    val savedLocations: List<SavedLocation> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<LocationResult> = emptyList(),
    val selectedLocation: SavedLocation? = null,
    val error: String? = null
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        loadSavedLocations()
        // Load default location on start
        viewModelScope.launch {
            loadWeatherForLocation(23.8103, 90.4125) // Dhaka default
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        if (query.length >= 2) {
            viewModelScope.launch {
                searchLocations(query)
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    private suspend fun searchLocations(query: String) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val results = weatherRepository.searchLocations(query)
            _uiState.update { it.copy(searchResults = results, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    fun selectLocation(location: LocationResult) {
        viewModelScope.launch {
            loadWeatherForLocation(location.latitude, location.longitude)
            saveLocation(location)
            _uiState.update { 
                it.copy(
                    searchQuery = "",
                    searchResults = emptyList(),
                    selectedLocation = SavedLocation(
                        name = location.name,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
            }
        }
    }

    private suspend fun loadWeatherForLocation(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val weather = weatherRepository.getWeather(latitude, longitude)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    currentWeather = weather.current,
                    hourlyForecast = weather.hourly,
                    dailyForecast = weather.daily
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    private fun loadSavedLocations() {
        viewModelScope.launch {
            val locations = weatherRepository.getSavedLocations()
            _uiState.update { it.copy(savedLocations = locations) }
        }
    }

    private fun saveLocation(location: LocationResult) {
        viewModelScope.launch {
            weatherRepository.saveLocation(
                SavedLocation(
                    name = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
            loadSavedLocations()
        }
    }

    fun selectSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            loadWeatherForLocation(location.latitude, location.longitude)
            _uiState.update { it.copy(selectedLocation = location) }
        }
    }

    fun deleteSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            weatherRepository.deleteLocation(location)
            loadSavedLocations()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}