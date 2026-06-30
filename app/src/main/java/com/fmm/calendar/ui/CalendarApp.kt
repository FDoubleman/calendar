package com.fmm.calendar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fmm.calendar.ui.calendar.CalendarScreen
import com.fmm.calendar.ui.theme.CalendarTheme
import com.fmm.calendar.ui.weather.WeatherScreen

// BottomTab 把底部导航需要的信息收拢在一起。
// 这样以后新增/修改 tab 时，不需要到处找标题、route 和图标。
private data class BottomTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(
        route = "calendar",
        title = "日历",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
    ),
    BottomTab(
        route = "weather",
        title = "天气",
        selectedIcon = Icons.Filled.Cloud,
        unselectedIcon = Icons.Outlined.Cloud,
    ),
    BottomTab(
        route = "schedule",
        title = "日程",
        selectedIcon = Icons.Filled.EventNote,
        unselectedIcon = Icons.Outlined.EventNote,
    ),
    BottomTab(
        route = "profile",
        title = "我的",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    ),
)

@Composable
fun CalendarApp() {
    // NavController 是 Navigation Compose 的核心对象，负责保存当前页面、处理跳转和返回栈。
    val navController = rememberNavController()

    // currentBackStackEntryAsState 会把当前导航位置转换成 Compose State。
    // 当 route 改变时，读取它的 NavigationBarItem 会自动重组，从而更新选中状态。
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Scaffold 是 Material Design 常见页面骨架：顶部栏、内容区、底部栏等都可以放在这里。
    Scaffold(
        bottomBar = {
            NavigationBar {
                // 遍历定义的底部标签列表，为每个标签创建一个导航项
                bottomTabs.forEach { tab ->
                    // 判断当前标签是否被选中
                    // currentDestination 表示当前所在页面。hierarchy 会检查导航层级，
                    // 确保即使在子路由中，对应的父级 Tab 也能保持选中状态。
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { destination -> destination.route == tab.route } == true
                    // 在NavigationBar 中创建，多个 Tab 选项
                    NavigationBarItem(
                        selected = selected, // 设置选中状态
                        onClick = {
                            // 执行导航跳转
                            navController.navigate(tab.route) {
                                // 1. popUpTo: 将返回栈弹出到图表的起始目的地（通常是首页）。
                                // 这样可以避免用户在不同 Tab 之间切换时，返回栈堆积大量重复页面。
                                popUpTo(navController.graph.startDestinationId) {
                                    // 保存被弹出页面的状态，以便之后恢复。
                                    saveState = true
                                }

                                // 2. launchSingleTop: 开启单栈顶模式。
                                // 如果当前已经在该 Tab 页面，再次点击不会在栈顶重复创建新实例。
                                launchSingleTop = true

                                // 3. restoreState: 恢复之前保存的状态。
                                // 当用户切回之前访问过的 Tab 时，之前的滚动位置或输入内容等会被还原。
                                restoreState = true
                            }
                        },
                        icon = {
                            // 根据是否选中，动态切换“实心”或“描边”图标
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title, // 提供给无障碍服务的描述
                            )
                        },
                        label = {
                            // 显示标签的标题文字
                            Text(text = tab.title)
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        // innerPadding 来自 Scaffold，用来避开底部导航栏等固定区域，防止内容被遮挡。
        AppNavHost(
            contentPadding = innerPadding,
            navController = navController,
        )
    }
}

// 总结：
// NavigationBarItem 发出 “去哪儿” 的指令，
// NavController 负责 “带路”，
// 而 NavHost 则是 “舞台”，根据当前的地址切换台上表演的演员（Screen）。
@Composable
private fun AppNavHost(
    contentPadding: PaddingValues,
    navController: androidx.navigation.NavHostController,
) {
    // NavHost 是页面容器。startDestination 表示 App 打开后默认显示哪个 route。
    NavHost(
        navController = navController,
        startDestination = "calendar",
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        // composable(route) 定义一个页面入口。route 是页面的内部地址，不直接显示给用户。
        composable("calendar") {
            CalendarScreen()
        }
        composable("weather") {
            WeatherScreen()
        }
        composable("schedule") {
            ScheduleScreen()
        }
        composable("profile") {
            ProfileScreen()
        }
    }
}

@Composable
private fun ScheduleScreen() {
    PlaceholderScreen(
        title = "日程",
        message = "这里将逐步实现待办、提醒和日程列表。",
    )
}

@Composable
private fun ProfileScreen() {
    PlaceholderScreen(
        title = "我的",
        message = "这里将逐步放置偏好设置、账号信息和应用配置。",
    )
}

@Composable
private fun PlaceholderScreen(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    // Surface 使用主题里的 background/surface 颜色，保证页面在浅色和深色模式下都自然。
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarAppPreview() {
    CalendarTheme {
        CalendarApp()
    }
}
