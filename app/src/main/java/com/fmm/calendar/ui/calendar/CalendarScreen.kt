package com.fmm.calendar.ui.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // collectAsState 会把 ViewModel 中的 StateFlow 转成 Compose State。
    // 当 ViewModel 更新月份或选中日期时，这个页面会自动重组并刷新 UI。
    CalendarScreenContent(
        uiState = uiState,
        onDateClick = viewModel::selectDate,
        onTodayClick = viewModel::goToToday,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
    )
}

@Composable
private fun CalendarScreenContent(
    uiState: CalendarUiState,
    onDateClick: (LocalDate) -> Unit,
    onTodayClick: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            CalendarTopBar()
            Spacer(modifier = Modifier.height(8.dp))
            MonthCalendarCard(
                uiState = uiState,
                onDateClick = onDateClick,
                onTodayClick = onTodayClick,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectedDateCard(selectedDay = uiState.selectedDay)
            Spacer(modifier = Modifier.height(10.dp))
            AlmanacCard(selectedDay = uiState.selectedDay)
            Spacer(modifier = Modifier.height(10.dp))
            TodayEventsCard(events = uiState.selectedEvents)
        }
    }
}

@Composable
private fun CalendarTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "万年历",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        // 顶部两个按钮本版只做占位。保留 onClick 入口，后续可以接入快速入口或设置页。
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Outlined.Today,
                contentDescription = "顶部功能占位",
            )
        }
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "设置占位",
            )
        }
    }
}

@Composable
private fun MonthCalendarCard(
    uiState: CalendarUiState,
    onDateClick: (LocalDate) -> Unit,
    onTodayClick: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // animateContentSize 会在月历行数从 6 行变 5 行、5 行变 4 行时平滑改变高度。
            .animateContentSize(animationSpec = tween(durationMillis = 220)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { /* 后续用于快速定位到指定日期。 */ }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiState.visibleMonth.monthTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = "选择年月占位",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                onClick = onTodayClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(text = "今天")
                }
            }
            WeekHeader()
            MonthGrid(
                cells = uiState.monthCells,
                selectedDate = uiState.selectedDate,
                today = uiState.today,
                onDateClick = onDateClick,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )
        }
    }
}

@Composable
private fun WeekHeader() {
    val weeks = listOf("日", "一", "二", "三", "四", "五", "六")
    Row(modifier = Modifier.fillMaxWidth()) {
        weeks.forEachIndexed { index, week ->
            Text(
                text = week,
                modifier = Modifier.weight(1f),
                color = if (index == 0 || index == 6) ErrorRed else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    cells: List<CalendarDayCellUi>,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    var dragAmount by remember { mutableFloatStateOf(0f) }

    // 手势只挂在月历网格上：左滑下个月，右滑上个月。
    // consume 逻辑保持简单，方便先理解“累计拖动距离 -> 判断方向 -> 切换月份”这条线。
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAmount = 0f },
                    onHorizontalDrag = { _, amount -> dragAmount += amount },
                    onDragEnd = {
                        when {
                            dragAmount < -80f -> onNextMonth()
                            dragAmount > 80f -> onPreviousMonth()
                        }
                        dragAmount = 0f
                    },
                )
            },
    ) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                week.forEach { cell ->
                    CalendarDayCell(
                        cell = cell,
                        selectedDate = selectedDate,
                        today = today,
                        onDateClick = onDateClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarDayCellUi,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (cell) {
        is CalendarDayCellUi.Day -> {
            val selected = cell.date == selectedDate
            val isToday = cell.date == today
            val dayColor = if (selected) {
                Color.White
            } else if (!cell.isCurrentMonth) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            } else if (cell.isWeekend) {
                ErrorRed
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val subtitleColor = if (selected) {
                AccentGreen
            } else {
                cell.subtitleType.baseSubtitleColor().let { baseColor ->
                    if (cell.isCurrentMonth) baseColor else baseColor.copy(alpha = 0.45f)
                }
            }

            Box(
                modifier = modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDateClick(cell.date) },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (selected) AccentGreen else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = cell.dayNumber,
                            color = dayColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                    Text(
                        text = cell.subtitle,
                        color = subtitleColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    EventDots(dots = cell.eventDots)
                }
            }
        }
    }
}

private fun CalendarSubtitleType.baseSubtitleColor(): Color {
    return when (this) {
        CalendarSubtitleType.Jieqi -> BlueDot
        CalendarSubtitleType.Festival -> ErrorRed
        CalendarSubtitleType.LunarDay -> Color.Gray
    }
}

@Composable
private fun EventDots(dots: List<EventPriority>) {
    Row(
        modifier = Modifier.height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dots.forEach { priority ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(priority.color),
            )
        }
    }
}

@Composable
private fun SelectedDateCard(selectedDay: SelectedDayUi?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        if (selectedDay == null) {
            Text(
                text = "正在加载日期信息...",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Card
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 38.dp)
                        .border(1.5.dp, ErrorRed, RoundedCornerShape(4.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "农\n历",
                        color = ErrorRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = selectedDay.lunarText,
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = selectedDay.ganzhiSummary,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )

            if (selectedDay.festivalLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedDay.festivalLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (selectedDay.jieqiDistanceText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                ) {
                    Text(
                        text = selectedDay.jieqiDistanceText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagRow(tags: List<String>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.take(3).forEachIndexed { index, tag ->
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = tag,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (index == 0) SoftGreen else SoftBlue,
                    labelColor = if (index == 0) AccentGreen else BlueDot,
                ),
                border = null,
            )
        }
    }
}

@Composable
private fun AlmanacCard(selectedDay: SelectedDayUi?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AlmanacColumn(
                title = "今日宜",
                titleColor = ErrorRed,
                dotColor = ErrorRed,
                items = selectedDay?.yiItems.orEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            )
            AlmanacColumn(
                title = "今日忌",
                titleColor = Color.Black,
                dotColor = Color.Black,
                items = selectedDay?.jiItems.orEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AlmanacColumn(
    title: String,
    titleColor: Color,
    dotColor: Color,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(dotColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title.removePrefix("今日"),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (items.isEmpty()) "暂无" else items.joinToString(" "),
                modifier = Modifier.weight(1f),
                color = titleColor,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun TodayEventsCard(events: List<MockCalendarEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "今日事项",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.clickable { },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "添加事项占位",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "添加事项",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (events.isEmpty()) {
                Text(
                    text = "暂无事项",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                events.forEach { event ->
                    EventRow(event = event)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: MockCalendarEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = event.priority.color)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = event.time,
            modifier = Modifier.width(54.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = event.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = event.location,
            modifier = Modifier.widthIn(max = 64.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun YearMonth.monthTitle(): String {
    return "${year}年${monthValue}月"
}

private val EventPriority.color: Color
    get() = when (this) {
        EventPriority.High -> ErrorRed
        EventPriority.Medium -> YellowDot
        EventPriority.Low -> BlueDot
    }

private val AccentGreen = Color(0xFF119B55)
private val SoftGreen = Color(0xFFE3F5EA)
private val SoftBlue = Color(0xFFE8F2FF)
private val ErrorRed = Color(0xFFE53935)
private val YellowDot = Color(0xFFFF9800)
private val BlueDot = Color(0xFF1976D2)
