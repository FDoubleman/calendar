package com.fmm.calendar.ui.weather

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmm.calendar.data.weather.DailyWeather
import com.fmm.calendar.data.weather.HourlyWeather
import com.fmm.calendar.data.weather.WeatherIndex
import com.fmm.calendar.data.weather.WeatherSnapshot
import com.fmm.calendar.data.weather.WeatherWarning
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        viewModel.onPermissionResult(
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
        )
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    WeatherScreenContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onRequestPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
    )
}

@Composable
private fun WeatherScreenContent(
    uiState: WeatherUiState,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WeatherTopBar(onRefresh = onRefresh, loading = uiState.isLoading)

                val snapshot = uiState.snapshot
                if (snapshot != null) {
                    CurrentWeatherCard(snapshot = snapshot, onRefresh = onRefresh)
                    WarningStrip(warnings = snapshot.warnings)
                    TodayTomorrowCard(daily = snapshot.daily.take(2))
                    IndicesCard(indices = snapshot.indices)
                    HourlyForecastCard(hourly = snapshot.hourly)
                    DailyForecastCard(
                        yesterday = snapshot.yesterday,
                        yesterdayUnavailableReason = snapshot.yesterdayUnavailableReason,
                        daily = snapshot.daily,
                    )
                } else {
                    EmptyWeatherCard(
                        message = uiState.errorMessage ?: "正在准备当前位置天气",
                        hasPermission = uiState.hasLocationPermission,
                        onRefresh = onRefresh,
                        onRequestPermission = onRequestPermission,
                    )
                }
            }

            if (uiState.isLoading && uiState.snapshot == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun WeatherTopBar(
    onRefresh: () -> Unit,
    loading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "天气",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(
            onClick = onRefresh,
            enabled = !loading,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "刷新天气",
            )
        }
    }
}

@Composable
private fun CurrentWeatherCard(
    snapshot: WeatherSnapshot,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = snapshot.location.displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = snapshot.current.observedAt.toFriendlyTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = snapshot.current.temp.ifBlank { "--" },
                            fontSize = 62.sp,
                            lineHeight = 66.sp,
                            fontWeight = FontWeight.Light,
                        )
                        Text(
                            text = "°",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Light,
                        )
                    }
                    Text(
                        text = snapshot.current.text.ifBlank { "天气未知" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                WeatherIconBubble(
                    text = snapshot.current.text,
                    icon = snapshot.current.icon,
                    modifier = Modifier.size(72.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WeatherMetricChip("体感", "${snapshot.current.feelsLike.blankAsDash()}°")
                WeatherMetricChip("湿度", "${snapshot.current.humidity.blankAsDash()}%")
                WeatherMetricChip("降水", "${snapshot.current.precip.blankAsDash()}mm")
                WeatherMetricChip("风力", "${snapshot.current.windDir} ${snapshot.current.windScale.blankAsDash()}级")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onRefresh,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                elevation = null,
            ) {
                Text(text = "更新当前位置天气")
            }
        }
    }
}

@Composable
private fun WarningStrip(warnings: List<WeatherWarning>) {
    if (warnings.isEmpty()) return

    val warning = warnings.first()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF3D8),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = Color(0xFF9A5F00),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = warning.title.ifBlank { "${warning.typeName}${warning.severity}预警" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6F4300),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = warning.text.ifBlank { "请留意最新天气预警信息" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6F4300),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TodayTomorrowCard(daily: List<DailyWeather>) {
    WeatherSectionCard(title = null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            daily.forEachIndexed { index, item ->
                DaySummary(
                    title = if (index == 0) "今天" else "明天",
                    day = item,
                    modifier = Modifier.weight(1f),
                )
            }
            if (daily.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DaySummary(
    title: String,
    day: DailyWeather,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${day.tempMax.blankAsDash()}/${day.tempMin.blankAsDash()}°C",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = day.textDay.ifBlank { "--" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        WeatherIconBubble(
            text = day.textDay,
            icon = day.iconDay,
            modifier = Modifier.size(44.dp),
        )
    }
}

@Composable
private fun IndicesCard(indices: List<WeatherIndex>) {
    WeatherSectionCard(title = "生活指数") {
        if (indices.isEmpty()) {
            MutedText("生活指数暂不可用")
            return@WeatherSectionCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            indices.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { index ->
                        IndexItem(
                            index = index,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexItem(
    index: WeatherIndex,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 82.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (index.name.contains("雨")) Icons.Outlined.Umbrella else Icons.Outlined.Cloud,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = index.name.ifBlank { "指数" },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = index.category.ifBlank { "--" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = index.text.ifBlank { "暂无建议" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HourlyForecastCard(hourly: List<HourlyWeather>) {
    WeatherSectionCard(title = "24小时预报") {
        if (hourly.isEmpty()) {
            MutedText("24小时预报暂不可用")
            return@WeatherSectionCard
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(hourly) { item ->
                HourlyItem(item = item)
            }
        }
    }
}

@Composable
private fun HourlyItem(item: HourlyWeather) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = item.time.toHourLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        WeatherIconBubble(
            text = item.text,
            icon = item.icon,
            modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${item.temp.blankAsDash()}°",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "风${item.windScale.blankAsDash()}级",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = "雨${item.pop.blankAsDash()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun DailyForecastCard(
    yesterday: DailyWeather?,
    yesterdayUnavailableReason: String?,
    daily: List<DailyWeather>,
) {
    WeatherSectionCard(title = "15日预报") {
        if (daily.isEmpty()) {
            MutedText("15日预报暂不可用")
            return@WeatherSectionCard
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            if (yesterday != null) {
                item {
                    DailyItem(label = "昨天", day = yesterday)
                }
            } else {
                item {
                    YesterdayUnavailableItem(reason = yesterdayUnavailableReason)
                }
            }
            items(daily) { day ->
                DailyItem(label = day.date.toDateLabel(), day = day)
            }
        }
    }
}

@Composable
private fun DailyItem(
    label: String,
    day: DailyWeather,
) {
    Column(
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = day.date.toMonthDay(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        WeatherIconBubble(
            text = day.textDay,
            icon = day.iconDay,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = day.textDay.ifBlank { "--" },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${day.tempMax.blankAsDash()}°",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${day.tempMin.blankAsDash()}°",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE55D5D),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = day.windDirDay.ifBlank { "--" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${day.windScaleDay.blankAsDash()}级",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun YesterdayUnavailableItem(reason: String?) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "昨天",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MutedText(
            text = reason ?: "昨日数据暂不可用",
            maxLines = 4,
        )
    }
}

@Composable
private fun EmptyWeatherCard(
    message: String,
    hasPermission: Boolean?,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = if (hasPermission == false) onRequestPermission else onRefresh,
            ) {
                Text(text = if (hasPermission == false) "授权定位" else "重新加载")
            }
        }
    }
}

@Composable
private fun WeatherSectionCard(
    title: String?,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
private fun WeatherMetricChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
    ) {
        Text(
            text = "$label $value",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun WeatherIconBubble(
    text: String,
    icon: String,
    modifier: Modifier = Modifier,
) {
    val color = weatherTint(icon = icon, text = text)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = weatherSymbol(icon = icon, text = text),
            fontSize = 24.sp,
        )
    }
}

@Composable
private fun MutedText(
    text: String,
    maxLines: Int = 2,
) {
    Text(
        text = text,
        modifier = Modifier.widthIn(max = 260.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun weatherSymbol(icon: String, text: String): String {
    return when {
        icon.startsWith("10") || text.contains("晴") -> "晴"
        icon.startsWith("15") || text.contains("夜") -> "夜"
        text.contains("雷") -> "雷"
        text.contains("雪") -> "雪"
        text.contains("雨") -> "雨"
        text.contains("云") || text.contains("阴") -> "云"
        text.contains("雾") || text.contains("霾") -> "雾"
        else -> "天"
    }
}

private fun weatherTint(icon: String, text: String): Color {
    return when {
        text.contains("雨") -> Color(0xFF4AA8D8)
        text.contains("雪") -> Color(0xFF7AA7D9)
        text.contains("雷") -> Color(0xFFE0A536)
        icon.startsWith("10") || text.contains("晴") -> Color(0xFFFFB84D)
        else -> Color(0xFF69A7D8)
    }
}

private fun String.blankAsDash(): String = ifBlank { "--" }

private fun String.toFriendlyTime(): String {
    if (isBlank()) return "刚刚更新"
    return try {
        val time = OffsetDateTime.parse(this)
        time.format(DateTimeFormatter.ofPattern("HH:mm 更新", Locale.CHINA))
    } catch (_: DateTimeParseException) {
        this
    }
}

private fun String.toHourLabel(): String {
    if (isBlank()) return "--:--"
    return try {
        OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
    } catch (_: DateTimeParseException) {
        takeLast(5)
    }
}

private fun String.toDateLabel(): String {
    return runCatching {
        val date = LocalDate.parse(this)
        val today = LocalDate.now()
        when (date) {
            today -> "今天"
            today.plusDays(1) -> "明天"
            else -> date.dayOfWeek.chineseName()
        }
    }.getOrDefault("--")
}

private fun String.toMonthDay(): String {
    return runCatching {
        LocalDate.parse(this).format(DateTimeFormatter.ofPattern("MM/dd", Locale.CHINA))
    }.getOrDefault(this)
}

private fun java.time.DayOfWeek.chineseName(): String {
    return when (this) {
        java.time.DayOfWeek.MONDAY -> "周一"
        java.time.DayOfWeek.TUESDAY -> "周二"
        java.time.DayOfWeek.WEDNESDAY -> "周三"
        java.time.DayOfWeek.THURSDAY -> "周四"
        java.time.DayOfWeek.FRIDAY -> "周五"
        java.time.DayOfWeek.SATURDAY -> "周六"
        java.time.DayOfWeek.SUNDAY -> "周日"
    }
}
