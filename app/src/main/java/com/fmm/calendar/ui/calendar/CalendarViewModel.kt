package com.fmm.calendar.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmm.calendar.data.local.CalendarDatabase
import com.fmm.calendar.data.local.CalendarDayEntity
import com.fmm.calendar.data.repository.CalendarRepository
import com.fmm.calendar.data.repository.JieqiDistance
import com.fmm.calendar.data.repository.extractCurrentJieqiName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalendarRepository(
        CalendarDatabase.getInstance(application).calendarDao(),
    )

    private val dayEntitiesCache = mutableMapOf<YearMonth, List<CalendarDayEntity>>()

    private val _uiState = MutableStateFlow(
        LocalDate.now().let { today ->
            CalendarUiState(
                today = today,
                visibleMonth = YearMonth.from(today),
                selectedDate = today,
            )
        },
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        val today = _uiState.value.today
        loadMonth(YearMonth.from(today), selectedDate = today)
    }

    fun onMonthChanged(month: YearMonth) {
        if (month == _uiState.value.visibleMonth) return
        loadMonth(month, selectedDate = preferredSelectedDate(month))
    }

    fun selectDate(date: LocalDate) {
        val targetMonth = YearMonth.from(date)
        if (targetMonth != _uiState.value.visibleMonth) {
            loadMonth(targetMonth, selectedDate = date)
            return
        }

        _uiState.update { current ->
            current.copy(selectedDate = date)
        }
        loadSelectedDate(date)
    }

    fun goToToday() {
        val today = LocalDate.now()
        loadMonth(YearMonth.from(today), selectedDate = today)
    }

    fun refreshTodayIfNeeded() {
        val today = LocalDate.now()
        if (today == _uiState.value.today) return

        loadMonth(YearMonth.from(today), selectedDate = today)
    }

    fun showPreviousMonth() {
        val target = _uiState.value.visibleMonth.minusMonths(1)
        loadMonth(target, selectedDate = preferredSelectedDate(target))
    }

    fun showNextMonth() {
        val target = _uiState.value.visibleMonth.plusMonths(1)
        loadMonth(target, selectedDate = preferredSelectedDate(target))
    }

    // 如果切换到今天所在月份，优先选中今天；否则默认选中该月 1 号。
    private fun preferredSelectedDate(month: YearMonth): LocalDate {
        val today = LocalDate.now()
        return if (month == YearMonth.from(today)) today else month.atDay(1)
    }

    private suspend fun getDaysForMonth(month: YearMonth): List<CalendarDayEntity> {
        dayEntitiesCache[month]?.let { return it }
        return repository.getMonthDays(month.year, month.monthValue).also {
            dayEntitiesCache[month] = it
        }
    }

    private fun loadMonth(month: YearMonth, selectedDate: LocalDate) {
        viewModelScope.launch {
            val today = LocalDate.now()
            _uiState.update {
                it.copy(
                    today = today,
                    isLoading = true,
                    visibleMonth = month,
                    selectedDate = selectedDate,
                    rowCount = calculateRowCount(month),
                )
            }

            // 加载范围：M-2 到 M+2，确保 M-1, M, M+1 的 cells 都能完整构建（包含补全的头尾天数）
            val monthsToFetch = listOf(
                month.minusMonths(2),
                month.minusMonths(1),
                month,
                month.plusMonths(1),
                month.plusMonths(2)
            )
            monthsToFetch.forEach { getDaysForMonth(it) }

            val newCellsMap = mutableMapOf<YearMonth, List<CalendarDayCellUi>>()
            listOf(month.minusMonths(1), month, month.plusMonths(1)).forEach { m ->
                newCellsMap[m] = buildMonthCells(
                    month = m,
                    monthDays = getDaysForMonth(m),
                    previousMonthDays = getDaysForMonth(m.minusMonths(1)),
                    nextMonthDays = getDaysForMonth(m.plusMonths(1)),
                    today = today,
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    monthCells = newCellsMap[month].orEmpty(),
                    monthCellsMap = it.monthCellsMap + newCellsMap
                )
            }
            loadSelectedDate(selectedDate)
        }
    }

    private fun loadSelectedDate(date: LocalDate) {
        viewModelScope.launch {
            val entity = repository.getDay(date)
            val distance = repository.getNextJieqiDistance(date, entity?.jieqi.orEmpty())

            _uiState.update {
                val today = it.today
                it.copy(
                    selectedDay = entity?.toSelectedDayUi(distance, today),
                    selectedEvents = mockEvents(today).filter { event -> event.date == date },
                )
            }
        }
    }

    private fun buildMonthCells(
        month: YearMonth,
        monthDays: List<CalendarDayEntity>,
        previousMonthDays: List<CalendarDayEntity>,
        nextMonthDays: List<CalendarDayEntity>,
        today: LocalDate,
    ): List<CalendarDayCellUi> {
        val leadingEmptyCount = month.atDay(1).dayOfWeek.value % 7
        val trailingEmptyCount = (7 - (leadingEmptyCount + month.lengthOfMonth()) % 7) % 7

        val leading = previousMonthDays
            .takeLast(leadingEmptyCount)
            .map { entity -> entity.toDayCellUi(isCurrentMonth = false, eventDots = emptyList()) }
        val days = monthDays.map { entity ->
            val date = LocalDate.parse(entity.solarDate)
            entity.toDayCellUi(
                isCurrentMonth = true,
                eventDots = eventDotsFor(date, today),
            )
        }
        val trailing = nextMonthDays
            .take(trailingEmptyCount)
            .map { entity -> entity.toDayCellUi(isCurrentMonth = false, eventDots = emptyList()) }

        return leading + days + trailing
    }

    private fun calculateRowCount(month: YearMonth): Int {
        val leadingEmptyCount = month.atDay(1).dayOfWeek.value % 7
        return (leadingEmptyCount + month.lengthOfMonth() + 6) / 7
    }

    private fun eventDotsFor(date: LocalDate, today: LocalDate): List<EventPriority> {
        return mockEvents(today)
            .filter { event -> event.date == date }
            .map { event -> event.priority }
            .distinct()
            .sortedBy { priority -> priority.sortOrder }
            .take(3)
    }
}

data class CalendarUiState(
    val today: LocalDate,
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val monthCells: List<CalendarDayCellUi> = emptyList(),
    val monthCellsMap: Map<YearMonth, List<CalendarDayCellUi>> = emptyMap(),
    val rowCount: Int = 5,
    val selectedDay: SelectedDayUi? = null,
    val selectedEvents: List<MockCalendarEvent> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface CalendarDayCellUi {
    data class Day(
        val date: LocalDate,
        val dayNumber: String,
        val subtitle: String,
        val subtitleType: CalendarSubtitleType,
        val isWeekend: Boolean,
        val festivals: List<String>,
        val jieqiName: String?,
        val eventDots: List<EventPriority>,
        val isCurrentMonth: Boolean,
    ) : CalendarDayCellUi
}

enum class CalendarSubtitleType {
    Jieqi,
    Festival,
    LunarDay,
}

data class SelectedDayUi(
    val date: LocalDate,
    val dateText: String,
    val dayNumber: String,
    val weekdayText: String,
    val statusText: String,
    val ganzhiSummary: String,
    val lunarText: String,
    val tags: List<String>,
    val festivalLine: String,
    val jieqiDistanceText: String,
    val yiItems: List<String>,
    val jiItems: List<String>,
)

data class MockCalendarEvent(
    val date: LocalDate,
    val time: String,
    val title: String,
    val location: String,
    val priority: EventPriority,
)

enum class EventPriority(val sortOrder: Int) {
    High(0),
    Medium(1),
    Low(2),
}

private fun CalendarDayEntity.toSelectedDayUi(distance: JieqiDistance?, today: LocalDate): SelectedDayUi {
    val date = LocalDate.parse(solarDate)
    val jieqiName = extractCurrentJieqiName(jieqi)
    val festivalTags = festivalList()
    val tags = listOfNotNull(jieqiName) + festivalTags.take(2)
    val displayWeekday = "周$weekdayCn"
    val relativeStatus = date.relativeDayText(today)

    return SelectedDayUi(
        date = date,
        dateText = "${year}年${month}月${day}日",
        dayNumber = day.toString(),
        weekdayText = displayWeekday,
        statusText = relativeStatus,
        ganzhiSummary = "$ganzhiYear $ganzhiMonth $ganzhiDay [属$zodiac] $displayWeekday 第${date.weekOfYear()}周 $relativeStatus",
        lunarText = lunarDate,
        tags = tags,
        festivalLine = festivalTags.joinToString(" ") { festival -> "· $festival" },
        jieqiDistanceText = distance?.let { "距${it.name} ${it.days}天" }.orEmpty(),
        yiItems = yiText.splitItems(),
        jiItems = jiText.splitItems(),
    )
}

private fun CalendarDayEntity.toDayCellUi(
    isCurrentMonth: Boolean,
    eventDots: List<EventPriority>,
): CalendarDayCellUi.Day {
    val date = LocalDate.parse(solarDate)
    return CalendarDayCellUi.Day(
        date = date,
        dayNumber = day.toString(),
        subtitle = cellSubtitle(),
        subtitleType = cellSubtitleType(),
        isWeekend = isWeekend == 1,
        festivals = festivalList(),
        jieqiName = extractCurrentJieqiName(jieqi),
        eventDots = eventDots,
        isCurrentMonth = isCurrentMonth,
    )
}

private fun CalendarDayEntity.cellSubtitle(): String {
    extractCurrentJieqiName(jieqi)?.let { return it }
    festivalList().firstOrNull { festival -> festival.length < 5 }?.let { return it }
    return lunarDayOnly()
}

private fun CalendarDayEntity.cellSubtitleType(): CalendarSubtitleType {
    return when {
        extractCurrentJieqiName(jieqi) != null -> CalendarSubtitleType.Jieqi
        festivalList().any { festival -> festival.length < 5 } -> CalendarSubtitleType.Festival
        else -> CalendarSubtitleType.LunarDay
    }
}

private fun CalendarDayEntity.festivalList(): List<String> {
    return festivalsText.splitItems()
}

// 月历 cell 的副标题只显示“农历天”，不显示农历月。
// 例如：
// - 八月廿一 -> 廿一
// - 九月初一 -> 初一
private fun CalendarDayEntity.lunarDayOnly(): String {
    val monthSeparatorIndex = lunarDate.indexOf("月")
    return if (monthSeparatorIndex >= 0 && monthSeparatorIndex < lunarDate.lastIndex) {
        lunarDate.substring(monthSeparatorIndex + 1)
    } else {
        lunarDate
    }
}

private fun String.splitItems(): List<String> {
    return split("、")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun LocalDate.relativeDayText(today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, this).toInt()
    return when (days) {
        0 -> "今天"
        -1 -> "昨天"
        1 -> "明天"
        2 -> "后天"
        else -> if (days < 0) "${-days}天前" else "${days}天后"
    }
}

private fun LocalDate.weekOfYear(): Int {
    return java.time.temporal.WeekFields.ISO.weekOfYear().getFrom(this).toInt()
}

private fun mockEvents(today: LocalDate) = listOf(
    MockCalendarEvent(
        date = today,
        time = "10:00",
        title = "项目周会",
        location = "会议室A",
        priority = EventPriority.High,
    ),
    MockCalendarEvent(
        date = today,
        time = "14:30",
        title = "设计评审",
        location = "会议室B",
        priority = EventPriority.Low,
    ),
    MockCalendarEvent(
        date = today,
        time = "19:00",
        title = "健身",
        location = "健身房",
        priority = EventPriority.Medium,
    ),
    MockCalendarEvent(
        date = today.plusDays(2),
        time = "09:30",
        title = "整理需求",
        location = "线上",
        priority = EventPriority.Medium,
    ),
    MockCalendarEvent(
        date = today.plusDays(5),
        time = "16:00",
        title = "版本检查",
        location = "会议室C",
        priority = EventPriority.Low,
    ),
    MockCalendarEvent(
        date = today.minusDays(3),
        time = "11:00",
        title = "资料归档",
        location = "办公室",
        priority = EventPriority.High,
    ),
)
