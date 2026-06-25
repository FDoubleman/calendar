package com.fmm.calendar.data.local

import androidx.room.Dao
import androidx.room.Query

@Dao
interface CalendarDao {
    // 按月查询用于绘制月历网格。数据库已有 year/month 索引，查询速度会比较稳。
    @Query(
        """
        SELECT *
        FROM calendar_day
        WHERE year = :year AND month = :month
        ORDER BY solar_date
        """
    )
    suspend fun getMonthDays(year: Int, month: Int): List<CalendarDayEntity>

    // 按天查询用于点击日期后刷新详情卡片。
    @Query(
        """
        SELECT *
        FROM calendar_day
        WHERE solar_date = :solarDate
        LIMIT 1
        """
    )
    suspend fun getDay(solarDate: String): CalendarDayEntity?

    // 当 jieqi 字段解析不到“距下一个节气”时，用这个查询向后找下一条带节气名称的数据。
    @Query(
        """
        SELECT *
        FROM calendar_day
        WHERE solar_date > :solarDate AND jieqi != ''
        ORDER BY solar_date
        LIMIT 1
        """
    )
    suspend fun getNextJieqiDay(solarDate: String): CalendarDayEntity?
}
