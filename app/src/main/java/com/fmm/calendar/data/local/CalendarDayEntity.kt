package com.fmm.calendar.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Entity 是 Room 对数据库表的 Kotlin 映射。
// tableName 必须和 calendar.db 里的真实表名一致，否则 Room 查询不到预置数据库里的数据。
@Entity(
    tableName = "calendar_day",
    indices = [Index(value = ["year", "month"], name = "idx_calendar_day_year_month")]
)
data class CalendarDayEntity(
    @PrimaryKey
    @ColumnInfo(name = "solar_date")
    val solarDate: String, // 回退到 String，因为 kapt 不允许 PrimaryKey 为 null
    val year: Int,
    val month: Int,
    val day: Int,
    @ColumnInfo(name = "weekday_cn")
    val weekdayCn: String,
    @ColumnInfo(name = "weekday_num")
    val weekdayNum: Int,
    @ColumnInfo(name = "is_weekend")
    val isWeekend: Int,
    @ColumnInfo(name = "lunar_date")
    val lunarDate: String,
    @ColumnInfo(name = "ganzhi_year")
    val ganzhiYear: String,
    @ColumnInfo(name = "ganzhi_month")
    val ganzhiMonth: String,
    @ColumnInfo(name = "ganzhi_day")
    val ganzhiDay: String,
    @ColumnInfo(defaultValue = "")
    val zodiac: String,
    @ColumnInfo(defaultValue = "")
    val constellation: String,
    @ColumnInfo(name = "pengzu_baiji", defaultValue = "")
    val pengzuBaiji: String,
    @ColumnInfo(name = "taishen_direction", defaultValue = "")
    val taishenDirection: String,
    @ColumnInfo(name = "year_wuxing", defaultValue = "")
    val yearWuxing: String,
    @ColumnInfo(defaultValue = "")
    val season: String,
    @ColumnInfo(name = "month_wuxing", defaultValue = "")
    val monthWuxing: String,
    @ColumnInfo(defaultValue = "")
    val xingxiu: String,
    @ColumnInfo(name = "day_wuxing", defaultValue = "")
    val dayWuxing: String,
    @ColumnInfo(defaultValue = "")
    val jieqi: String,
    @ColumnInfo(defaultValue = "")
    val chong: String,
    @ColumnInfo(defaultValue = "")
    val sha: String,
    @ColumnInfo(defaultValue = "")
    val liuyao: String,
    @ColumnInfo(name = "shier_shen", defaultValue = "")
    val shierShen: String,
    @ColumnInfo(name = "festivals_text", defaultValue = "")
    val festivalsText: String,
    @ColumnInfo(name = "yi_text", defaultValue = "")
    val yiText: String,
    @ColumnInfo(name = "ji_text", defaultValue = "")
    val jiText: String,
    @ColumnInfo(name = "source_url", defaultValue = "")
    val sourceUrl: String,
    @ColumnInfo(name = "fetched_at", defaultValue = "")
    val fetchedAt: String,
)
