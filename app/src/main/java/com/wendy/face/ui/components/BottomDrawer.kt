package com.wendy.face.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * 底部抽屉状态
 */
enum class BottomDrawerState {
    COLLAPSED,    // 收起状态
    EXPANDED,     // 展开状态
    HIDDEN        // 完全隐藏
}

/**
 * 底部抽屉组件
 * 支持自下到上滑动、拖拽控制、背景遮罩等功能
 */
@Composable
fun BottomDrawer(
    drawerState: BottomDrawerState,
    onStateChange: (BottomDrawerState) -> Unit,
    modifier: Modifier = Modifier,
    collapsedHeight: Float = 80f, // 收起时的高度（dp）
    expandedHeightRatio: Float = 0.7f, // 展开时占屏幕高度的比例
    content: @Composable BoxScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeight.toPx() }
    
    // 计算各种高度
    val collapsedHeightPx = with(density) { collapsedHeight.dp.toPx() }
    val expandedHeightPx = screenHeightPx * expandedHeightRatio

    // 动画状态 - 从底部向上滑出的偏移量
    val animatedOffset by animateFloatAsState(
        targetValue = when (drawerState) {
            BottomDrawerState.HIDDEN -> expandedHeightPx // 完全隐藏到屏幕下方
            BottomDrawerState.COLLAPSED -> expandedHeightPx - collapsedHeightPx // 只显示标题栏高度
            BottomDrawerState.EXPANDED -> 0f // 完全展开
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "drawer_offset"
    )
    
    // 背景遮罩透明度
    val backgroundAlpha by animateFloatAsState(
        targetValue = when (drawerState) {
            BottomDrawerState.EXPANDED -> 0.5f
            else -> 0f
        },
        animationSpec = tween(300),
        label = "background_alpha"
    )
    
    // 拖拽状态
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 背景遮罩（仅在展开时显示）
        if (backgroundAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = backgroundAlpha))
                    .pointerInput(Unit) {
                        detectDragGestures { _, _ ->
                            // 点击背景时收起抽屉
                            if (drawerState == BottomDrawerState.EXPANDED) {
                                onStateChange(BottomDrawerState.COLLAPSED)
                            }
                        }
                    }
            )
        }
        
        // 抽屉主体 - 固定高度，通过offset控制显示
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    with(density) {
                        // 始终保持展开时的高度，通过offset控制显示
                        (expandedHeightPx / density.density).dp
                    }
                )
                .offset {
                    IntOffset(
                        0,
                        (animatedOffset + dragOffset).roundToInt()
                    )
                }
                .align(Alignment.BottomCenter)
                .zIndex(10f)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color.Black.copy(alpha = 0.9f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            
                            // 根据拖拽距离和速度决定最终状态
                            val finalOffset = animatedOffset + dragOffset
                            val threshold = collapsedHeightPx / 2
                            
                            when (drawerState) {
                                BottomDrawerState.COLLAPSED -> {
                                    if (dragOffset < -threshold) {
                                        onStateChange(BottomDrawerState.EXPANDED)
                                    } else if (dragOffset > threshold) {
                                        onStateChange(BottomDrawerState.HIDDEN)
                                    }
                                }
                                BottomDrawerState.EXPANDED -> {
                                    if (dragOffset > threshold) {
                                        onStateChange(BottomDrawerState.COLLAPSED)
                                    }
                                }
                                BottomDrawerState.HIDDEN -> {
                                    if (dragOffset < -threshold) {
                                        onStateChange(BottomDrawerState.COLLAPSED)
                                    }
                                }
                            }
                            
                            dragOffset = 0f
                        }
                    ) { _, dragAmount ->
                        dragOffset += dragAmount.y
                        
                        // 限制拖拽范围
                        val maxDragUp = when (drawerState) {
                            BottomDrawerState.COLLAPSED -> expandedHeightPx - collapsedHeightPx // 可以向上拖拽到展开状态
                            BottomDrawerState.EXPANDED -> 0f // 已经完全展开，不能再向上
                            BottomDrawerState.HIDDEN -> expandedHeightPx // 可以向上拖拽到收起状态
                        }

                        val maxDragDown = when (drawerState) {
                            BottomDrawerState.COLLAPSED -> collapsedHeightPx // 可以向下拖拽到隐藏状态
                            BottomDrawerState.EXPANDED -> expandedHeightPx - collapsedHeightPx // 可以向下拖拽到收起状态
                            BottomDrawerState.HIDDEN -> 0f // 已经隐藏，不能再向下
                        }

                        dragOffset = dragOffset.coerceIn(-maxDragUp, maxDragDown)
                    }
                }
        ) {
            Column {
                // 拖拽手柄和标题栏
                DrawerHandle(
                    drawerState = drawerState,
                    onToggle = {
                        when (drawerState) {
                            BottomDrawerState.HIDDEN -> onStateChange(BottomDrawerState.COLLAPSED)
                            BottomDrawerState.COLLAPSED -> onStateChange(BottomDrawerState.EXPANDED)
                            BottomDrawerState.EXPANDED -> onStateChange(BottomDrawerState.COLLAPSED)
                        }
                    },
                    modifier = Modifier.height(collapsedHeight.dp)
                )
                
                // 内容区域（仅在展开时显示）
                if (drawerState == BottomDrawerState.EXPANDED) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * 抽屉拖拽手柄和标题栏
 */
@Composable
private fun DrawerHandle(
    drawerState: BottomDrawerState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures { _, _ ->
                    onToggle()
                }
            },
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 拖拽指示条
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        Color.White.copy(alpha = 0.5f),
                        RoundedCornerShape(2.dp)
                    )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 标题和展开/收起按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (drawerState) {
                        BottomDrawerState.HIDDEN -> "显示分析报告"
                        BottomDrawerState.COLLAPSED -> "分析报告"
                        BottomDrawerState.EXPANDED -> "分析报告"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = when (drawerState) {
                            BottomDrawerState.HIDDEN -> Icons.Default.ExpandLess
                            BottomDrawerState.COLLAPSED -> Icons.Default.ExpandLess
                            BottomDrawerState.EXPANDED -> Icons.Default.ExpandMore
                        },
                        contentDescription = when (drawerState) {
                            BottomDrawerState.HIDDEN -> "显示分析报告"
                            BottomDrawerState.COLLAPSED -> "展开分析报告"
                            BottomDrawerState.EXPANDED -> "收起分析报告"
                        },
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
