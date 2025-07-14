package com.wendy.face.detection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.facemesh.FaceMesh

/**
 * 人脸网格渲染器
 * 负责在Canvas上绘制人脸网格检测结果
 */
object FaceRenderer {

    /**
     * 绘制人脸网格
     * @param drawScope 绘制作用域
     * @param faceMeshes 检测到的人脸网格列表
     * @param offsetX X轴偏移
     * @param offsetY Y轴偏移
     * @param scaleX X轴缩放比例
     * @param scaleY Y轴缩放比例
     * @param isBackCamera 是否是后置摄像头
     * @param imageWidth 图像宽度
     */
    fun drawFaceMeshes(
        drawScope: DrawScope,
        faceMeshes: List<FaceMesh>,
        offsetX: Float,
        offsetY: Float,
        scaleX: Float,
        scaleY: Float,
        isBackCamera: Boolean,
        imageWidth: Int
    ) {
        with(drawScope) {
            for (faceMesh in faceMeshes) {
                // 绘制所有468个关键点
                drawFaceMeshPoints(faceMesh, offsetX, offsetY, scaleX, scaleY, isBackCamera, imageWidth)
            }
        }
    }

    /**
     * 绘制人脸网格的关键点
     */
    private fun DrawScope.drawFaceMeshPoints(
        faceMesh: FaceMesh,
        offsetX: Float,
        offsetY: Float,
        scaleX: Float,
        scaleY: Float,
        isBackCamera: Boolean,
        imageWidth: Int
    ) {
        faceMesh.allPoints.forEach { point ->
            val pos = point.position
            val x = if (isBackCamera) {
                offsetX + pos.x * scaleX
            } else {
                // 前置摄像头需要水平翻转X坐标
                offsetX + (imageWidth - pos.x) * scaleX
            }
            val y = offsetY + pos.y * scaleY

            drawCircle(
                color = Color.Cyan,
                center = Offset(x, y),
                radius = 2.dp.toPx()
            )
        }
    }
}
