package com.fmm.calendar.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3,
    itemHeight: Dp = 48.dp,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // 使用 rememberUpdatedState 解决闭包捕获 stale state 的问题
    val currentItemSelected by rememberUpdatedState(onItemSelected)
    val currentItems by rememberUpdatedState(items)

    // 支持外部索引更新（如“回到今天”）
    // 仅当用户不在滚动且索引确实变化时才同步，避免与用户手势冲突
    LaunchedEffect(initialIndex) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != initialIndex) {
            listState.scrollToItem(initialIndex)
        }
    }
    
    // 监听滚动停止
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { isScrolling ->
                if (!isScrolling) {
                    val centerIndex = listState.firstVisibleItemIndex
                    if (centerIndex in currentItems.indices) {
                        currentItemSelected(centerIndex)
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2))
        ) {
            items(items.size) { index ->
                val isSelected by remember {
                    derivedStateOf { listState.firstVisibleItemIndex == index }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = if (isSelected) 20.sp else 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) textColor else textColor.copy(alpha = 0.4f)
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
