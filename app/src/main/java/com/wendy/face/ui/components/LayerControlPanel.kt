package com.wendy.face.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 图层控制面板组件
 * 用于控制各种图层的显示/隐藏
 */
@Composable
fun LayerControlPanel(
    showPhoto: Boolean,
    showAnalysisReport: Boolean,
    showContours: Boolean,
    showAllKeypoints: Boolean,
    onPhotoToggle: (Boolean) -> Unit,
    onAnalysisReportToggle: (Boolean) -> Unit,
    onContoursToggle: (Boolean) -> Unit,
    onAllKeypointsToggle: (Boolean) -> Unit,
    showThreeCourtFiveEye: Boolean,
    onThreeCourtFiveEyeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 照片图层
        LayerToggleButton(
            icon = Icons.Default.Image,
            isEnabled = showPhoto,
            onClick = { onPhotoToggle(!showPhoto) }
        )

        // 分析报告图层
        LayerToggleButton(
            icon = Icons.Default.Analytics,
            isEnabled = showAnalysisReport,
            onClick = { onAnalysisReportToggle(!showAnalysisReport) }
        )

        // 轮廓图层 (面部 + 五官)
        LayerToggleButton(
            icon = Icons.Default.Face,
            isEnabled = showContours,
            onClick = { onContoursToggle(!showContours) }
        )

        // 所有关键点图层 (绿色点)
        LayerToggleButton(
            icon = Icons.Default.Visibility,
            isEnabled = showAllKeypoints,
            onClick = { onAllKeypointsToggle(!showAllKeypoints) }
        )
       // 三庭五眼图层
       LayerToggleButton(
           icon = Icons.Default.Schema,
           isEnabled = showThreeCourtFiveEye,
           onClick = { onThreeCourtFiveEyeToggle(!showThreeCourtFiveEye) }
       )
    }
}

/**
 * 图层切换按钮
 */
@Composable
private fun LayerToggleButton(
    icon: ImageVector,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isEnabled) Color.White else Color.Red,
            modifier = Modifier.size(20.dp)
        )
    }
}
