package com.wendy.face.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendy.face.analyzer.PalaceAnalysisResult
import com.wendy.face.model.ThreeCourtFiveEyeResult
import kotlinx.coroutines.CoroutineScope

/**
 * 分析报告内容组件
 * 包含三庭五眼分析、十二宫分析和AI命格分析
 */
@Composable
fun AnalysisReportContent(
    analysisResults: List<PalaceAnalysisResult>,
    threeCourtFiveEyeResult: ThreeCourtFiveEyeResult?,
    destinyText: String,
    isAnalyzing: Boolean,
    showDestinyResult: Boolean,
    onDestinyAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    // 当开始分析或有分析结果时，自动滚动到AI分析区域
    LaunchedEffect(isAnalyzing, showDestinyResult) {
        if (isAnalyzing || showDestinyResult) {
            // 计算AI分析item的索引
            var itemIndex = 0
            if (analysisResults.isNotEmpty()) {
                if (threeCourtFiveEyeResult != null) itemIndex++ // 三庭五眼分析
                if (!showDestinyResult) itemIndex++ // 十二宫分析
                itemIndex++ // AI命格分析
            }
            // 滚动到AI分析区域
            listState.animateScrollToItem(itemIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 检测状态信息
        if (analysisResults.isEmpty()) {
            item {
                EmptyStateCard()
            }
        } else {
            // 三庭五眼分析结果
            threeCourtFiveEyeResult?.let { result ->
                item {
                    AnalysisSection(
                        title = "三庭五眼分析",
                        icon = Icons.Default.Face,
                        iconColor = Color(0xFF4CAF50)
                    ) {
                        ThreeCourtFiveEyeCard(result = result)
                    }
                }
            }
            
            // 十二宫分析结果
            if (!showDestinyResult) {
                item {
                    AnalysisSection(
                        title = "十二宫相学分析",
                        icon = Icons.Default.Analytics,
                        iconColor = Color(0xFF2196F3)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            analysisResults.forEach { result ->
                                PalaceAnalysisCard(
                                    palaceName = result.palaceName,
                                    description = result.description
                                )
                            }
                        }
                    }
                }
            }
            
            // AI命格分析按钮和结果
            item {
                AnalysisSection(
                    title = "AI命格推演",
                    icon = Icons.Default.Psychology,
                    iconColor = Color(0xFF9C27B0)
                ) {
                    DestinyAnalysisCard(
                        destinyText = destinyText,
                        isAnalyzing = isAnalyzing,
                        showDestinyResult = showDestinyResult,
                        onDestinyAnalysis = onDestinyAnalysis
                    )
                }
            }
        }
    }
}

/**
 * 分析区块组件
 */
@Composable
private fun AnalysisSection(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 区块标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 区块内容
            content()
        }
    }
}

/**
 * 空状态卡片
 */
@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "未检测到人脸",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "请确保照片中有清晰的人脸，然后点击\"重新分析\"",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * 三庭五眼分析卡片
 */
@Composable
private fun ThreeCourtFiveEyeCard(result: ThreeCourtFiveEyeResult) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 三庭分析
        FeatureCard(
            title = "三庭比例",
            description = result.threeCourt.description
        )
        
        // 五眼分析
        FeatureCard(
            title = "五眼比例",
            description = result.fiveEye.description
        )
    }
}

/**
 * 宫位分析卡片
 */
@Composable
private fun PalaceAnalysisCard(
    palaceName: String,
    description: String
) {
    FeatureCard(
        title = palaceName,
        description = description
    )
}

/**
 * 特征分析卡片（通用）
 */
@Composable
private fun FeatureCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 18.sp
        )
    }
}

/**
 * AI命格分析卡片
 */
@Composable
private fun DestinyAnalysisCard(
    destinyText: String,
    isAnalyzing: Boolean,
    showDestinyResult: Boolean,
    onDestinyAnalysis: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI分析按钮
        if (!showDestinyResult) {
            Button(
                onClick = onDestinyAnalysis,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6)
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isAnalyzing) "AI大师分析中..." else "开始AI命格推演",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // AI分析结果
        if (destinyText.isNotEmpty()) {
            val scrollState = rememberScrollState()

            // 当内容更新时自动滚动到底部，确保用户能看到最新输出
            LaunchedEffect(destinyText) {
                if (destinyText.isNotEmpty()) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    text = destinyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                )
            }
        }
        
        // 分析中状态
        if (isAnalyzing && destinyText.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFF8B5CF6),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "正在连接AI大师，请稍候...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
