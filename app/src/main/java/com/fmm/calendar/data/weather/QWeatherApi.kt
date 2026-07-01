package com.fmm.calendar.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class QWeatherApi(
    private val apiHost: String,
    private val apiKey: String,
    private val client: OkHttpClient = defaultClient,
) {
    suspend fun lookupLocation(longitude: Double, latitude: Double): WeatherLocation {
        val root = getJson(
            pathSegments = listOf("geo", "v2", "city", "lookup"),
            query = mapOf(
                "location" to "$longitude,$latitude",
                "range" to "cn",
                "number" to "1",
            ),
        )
        val first = root.getJSONArray("location").getJSONObject(0)
        return first.toWeatherLocation()
    }

    suspend fun currentWeather(locationId: String): CurrentWeather {
        return getJson(
            pathSegments = listOf("v7", "weather", "now"),
            query = mapOf("location" to locationId),
        ).getJSONObject("now").toCurrentWeather()
    }

    suspend fun dailyWeather(locationId: String, days: Int): List<DailyWeather> {
        return getJson(
            pathSegments = listOf("v7", "weather", "${days}d"),
            query = mapOf("location" to locationId),
        ).getJSONArray("daily").mapObjects { it.toDailyWeather() }
    }

    suspend fun hourlyWeather(locationId: String): List<HourlyWeather> {
        return getJson(
            pathSegments = listOf("v7", "weather", "24h"),
            query = mapOf("location" to locationId),
        ).getJSONArray("hourly").mapObjects { it.toHourlyWeather() }
    }

    suspend fun indices(locationId: String): List<WeatherIndex> {
        return getJson(
            pathSegments = listOf("v7", "indices", "1d"),
            query = mapOf(
                "location" to locationId,
                "type" to "1,2,3,5,8,9",
            ),
        ).getJSONArray("daily").mapObjects { it.toWeatherIndex() }
    }

    suspend fun warnings(locationId: String): List<WeatherWarning> {
        return getJson(
            pathSegments = listOf("v7", "warning", "now"),
            query = mapOf("location" to locationId),
        ).optJSONArray("warning")?.mapObjects { it.toWeatherWarning() }.orEmpty()
    }

    suspend fun historicalWeather(locationId: String, date: LocalDate): DailyWeather {
        val weatherDaily = getJson(
            pathSegments = listOf("v7", "historical", "weather"),
            query = mapOf(
                "location" to locationId,
                "date" to date.toString().replace("-", ""),
            ),
        ).getJSONObject("weatherDaily")
        return weatherDaily.toHistoricalDailyWeather(date)
    }

    private suspend fun getJson(
        pathSegments: List<String>,
        query: Map<String, String>,
    ): JSONObject = withContext(Dispatchers.IO) {
        val host = apiHost.trim().removeSuffix("/")
        if (host.isBlank() || apiKey.isBlank()) {
            throw QWeatherException("请先在 local.properties 配置和风天气 API Host 和 API Key")
        }

        val builder = host.toHttpUrl().newBuilder()
        pathSegments.forEach(builder::addPathSegment)
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        builder.addQueryParameter("key", apiKey)

        val request = Request.Builder()
            .url(builder.build())
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("天气服务请求失败：HTTP ${response.code}")
            }
            val root = JSONObject(body)
            val code = root.optString("code")
            if (code != "200") {
                throw QWeatherException(code.toQWeatherMessage())
            }
            root
        }
    }

    private fun JSONObject.toWeatherLocation(): WeatherLocation {
        return WeatherLocation(
            id = getString("id"),
            name = optString("name"),
            district = optString("name"),
            city = optString("adm2"),
            province = optString("adm1"),
        )
    }

    private fun JSONObject.toCurrentWeather(): CurrentWeather {
        return CurrentWeather(
            observedAt = optString("obsTime"),
            temp = optString("temp"),
            feelsLike = optString("feelsLike"),
            text = optString("text"),
            icon = optString("icon"),
            windDir = optString("windDir"),
            windScale = optString("windScale"),
            humidity = optString("humidity"),
            precip = optString("precip"),
        )
    }

    private fun JSONObject.toDailyWeather(): DailyWeather {
        return DailyWeather(
            date = optString("fxDate"),
            tempMax = optString("tempMax"),
            tempMin = optString("tempMin"),
            textDay = optString("textDay"),
            textNight = optString("textNight"),
            iconDay = optString("iconDay"),
            windDirDay = optString("windDirDay"),
            windScaleDay = optString("windScaleDay"),
            humidity = optString("humidity"),
            precip = optString("precip"),
        )
    }

    private fun JSONObject.toHourlyWeather(): HourlyWeather {
        return HourlyWeather(
            time = optString("fxTime"),
            temp = optString("temp"),
            text = optString("text"),
            icon = optString("icon"),
            windScale = optString("windScale"),
            pop = optString("pop"),
        )
    }

    private fun JSONObject.toWeatherIndex(): WeatherIndex {
        return WeatherIndex(
            name = optString("name"),
            category = optString("category"),
            text = optString("text"),
        )
    }

    private fun JSONObject.toWeatherWarning(): WeatherWarning {
        return WeatherWarning(
            title = optString("title"),
            severity = optString("severity"),
            typeName = optString("typeName"),
            text = optString("text"),
        )
    }

    private fun JSONObject.toHistoricalDailyWeather(date: LocalDate): DailyWeather {
        return DailyWeather(
            date = date.toString(),
            tempMax = optString("tempMax"),
            tempMin = optString("tempMin"),
            textDay = optString("textDay"),
            textNight = optString("textNight"),
            iconDay = optString("iconDay"),
            windDirDay = optString("windDir"),
            windScaleDay = optString("windScale"),
            humidity = optString("humidity"),
            precip = optString("precip"),
        )
    }

    private fun String.toQWeatherMessage(): String {
        return when (this) {
            "204" -> "天气服务暂无该地区数据"
            "400" -> "天气服务参数错误"
            "401", "402" -> "和风天气认证失败，请检查 API Key"
            "403" -> "当前和风天气套餐不支持该接口"
            "404" -> "天气服务接口地址不正确"
            "429" -> "天气服务调用过于频繁，请稍后再试"
            else -> "天气服务返回异常：$this"
        }
    }

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        return List(length()) { index -> transform(getJSONObject(index)) }
    }

    class QWeatherException(message: String) : Exception(message)

    companion object {
        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
