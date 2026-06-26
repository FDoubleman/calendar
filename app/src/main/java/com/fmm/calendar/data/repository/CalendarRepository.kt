package com.fmm.calendar.data.repository

import com.fmm.calendar.data.local.CalendarDao
import com.fmm.calendar.data.local.CalendarDayEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CalendarRepository(
    private val calendarDao: CalendarDao,
) {
    suspend fun getMonthDays(year: Int, month: Int): List<CalendarDayEntity> {
        return calendarDao.getMonthDays(year, month)
    }

    suspend fun getDay(date: LocalDate): CalendarDayEntity? {
        return calendarDao.getDay(date.toString())
    }

    suspend fun getNextJieqiDistance(date: LocalDate, currentJieqiText: String): JieqiDistance? {
        parseDistanceFromJieqi(currentJieqiText)?.let { return it }

        val next = calendarDao.getNextJieqiDay(date.toString()) ?: return null
        val nextDate = LocalDate.parse(next.solarDate)
        val name = extractCurrentJieqiName(next.jieqi) ?: return null
        val days = ChronoUnit.DAYS.between(date, nextDate).toInt()
        return JieqiDistance(name = name, days = days)
    }

    private fun parseDistanceFromJieqi(jieqi: String): JieqiDistance? {
        if (jieqi.isBlank()) return null
        val regex = Regex("距下一个节气“(.+?)”，还有(\\d+)天")
        val match = regex.find(jieqi) ?: return null
        val name = match.groupValues.getOrNull(1).orEmpty()
        val days = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        return JieqiDistance(name = name, days = days)
    }
}

data class JieqiDistance(
    val name: String,
    val days: Int,
)

val solarTerms = listOf(
    "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
    "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
    "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
    "立冬", "小雪", "大雪", "冬至", "小寒", "大寒",
)

// 月历 cell 只在“当天正好是节气日”时显示节气名称。
// 数据库中的 jieqi 形如“秋分 第9天 （距下一个节气...）”，
// 只有“第1天”才表示这个公历日就是节气当天。
fun extractCurrentJieqiName(jieqi: String): String? {
    if (jieqi.isBlank() || !jieqi.contains("第1天")) return null
    return solarTerms.firstOrNull { term -> jieqi.startsWith(term) }
}
