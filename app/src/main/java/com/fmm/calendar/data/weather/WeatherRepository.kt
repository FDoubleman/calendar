package com.fmm.calendar.data.weather

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.fmm.calendar.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.resume

class WeatherRepository(
    private val context: Context,
    private val api: QWeatherApi = QWeatherApi(
        apiHost = BuildConfig.QWEATHER_API_HOST,
        apiKey = BuildConfig.QWEATHER_API_KEY,
    ),
) {
    suspend fun loadCurrentLocationWeather(): WeatherLoadResult {
        return runCatching {
            val location = getLastKnownLocation()
                ?: return WeatherLoadResult.Failure("无法获取当前位置，请确认定位服务已开启后重试")
            loadWeather(location.longitude, location.latitude)
        }.getOrElse { throwable ->
            WeatherLoadResult.Failure(throwable.userMessage())
        }
    }

    private suspend fun loadWeather(longitude: Double, latitude: Double): WeatherLoadResult = coroutineScope {
        val weatherLocation = api.lookupLocation(longitude, latitude)
        val current = async { api.currentWeather(weatherLocation.id) }
        val daily = async { api.dailyWeather(weatherLocation.id, 15) }
        val hourly = async { runCatching { api.hourlyWeather(weatherLocation.id) }.getOrDefault(emptyList()) }
        val indices = async { runCatching { api.indices(weatherLocation.id) }.getOrDefault(emptyList()) }
        val warnings = async { runCatching { api.warnings(weatherLocation.id) }.getOrDefault(emptyList()) }
        val yesterdayResult = async {
            runCatching { api.historicalWeather(weatherLocation.id, LocalDate.now().minusDays(1)) }
        }

        val yesterday = yesterdayResult.await()
        WeatherLoadResult.Success(
            WeatherSnapshot(
                location = weatherLocation,
                current = current.await(),
                daily = daily.await(),
                hourly = hourly.await(),
                indices = indices.await(),
                warnings = warnings.await(),
                yesterday = yesterday.getOrNull(),
                yesterdayUnavailableReason = yesterday.exceptionOrNull()?.userMessage(),
            ),
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? = withContext(Dispatchers.IO) {
        val locationManager = context.getSystemService<LocationManager>() ?: return@withContext null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        providers
            .mapNotNull { provider ->
                runCatching {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.getLastKnownLocation(provider)
                    } else {
                        null
                    }
                }.getOrNull()
            }
            .maxByOrNull { it.time }
            ?: providers.firstNotNullOfOrNull { provider ->
                runCatching {
                    if (locationManager.isProviderEnabled(provider)) {
                        requestSingleLocation(locationManager, provider)
                    } else {
                        null
                    }
                }.getOrNull()
            }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun requestSingleLocation(
        locationManager: LocationManager,
        provider: String,
    ): Location? = suspendCancellableCoroutine { continuation ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            locationManager.getCurrentLocation(
                provider,
                signal,
                ContextCompat.getMainExecutor(context),
            ) { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
            continuation.invokeOnCancellation { signal.cancel() }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

                @Deprecated("Required by the legacy LocationListener API.")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }

            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
        }
    }
}

private fun Throwable.userMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: "天气数据加载失败，请稍后再试"
}
