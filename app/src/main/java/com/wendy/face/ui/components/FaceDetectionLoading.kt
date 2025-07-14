package com.wendy.face.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * 检测状态枚举
 */
enum class DetectionState {
    PREPROCESSING,    // 图像预处理
    DETECTING,       // 人脸检测中
    ANALYZING,       // 面相分析中
    COMPLETED        // 检测完成
}

/**
 * 人脸检测Loading组件
 * 提供优雅的检测过程动画效果
 */
@Composable
fun FaceDetectionLoading(
    capturedBitmap: android.graphics.Bitmap,
    detectionState: DetectionState = DetectionState.DETECTING,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    // 动画状态
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    
    // 扫描线动画
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_progress"
    )
    
    // 脉冲动画
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // 旋转动画
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )
    
    // 透明度动画
    val alphaAnimation by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 背景图片（稍微暗化）
        Image(
            bitmap = capturedBitmap.asImageBitmap(),
            contentDescription = "Captured Photo",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.7f),
            contentScale = ContentScale.Crop
        )
        
        // 暗化遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )
        
        // 主要Loading内容
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            
            // 面部扫描动画区域
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // 扫描框动画
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawFaceScanAnimation(
                        scanProgress = scanProgress,
                        rotationAngle = rotationAngle,
                        alpha = alphaAnimation
                    )
                }
                
                // 中心图标
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.8f),
                                    Color(0xFF2196F3).copy(alpha = 0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👤",
                        fontSize = 32.sp,
                        modifier = Modifier.alpha(alphaAnimation)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 进度指示器
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .width(200.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 状态文字
            Text(
                text = getStateText(detectionState),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alphaAnimation)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 提示文字
            Text(
                text = getHintText(detectionState),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * 绘制面部扫描动画
 */
private fun DrawScope.drawFaceScanAnimation(
    scanProgress: Float,
    rotationAngle: Float,
    alpha: Float
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 * 0.8f
    
    // 外圈旋转环
    drawCircle(
        color = Color(0xFF2196F3).copy(alpha = alpha * 0.6f),
        radius = radius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
    
    // 内圈脉冲环
    drawCircle(
        color = Color(0xFF4CAF50).copy(alpha = alpha * 0.8f),
        radius = radius * 0.7f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    
    // 扫描线
    val scanY = center.y - radius + (2 * radius * scanProgress)
    drawLine(
        color = Color(0xFFFFEB3B).copy(alpha = alpha),
        start = Offset(center.x - radius * 0.8f, scanY),
        end = Offset(center.x + radius * 0.8f, scanY),
        strokeWidth = 2.dp.toPx()
    )
    
    // 旋转的检测点
    for (i in 0..7) {
        val angle = (rotationAngle + i * 45) * PI / 180
        val pointRadius = radius * 0.9f
        val pointX = center.x + pointRadius * cos(angle).toFloat()
        val pointY = center.y + pointRadius * sin(angle).toFloat()
        
        drawCircle(
            color = Color(0xFF4CAF50).copy(alpha = alpha * 0.7f),
            radius = 4.dp.toPx(),
            center = Offset(pointX, pointY)
        )
    }
    
    // 中心十字线
    val crossSize = 20.dp.toPx()
    drawLine(
        color = Color.White.copy(alpha = alpha * 0.8f),
        start = Offset(center.x - crossSize, center.y),
        end = Offset(center.x + crossSize, center.y),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = Color.White.copy(alpha = alpha * 0.8f),
        start = Offset(center.x, center.y - crossSize),
        end = Offset(center.x, center.y + crossSize),
        strokeWidth = 1.dp.toPx()
    )
}

/**
 * 获取状态文字
 */
private fun getStateText(state: DetectionState): String {
    return when (state) {
        DetectionState.PREPROCESSING -> "图像预处理中..."
        DetectionState.DETECTING -> "人脸检测中..."
        DetectionState.ANALYZING -> "面相分析中..."
        DetectionState.COMPLETED -> "分析完成"
    }
}

/**
 * 获取提示文字
 */
private fun getHintText(state: DetectionState): String {
    return when (state) {
        DetectionState.PREPROCESSING -> "正在优化图像质量以提高检测精度"
        DetectionState.DETECTING -> "正在定位面部关键点，请稍候"
        DetectionState.ANALYZING -> "正在分析十二宫相学特征"
        DetectionState.COMPLETED -> "检测分析已完成"
    }
}

/**
 * 简化版Loading组件（用于快速显示）
 */
@Composable
fun SimpleFaceDetectionLoading(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "simple_loading")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "simple_rotation"
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { rotationZ = rotationAngle },
                color = Color(0xFF4CAF50),
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "检测中...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
