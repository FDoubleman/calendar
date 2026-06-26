package com.fmm.calendar.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 简易农历工具类，支持 1900-2100 年。
 * 用于日期选择器的滚轮显示及预览文字转换。
 */
object LunarCalendarHelper {

    private val MIN_DATE = LocalDate.of(1900, 1, 31)
    private val MAX_DATE = LocalDate.of(2100, 12, 31)

    // 农历 1900-2100 的信息（二进制压缩格式）
    // 前12位代表12个月的大小（1大0小），第13-16位代表闰月月份（0表示无闰月），第17位代表闰月大小
    private val LUNAR_INFO = longArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520
    )

    private val LUNAR_MONTH_NAME = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    private val LUNAR_DAY_NAME = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    data class LunarDate(
        val year: Int,
        val month: Int,
        val day: Int,
        val isLeap: Boolean,
        val lunarMonthName: String,
        val lunarDayName: String
    )

    /**
     * 公历转农历
     */
    fun fromSolar(date: LocalDate): LunarDate? {
        if (date.isBefore(MIN_DATE) || date.isAfter(MAX_DATE)) return null

        var offset = ChronoUnit.DAYS.between(MIN_DATE, date).toInt()
        var year = 1900
        while (year <= 2100 && offset > 0) {
            val daysInYear = getLunarYearDays(year)
            if (offset < daysInYear) break
            offset -= daysInYear
            year++
        }

        val leapMonth = getLeapMonth(year)
        var month = 1
        var isLeap = false
        while (month <= 12 && offset > 0) {
            // 检查是否有闰月
            if (leapMonth > 0 && month == leapMonth + 1 && !isLeap) {
                // 进入闰月处理
                val leapDays = getLeapMonthDays(year)
                if (offset < leapDays) {
                    isLeap = true
                    month-- // 逻辑上月份还是原来的
                    break
                }
                offset -= leapDays
            }

            val monthDays = getLunarMonthDays(year, month)
            if (offset < monthDays) break
            offset -= monthDays
            month++
        }

        val day = offset + 1
        val monthName = (if (isLeap) "闰" else "") + LUNAR_MONTH_NAME[month - 1]
        val dayName = LUNAR_DAY_NAME[day - 1]

        return LunarDate(year, month, day, isLeap, monthName, dayName)
    }

    /**
     * 农历转公历 (逻辑相对复杂，这里提供基础实现)
     */
    fun toSolar(year: Int, month: Int, day: Int, isLeap: Boolean): LocalDate {
        var offset = 0
        for (y in 1900 until year) {
            offset += getLunarYearDays(y)
        }

        val leapMonth = getLeapMonth(year)
        for (m in 1 until month) {
            offset += getLunarMonthDays(year, m)
            if (leapMonth > 0 && m == leapMonth) {
                offset += getLeapMonthDays(year)
            }
        }

        if (isLeap) {
            offset += getLunarMonthDays(year, month)
        }

        offset += (day - 1)
        return MIN_DATE.plusDays(offset.toLong())
    }

    // 获取农历年总天数
    private fun getLunarYearDays(year: Int): Int {
        var sum = 348
        val info = LUNAR_INFO[year - 1900]
        var i = 0x8000
        while (i > 0x8) {
            if ((info and i.toLong()) != 0L) sum++
            i = i shr 1
        }
        return sum + getLeapMonthDays(year)
    }

    // 获取闰月月份，0表示无闰月
    fun getLeapMonth(year: Int): Int {
        return (LUNAR_INFO[year - 1900] and 0xf).toInt()
    }

    // 获取闰月天数
    private fun getLeapMonthDays(year: Int): Int {
        return if (getLeapMonth(year) != 0) {
            if ((LUNAR_INFO[year - 1900] and 0x10000L) != 0L) 30 else 29
        } else 0
    }

    // 获取某月天数
    fun getLunarMonthDays(year: Int, month: Int): Int {
        return if ((LUNAR_INFO[year - 1900] and (0x10000 shr month).toLong()) != 0L) 30 else 29
    }

    fun getLunarMonthNames(year: Int): List<String> {
        val names = mutableListOf<String>()
        val leapMonth = getLeapMonth(year)
        for (i in 1..12) {
            names.add(LUNAR_MONTH_NAME[i - 1])
            if (i == leapMonth) {
                names.add("闰" + LUNAR_MONTH_NAME[i - 1])
            }
        }
        return names
    }

    fun getLunarDayNames(year: Int, month: Int, isLeap: Boolean): List<String> {
        val days = if (isLeap) getLeapMonthDays(year) else getLunarMonthDays(year, month)
        return LUNAR_DAY_NAME.take(days)
    }

    /**
     * 预览文字格式化
     * isLunarMode: 当前选择器处于什么模式。
     * 如果是农历模式，预览区显示公历信息；如果是公历模式，预览区显示农历信息。
     */
    fun formatPreviewText(date: LocalDate, isLunarMode: Boolean): String {
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
        val weekNumber = date.get(weekFields.weekOfYear())
        val dayOfWeek = when (date.dayOfWeek.value) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }

        return if (isLunarMode) {
            // 农历模式下，预览显示公历：2017-06-01 [第25周] 周五
            val solarFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            "${date.format(solarFormatter)} [第${weekNumber}周] $dayOfWeek"
        } else {
            // 公历模式下，预览显示农历：2017年五月初九 [第25周] 周五
            val lunar = fromSolar(date) ?: return ""
            "${lunar.year}年${lunar.lunarMonthName}${lunar.lunarDayName} [第${weekNumber}周] $dayOfWeek"
        }
    }

    fun getWeekOfYear(date: LocalDate): Int {
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
        return date.get(weekFields.weekOfYear())
    }
}
