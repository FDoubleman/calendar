package com.fmm.calendar.ui.weather

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmm.calendar.data.weather.WeatherLoadResult
import com.fmm.calendar.data.weather.WeatherRepository
import com.fmm.calendar.data.weather.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            refresh()
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "需要定位权限后才能获取当前位置天气",
                )
            }
        }
    }

    fun refresh() {
        if (!hasLocationPermission()) {
            _uiState.update {
                it.copy(
                    hasLocationPermission = false,
                    isLoading = false,
                    errorMessage = "需要定位权限后才能获取当前位置天气",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    hasLocationPermission = true,
                    isLoading = true,
                    errorMessage = null,
                )
            }
            when (val result = repository.loadCurrentLocationWeather()) {
                is WeatherLoadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snapshot = result.snapshot,
                            errorMessage = null,
                        )
                    }
                }
                is WeatherLoadResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}

data class WeatherUiState(
    val isLoading: Boolean = false,
    val hasLocationPermission: Boolean? = null,
    val snapshot: WeatherSnapshot? = null,
    val errorMessage: String? = null,
)
