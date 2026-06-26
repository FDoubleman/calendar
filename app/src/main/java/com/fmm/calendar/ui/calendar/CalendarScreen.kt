package com.fmm.calendar.ui.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CalendarScreenContent(
        uiState = uiState,
        onDateClick = viewModel::selectDate,
        onTodayClick = viewModel::goToToday,
        onMonthChanged = viewModel::onMonthChanged,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarScreenContent(
    uiState: CalendarUiState,
    onDateClick: (LocalDate) -> Unit,
    onTodayClick: () -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
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
                visibleMonth = uiState.visibleMonth,
                selectedDate = uiState.selectedDate,
                today = uiState.today,
                monthCellsMap = uiState.monthCellsMap,
                onDateClick = onDateClick,
                onTodayClick = onTodayClick,
                onMonthChanged = onMonthChanged,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthCalendarCard(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    monthCellsMap: Map<YearMonth, List<CalendarDayCellUi>>,
    onDateClick: (LocalDate) -> Unit,
    onTodayClick: () -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
) {
    val initialMonth = remember { YearMonth.from(today) }
    val pagerState = rememberPagerState(initialPage = 2500) { 5000 }

    // 使用 derivedStateOf 实时计算当前显示的月份，用于标题显示，确保滑动过程中的标题切换比等待 VM 状态回传更即时且丝滑
    val displayedMonth = remember {
        derivedStateOf {
            initialMonth.plusMonths((pagerState.currentPage - 2500).toLong())
        }
    }

    // 动态计算当前月份所需的行数 (rowCount)
    val targetRowCount by remember {
        derivedStateOf {
            val month = displayedMonth.value
            val firstDayOfWeek = month.atDay(1).dayOfWeek.value % 7
            val totalDays = firstDayOfWeek + month.lengthOfMonth()
            (totalDays + 6) / 7
        }
    }

    // 监听 Pager 滑动，当滑动停止并确定页面后（settledPage），再通知 VM 更新数据，减少滑动过程中的 UI 线程负载
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val targetMonth = initialMonth.plusMonths((page - 2500).toLong())
            if (targetMonth != visibleMonth) {
                onMonthChanged(targetMonth)
            }
        }
    }

    // 监听 VM 状态变化（如点击“今天”或外部切换），同步 Pager 位置
    LaunchedEffect(visibleMonth) {
        val targetPage = 2500 + ChronoUnit.MONTHS.between(initialMonth, visibleMonth).toInt()
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                        text = displayedMonth.value.monthTitle(),
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
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((targetRowCount * 60).dp), // 动态高度 = 行数 * 每行高度
                beyondBoundsPageCount = 1, // 预加载前后页，使滑动切换更流畅
                key = { it }, // 使用 key 优化 Pager 复用
            ) { page ->
                val month = initialMonth.plusMonths((page - 2500).toLong())
                val cells = monthCellsMap[month]
                
                if (cells != null) {
                    MonthGrid(
                        cells = cells,
                        selectedDate = selectedDate,
                        today = today,
                        onDateClick = onDateClick,
                    )
                } else {
                    // 加载中的占位，使用 fillMaxSize 以适应 Pager 的动态高度
                    Box(modifier = Modifier.fillMaxWidth().fillMaxSize())
                }
            }
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
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
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

            val onSurface = MaterialTheme.colorScheme.onSurface
            val dayColor = remember(selected, isToday, cell.isCurrentMonth, cell.isWeekend, onSurface) {
                if (selected || isToday) {
                    Color.White
                } else if (!cell.isCurrentMonth) {
                    onSurface.copy(alpha = 0.35f)
                } else if (cell.isWeekend) {
                    ErrorRed
                } else {
                    onSurface
                }
            }
            val subtitleColor = remember(selected, isToday, cell.isCurrentMonth, cell.subtitleType) {
                if (selected) {
                    AccentGreen
                } else if (isToday) {
                    Color(0xFFBDBDBD)
                } else {
                    cell.subtitleType.baseSubtitleColor().let { baseColor ->
                        if (cell.isCurrentMonth) baseColor else baseColor.copy(alpha = 0.45f)
                    }
                }
            }

            Box(
                modifier = modifier
                    .height(60.dp)
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
                            .background(
                                when {
                                    selected -> AccentGreen
                                    isToday -> Color(0xFFBDBDBD)
                                    else -> Color.Transparent
                                }
                            ),
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

@Preview(showBackground = true)
@Composable
private fun PreviewCalendarScreen() {
    val today = LocalDate.now()
    val visibleMonth = YearMonth.from(today)
    
    val mockCells = (1..35).map { i ->
        val date = today.minusDays(today.dayOfMonth.toLong() - i)
        CalendarDayCellUi.Day(
            date = date,
            dayNumber = date.dayOfMonth.toString(),
            subtitle = if (i % 7 == 0) "冬至" else "初$i",
            subtitleType = if (i % 7 == 0) CalendarSubtitleType.Jieqi else CalendarSubtitleType.LunarDay,
            isWeekend = i % 7 == 0 || i % 7 == 6,
            festivals = emptyList(),
            jieqiName = null,
            eventDots = if (i % 3 == 0) listOf(EventPriority.High, EventPriority.Medium) else emptyList(),
            isCurrentMonth = date.monthValue == today.monthValue
        )
    }

    val mockUiState = CalendarUiState(
        today = today,
        visibleMonth = visibleMonth,
        selectedDate = today,
        monthCells = mockCells,
        monthCellsMap = mapOf(visibleMonth to mockCells),
        selectedDay = SelectedDayUi(
            date = today,
            dateText = "2024年12月20日",
            dayNumber = "20",
            weekdayText = "周五",
            statusText = "今天",
            ganzhiSummary = "甲辰年 丙子月 癸卯日 [属龙] 周五 第51周 今天",
            lunarText = "冬月二十",
            tags = listOf("冬至"),
            festivalLine = "· 冬至",
            jieqiDistanceText = "距小寒 15天",
            yiItems = listOf("祭祀", "祈福", "纳畜", "入宅"),
            jiItems = listOf("嫁娶", "安葬", "作灶")
        ),
        selectedEvents = listOf(
            MockCalendarEvent(today, "10:00", "项目会议", "会议室A", EventPriority.High),
            MockCalendarEvent(today, "14:00", "需求评审", "线上", EventPriority.Medium)
        )
    )

    MaterialTheme {
        CalendarScreenContent(
            uiState = mockUiState,
            onDateClick = {},
            onTodayClick = {},
            onMonthChanged = {}
        )
    }
}
