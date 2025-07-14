package com.wendy.face.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 人脸检测引导组件
 * 显示一个相机镜头样式的圆形区域来引导用户放置人脸，并提供提示信息。
 * 经过修改，引导区域横向铺满屏幕。
 */
@Composable
fun FaceDetectionGuide() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 将半径设置为宽度的一半，使圆形横向铺满
        val circleRadiusDp = maxWidth / 2f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = circleRadiusDp.toPx()
            val center = size.center

            // 1. 绘制主圆环
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // 2. 绘制内圈
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = radius - 10.dp.toPx(), // 内圈半径稍小
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. 绘制四个角的标记 (修正并放置在圆环内)
            val cornerLength = 25f // 角标记的弧度
            val cornerRadius = radius - 18.dp.toPx() // 将角标记向内偏移
            val strokeWidth = 4.dp.toPx()
            val topLeftOffset = Offset(center.x - cornerRadius, center.y - cornerRadius)
            val cornerSize = Size(cornerRadius * 2, cornerRadius * 2)

            // Top Left
            drawArc(color = Color.White, startAngle = 180f, sweepAngle = cornerLength, useCenter = false, style = Stroke(strokeWidth), topLeft = topLeftOffset, size = cornerSize)
            drawArc(color = Color.White, startAngle = 270f, sweepAngle = -cornerLength, useCenter = false, style = Stroke(strokeWidth), topLeft = topLeftOffset, size = cornerSize)

            // Top Right
            drawArc(color = Color.White, startAngle = 270f, sweepAngle = cornerLength, useCenter = false, style = Stroke(strokeWidth), topLeft = topLeftOffset, size = cornerSize)
            drawArc(color = Color.White, startAngle = 0f, sweepAngle = -cornerLength, useCenter = false, style = Stroke(strokeWidth), topLeft = topLeftOffset, size = cornerSize)

            // Bottom Right
            drawArc(color = Color.White, startAngle = 0f, sweepAngle = cornerLength, useCenter = false, style = Stroke(strokeWidth), topLeft = topLeftOffset, size = cornerSize)
            drawArc(color = Color.White, startAngle = 90f, sweepAngle = -cornerLength, useCenter = false, style = Stroke(strokeWidth), topLeft = topLeftOffset, size = cornerSize)

            // Bottom Left
            drawArc(color = Color.White, startAngle = 90f, sweepAngle = cornerLength, useCenter = false, style = Stroke(strokeWidth), topLeft = topLeftOffset, size = cornerSize)
            drawArc(color = Color.White, startAngle = 180f, sweepAngle = -cornerLength, useCenter = false, style = Stroke(strokeWidth), topLeft = topLeftOffset, size = cornerSize)
        }

        // 在圆形上方添加提示
        Text(
            text = "[ 请将脸放于圆圈中，按下底部拍照按钮 ]",
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxOf(0.dp, (maxHeight / 2) - circleRadiusDp - 60.dp)) // 确保 padding 不为负数
        )
    }
}
