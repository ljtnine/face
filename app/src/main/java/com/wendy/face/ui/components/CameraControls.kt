package com.wendy.face.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 相机控制面板组件
 * 包含拍照、切换摄像头、从相册选择图片等功能按钮
 */
@Composable
fun CameraControls(
    onCameraSwitch: () -> Unit,
    onCapture: () -> Unit,
    onGalleryClick: () -> Unit
) {
    // 使用扁平化、现代风格的控制面板
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f)) // 半透明背景
            .padding(vertical = 24.dp, horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：相机反转按钮
        IconButton(
            onClick = {
                Log.d("CameraControls", "Camera switch button clicked")
                onCameraSwitch()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 中间：拍照按钮
        FloatingActionButton(
            onClick = {
                Log.d("CameraControls", "Capture button clicked")
                onCapture()
            },
            modifier = Modifier
                .size(80.dp)
                .shadow(8.dp, CircleShape),
            containerColor = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📸",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White
                    )
                )
            }
        }

        // 右侧：相册选择按钮
        IconButton(
            onClick = {
                Log.d("CameraControls", "Gallery button clicked")
                onGalleryClick()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Open Gallery",
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
