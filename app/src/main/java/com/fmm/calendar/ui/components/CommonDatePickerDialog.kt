package com.fmm.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fmm.calendar.util.LunarCalendarHelper
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate

@Preview(showBackground = true)
@Composable
fun PreviewCommonDatePickerDialog() {
    CommonDatePickerDialog(
        initialDate = LocalDate.now(),
        onDismiss = {},
        onConfirm = {}
    )
}

@Composable
fun CommonDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var isLunarMode by remember { mutableStateOf(false) }
    
    // 预览文字，只有当滚动结束时才更新
    var previewText by remember { mutableStateOf(LunarCalendarHelper.formatPreviewText(initialDate, false)) }

    // 当模式切换时，立即刷新预览文字
    LaunchedEffect(isLunarMode) {
        previewText = LunarCalendarHelper.formatPreviewText(selectedDate, isLunarMode)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部工具栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "回到今天",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            selectedDate = LocalDate.now()
                            previewText = LunarCalendarHelper.formatPreviewText(selectedDate, isLunarMode)
                        }
                    )
                    
                    LunarSolarSwitch(
                        isLunar = isLunarMode,
                        onToggle = { isLunarMode = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 日期预览区
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                
                // 滚轮区
                DateWheelGroup(
                    date = selectedDate,
                    isLunar = isLunarMode,
                    onDateChanged = { newDate ->
                        selectedDate = newDate
                        // 滚动结束后更新预览文字（逻辑在 WheelPicker 的 onItemSelected 中触发，
                        // 这里直接更新 previewText 即可，因为调用端确保了 Settled 后才触发）
                        previewText = LunarCalendarHelper.formatPreviewText(newDate, isLunarMode)
                    }
                )
                
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                
                // 底部按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "取消", color = Color.Gray, fontSize = 18.sp)
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(24.dp)
                            .background(Color.LightGray)
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onConfirm(selectedDate) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "确定", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LunarSolarSwitch(
    isLunar: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .padding(2.dp)
    ) {
        val activeColor = Color.Red
        val inactiveColor = Color.Transparent
        val activeTextColor = Color.White
        val inactiveTextColor = Color.Gray

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (!isLunar) activeColor else inactiveColor)
                .clickable { onToggle(false) }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "公历", color = if (!isLunar) activeTextColor else inactiveTextColor, fontSize = 12.sp)
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isLunar) activeColor else inactiveColor)
                .clickable { onToggle(true) }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "农历", color = if (isLunar) activeTextColor else inactiveTextColor, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DateWheelGroup(
    date: LocalDate,
    isLunar: Boolean,
    onDateChanged: (LocalDate) -> Unit
) {
    if (isLunar) {
        LunarDateWheels(date, onDateChanged)
    } else {
        SolarDateWheels(date, onDateChanged)
    }
}

@Composable
private fun SolarDateWheels(
    date: LocalDate,
    onDateChanged: (LocalDate) -> Unit
) {
    val years = (1900..2100).map { "${it}年" }
    val months = (1..12).map { "${it}月" }
    
    val daysInMonth = date.lengthOfMonth()
    val days = (1..daysInMonth).map { "${it}日" }

    Row(modifier = Modifier.fillMaxWidth()) {
        WheelPicker(
            items = years,
            initialIndex = date.year - 1900,
            onItemSelected = { index ->
                val newYear = 1900 + index
                val maxDays = LocalDate.of(newYear, date.monthValue, 1).lengthOfMonth()
                val newDay = date.dayOfMonth.coerceAtMost(maxDays)
                onDateChanged(LocalDate.of(newYear, date.monthValue, newDay))
            },
            modifier = Modifier.weight(1f)
        )
        WheelPicker(
            items = months,
            initialIndex = date.monthValue - 1,
            onItemSelected = { index ->
                val newMonth = index + 1
                val maxDays = LocalDate.of(date.year, newMonth, 1).lengthOfMonth()
                val newDay = date.dayOfMonth.coerceAtMost(maxDays)
                onDateChanged(LocalDate.of(date.year, newMonth, newDay))
            },
            modifier = Modifier.weight(1f)
        )
        WheelPicker(
            items = days,
            initialIndex = date.dayOfMonth - 1,
            onItemSelected = { index ->
                onDateChanged(date.withDayOfMonth(index + 1))
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LunarDateWheels(
    date: LocalDate,
    onDateChanged: (LocalDate) -> Unit
) {
    // 使用 remember 存储上一次稳定的农历状态，避免滚动年份时因公历转换导致的月份跳变
    val initialLunar = remember(date) { LunarCalendarHelper.fromSolar(date) } ?: return
    
    // 我们需要一个局部的“正在编辑的农历状态”，只有当用户真正操作滚轮时才更新
    var currentYear by remember(date) { mutableIntStateOf(initialLunar.year) }
    var currentMonth by remember(date) { mutableIntStateOf(initialLunar.month) }
    var isLeapMonth by remember(date) { mutableStateOf(initialLunar.isLeap) }
    var currentDay by remember(date) { mutableIntStateOf(initialLunar.day) }

    val years = (1900..2100).map { "${it}年" }
    val lunarMonths = remember(currentYear) { LunarCalendarHelper.getLunarMonthNames(currentYear) }
    val dayNames = remember(currentYear, currentMonth, isLeapMonth) { 
        LunarCalendarHelper.getLunarDayNames(currentYear, currentMonth, isLeapMonth) 
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        WheelPicker(
            items = years,
            initialIndex = currentYear - 1900,
            onItemSelected = { index ->
                val newYear = 1900 + index
                if (newYear != currentYear) {
                    currentYear = newYear
                    // 检查新的一年是否还有当前的闰月，如果没有，则降级为普通月
                    val maxLeap = LunarCalendarHelper.getLeapMonth(newYear)
                    if (isLeapMonth && currentMonth != maxLeap) {
                        isLeapMonth = false
                    }
                    val newSolar = LunarCalendarHelper.toSolar(newYear, currentMonth, currentDay, isLeapMonth)
                    onDateChanged(newSolar)
                }
            },
            modifier = Modifier.weight(1f)
        )
        WheelPicker(
            items = lunarMonths,
            initialIndex = calculateLunarMonthIndex(currentYear, currentMonth, isLeapMonth),
            onItemSelected = { index ->
                val (newMonth, isLeap) = getLunarMonthFromIndex(currentYear, index)
                if (newMonth != currentMonth || isLeap != isLeapMonth) {
                    currentMonth = newMonth
                    isLeapMonth = isLeap
                    val newSolar = LunarCalendarHelper.toSolar(currentYear, newMonth, currentDay, isLeap)
                    onDateChanged(newSolar)
                }
            },
            modifier = Modifier.weight(1f)
        )
        WheelPicker(
            items = dayNames,
            initialIndex = (currentDay - 1).coerceAtMost(dayNames.size - 1),
            onItemSelected = { index ->
                val newDay = index + 1
                if (newDay != currentDay) {
                    currentDay = newDay
                    val newSolar = LunarCalendarHelper.toSolar(currentYear, currentMonth, newDay, isLeapMonth)
                    onDateChanged(newSolar)
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun calculateLunarMonthIndex(year: Int, month: Int, isLeap: Boolean): Int {
    val months = LunarCalendarHelper.getLunarMonthNames(year)
    val targetName = (if (isLeap) "闰" else "") + (if (month == 1) "正月" else if (month == 11) "冬月" else if (month == 12) "腊月" else "${CHINESE_MONTHS[month-1]}月")
    // 实际上 LunarCalendarHelper 已经提供了名称，直接按规则查
    val searchName = (if (isLeap) "闰" else "") + when(month) {
        1 -> "正月"
        11 -> "冬月"
        12 -> "腊月"
        else -> CHINESE_MONTHS[month-1] + "月"
    }
    val idx = months.indexOf(searchName)
    return if (idx >= 0) idx else 0
}

private val CHINESE_MONTHS = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")

private fun getLunarMonthFromIndex(year: Int, index: Int): Pair<Int, Boolean> {
    val names = LunarCalendarHelper.getLunarMonthNames(year)
    val name = names.getOrNull(index) ?: return 1 to false
    val isLeap = name.startsWith("闰")
    val pureName = if (isLeap) name.substring(1) else name
    val month = when(pureName) {
        "正月" -> 1
        "冬月" -> 11
        "腊月" -> 12
        else -> CHINESE_MONTHS.indexOf(pureName.substring(0, 1)) + 1
    }
    return month to isLeap
}
