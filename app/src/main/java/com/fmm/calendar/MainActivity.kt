package com.fmm.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fmm.calendar.ui.CalendarApp
import com.fmm.calendar.ui.theme.CalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 开启边到边显示，让内容可以延伸到系统状态栏/导航栏区域。
        // Material3 的 Scaffold 会通过 innerPadding 帮我们处理主要内容的安全间距。
        enableEdgeToEdge()

        // setContent 是 Compose 应用的入口：从这里开始，传统 View XML 被 Composable 函数替代。
        setContent {
            // Theme 统一管理颜色、字体等视觉规范。后续改 App 风格时，优先改主题而不是每个页面单独改。
            CalendarTheme {
                CalendarApp()
            }
        }
    }
}
