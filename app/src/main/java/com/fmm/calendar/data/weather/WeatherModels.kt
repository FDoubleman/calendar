package com.fmm.calendar.data.weather

data class WeatherSnapshot(
    val location: WeatherLocation,
    val current: CurrentWeather,
    val daily: List<DailyWeather>,
    val hourly: List<HourlyWeather>,
    val indices: List<WeatherIndex>,
    val warnings: List<WeatherWarning>,
    val yesterday: DailyWeather?,
    val yesterdayUnavailableReason: String?,
)

data class WeatherLocation(
    val id: String,
    val name: String,
    val district: String,
    val city: String,
    val province: String,
) {
    val displayName: String
        get() = listOf(province, city, district.ifBlank { name })
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
}

data class CurrentWeather(
    val observedAt: String,
    val temp: String,
    val feelsLike: String,
    val text: String,
    val icon: String,
    val windDir: String,
    val windScale: String,
    val humidity: String,
    val precip: String,
)

data class DailyWeather(
    val date: String,
    val tempMax: String,
    val tempMin: String,
    val textDay: String,
    val textNight: String,
    val iconDay: String,
    val windDirDay: String,
    val windScaleDay: String,
    val humidity: String,
    val precip: String,
)

data class HourlyWeather(
    val time: String,
    val temp: String,
    val text: String,
    val icon: String,
    val windScale: String,
    val pop: String,
)

data class WeatherIndex(
    val name: String,
    val category: String,
    val text: String,
)

data class WeatherWarning(
    val title: String,
    val severity: String,
    val typeName: String,
    val text: String,
)

sealed interface WeatherLoadResult {
    data class Success(val snapshot: WeatherSnapshot) : WeatherLoadResult
    data class Failure(val message: String) : WeatherLoadResult
}
