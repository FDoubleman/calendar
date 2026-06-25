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

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalendarRepository(
        CalendarDatabase.getInstance(application).calendarDao(),
    )

    private val today = LocalDate.now()

    private val _uiState = MutableStateFlow(
        CalendarUiState(
            today = today,
            visibleMonth = YearMonth.from(today),
            selectedDate = today,
        ),
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(YearMonth.from(today), selectedDate = today)
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { current ->
            current.copy(selectedDate = date)
        }
        loadSelectedDate(date)
    }

    fun goToToday() {
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
        return if (month == YearMonth.from(today)) today else month.atDay(1)
    }

    private fun loadMonth(month: YearMonth, selectedDate: LocalDate) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    visibleMonth = month,
                    selectedDate = selectedDate,
                )
            }

            val monthDays = repository.getMonthDays(month.year, month.monthValue)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    monthCells = buildMonthCells(month, monthDays),
                    rowCount = calculateRowCount(month),
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
                it.copy(
                    selectedDay = entity?.toSelectedDayUi(distance),
                    selectedEvents = mockEvents.filter { event -> event.date == date },
                )
            }
        }
    }

    private fun buildMonthCells(
        month: YearMonth,
        monthDays: List<CalendarDayEntity>,
    ): List<CalendarDayCellUi> {
        val leadingEmptyCount = month.atDay(1).dayOfWeek.value % 7
        val trailingEmptyCount = (7 - (leadingEmptyCount + month.lengthOfMonth()) % 7) % 7

        val leading = List(leadingEmptyCount) { CalendarDayCellUi.Empty }
        val days = monthDays.map { entity ->
            val date = LocalDate.parse(entity.solarDate)
            CalendarDayCellUi.Day(
                date = date,
                dayNumber = entity.day.toString(),
                subtitle = entity.cellSubtitle(),
                isWeekend = entity.isWeekend == 1,
                festivals = entity.festivalList(),
                jieqiName = extractCurrentJieqiName(entity.jieqi),
                eventDots = eventDotsFor(date),
            )
        }
        val trailing = List(trailingEmptyCount) { CalendarDayCellUi.Empty }

        return leading + days + trailing
    }

    private fun calculateRowCount(month: YearMonth): Int {
        val leadingEmptyCount = month.atDay(1).dayOfWeek.value % 7
        return (leadingEmptyCount + month.lengthOfMonth() + 6) / 7
    }

    private fun eventDotsFor(date: LocalDate): List<EventPriority> {
        return mockEvents
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
    val rowCount: Int = 5,
    val selectedDay: SelectedDayUi? = null,
    val selectedEvents: List<MockCalendarEvent> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface CalendarDayCellUi {
    data object Empty : CalendarDayCellUi

    data class Day(
        val date: LocalDate,
        val dayNumber: String,
        val subtitle: String,
        val isWeekend: Boolean,
        val festivals: List<String>,
        val jieqiName: String?,
        val eventDots: List<EventPriority>,
    ) : CalendarDayCellUi
}

data class SelectedDayUi(
    val date: LocalDate,
    val dateText: String,
    val dayNumber: String,
    val weekdayText: String,
    val lunarText: String,
    val tags: List<String>,
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

private fun CalendarDayEntity.toSelectedDayUi(distance: JieqiDistance?): SelectedDayUi {
    val date = LocalDate.parse(solarDate)
    val jieqiName = extractCurrentJieqiName(jieqi)
    val festivalTags = festivalList().take(2)
    val tags = listOfNotNull(jieqiName) + festivalTags

    return SelectedDayUi(
        date = date,
        dateText = "${year}年${month}月${day}日",
        dayNumber = day.toString(),
        weekdayText = "星期$weekdayCn",
        lunarText = "$ganzhiYear $lunarDate",
        tags = tags,
        jieqiDistanceText = distance?.let { "距${it.name} ${it.days}天" }.orEmpty(),
        yiItems = yiText.splitItems().take(10),
        jiItems = jiText.splitItems().take(10),
    )
}

private fun CalendarDayEntity.cellSubtitle(): String {
    extractCurrentJieqiName(jieqi)?.let { return it }
    festivalList().firstOrNull { festival -> festival.length < 6 }?.let { return it }
    return lunarDate
}

private fun CalendarDayEntity.festivalList(): List<String> {
    return festivalsText.splitItems()
}

private fun String.splitItems(): List<String> {
    return split("、")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private val mockEvents = listOf(
    MockCalendarEvent(
        date = LocalDate.now(),
        time = "10:00",
        title = "项目周会",
        location = "会议室A",
        priority = EventPriority.High,
    ),
    MockCalendarEvent(
        date = LocalDate.now(),
        time = "14:30",
        title = "设计评审",
        location = "会议室B",
        priority = EventPriority.Low,
    ),
    MockCalendarEvent(
        date = LocalDate.now(),
        time = "19:00",
        title = "健身",
        location = "健身房",
        priority = EventPriority.Medium,
    ),
    MockCalendarEvent(
        date = LocalDate.now().plusDays(2),
        time = "09:30",
        title = "整理需求",
        location = "线上",
        priority = EventPriority.Medium,
    ),
    MockCalendarEvent(
        date = LocalDate.now().plusDays(5),
        time = "16:00",
        title = "版本检查",
        location = "会议室C",
        priority = EventPriority.Low,
    ),
    MockCalendarEvent(
        date = LocalDate.now().minusDays(3),
        time = "11:00",
        title = "资料归档",
        location = "办公室",
        priority = EventPriority.High,
    ),
)
