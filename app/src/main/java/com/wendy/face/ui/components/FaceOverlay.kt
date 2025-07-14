package com.wendy.face.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import com.google.mlkit.vision.facemesh.FaceMesh
import com.wendy.face.model.ThreeCourtFiveEyeResult
import com.wendy.face.model.TwelvePalaces
import com.wendy.face.utils.FaceExtractionUtils
import com.wendy.face.utils.FaceKeypointStabilizer
import com.wendy.face.utils.PreciseCoordinateTransform
import com.wendy.face.utils.StabilizedKeypoint

/**
 * 人脸覆盖层组件
 * 在图片或相机预览上绘制人脸检测结果
 * @param faceMeshes 人脸网格数据
 * @param scaleX X轴的缩放比例
 * @param scaleY Y轴的缩放比例
 * @param offsetX X轴的偏移量
 * @param offsetY Y轴的偏移量
 * @param isBackCamera 是否为后置摄像头
 * @param isPreviewMode 是否为相机预览模式（预览模式下不显示绿色关键点和红色人脸框）
 * @param show3DPoints 是否显示3D关键点
 * @param showKeypoints 是否显示绿色关键点
 * @param showFaceContour 是否显示面部轮廓
 * @param showFacialFeatures 是否显示五官连线
 * @param showPalaceMarkers 是否显示宫位标记
 */
@Composable
fun FaceOverlay(
    faceMeshes: List<FaceMesh>,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    isBackCamera: Boolean,
    isPreviewMode: Boolean = false,
    show3DPoints: Boolean = true,
    showAllKeypoints: Boolean = false, // 默认不显示所有绿色关键点
    showContours: Boolean = true,      // 合并了面部轮廓和五官轮廓
    showPalaceMarkers: Boolean = true,
    showThreeCourtFiveEye: Boolean = false,
    threeCourtFiveEyeResult: ThreeCourtFiveEyeResult? = null,
    // 暂时移除复杂参数，专注于基础稳定化
    useStabilizedKeypoints: Boolean = true
) {
    // 添加调试日志
    android.util.Log.d("FaceOverlay", "FaceOverlay called with ${faceMeshes.size} faces, scale: ${scaleX}x${scaleY}, offset: ${offsetX}, ${offsetY}")

    Box(modifier = Modifier.fillMaxSize()) {
        if (faceMeshes.isNotEmpty()) {
            android.util.Log.d("FaceOverlay", "Displaying face points for ${faceMeshes.size} faces")

            // 应用关键点稳定化
            val stabilizedFaceMeshes = if (useStabilizedKeypoints && !isPreviewMode) {
                faceMeshes.map { faceMesh ->
                    val imageWidth = (1000 * scaleX).toInt() // 估算图像尺寸
                    val imageHeight = (1000 * scaleY).toInt()
                    val stabilizedKeypoints = FaceKeypointStabilizer.stabilizeFaceKeypoints(
                        faceMesh, imageWidth, imageHeight
                    )
                    val quality = FaceKeypointStabilizer.evaluateKeypointQuality(stabilizedKeypoints)
                    android.util.Log.d("FaceOverlay", "Keypoint quality: ${quality.overallScore}")
                    faceMesh // 暂时返回原始faceMesh，后续可以创建稳定化版本
                }
            } else {
                faceMeshes
            }

            // 在整个图片区域显示人脸关键点（仅在非预览模式下显示）
            if (!isPreviewMode) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 绘制人脸关键点
                    stabilizedFaceMeshes.forEach { faceMesh ->
                        drawFacePoints(
                            faceMesh = faceMesh,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            isBackCamera = isBackCamera,
                            showAllKeypoints = showAllKeypoints,
                            showContours = showContours,
                            showPalaceMarkers = showPalaceMarkers,
                            showThreeCourtFiveEye = showThreeCourtFiveEye,
                            threeCourtFiveEyeResult = threeCourtFiveEyeResult
                        )
                    }
                }
            }

            // 同时在右下角显示3D关键点预览（仅在需要时显示）
            if (show3DPoints) {
                Face3DPointsDisplay(
                    faceMesh = faceMeshes.first(),
                    isBackCamera = isBackCamera,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 150.dp, end = 16.dp)
                        .size(120.dp)
                )
            }
        } else {
            android.util.Log.d("FaceOverlay", "No faces to display")
        }
    }
}


/**
 * 3D人脸关键点显示组件 - 固定在右下角
 */
@Composable
private fun Face3DPointsDisplay(
    faceMesh: FaceMesh,
    isBackCamera: Boolean,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("Face3DPointsDisplay", "Drawing face points, isBackCamera: $isBackCamera, points: ${faceMesh.allPoints.size}")
    Canvas(modifier = modifier) {
        val viewWidth = size.width
        val viewHeight = size.height
        android.util.Log.d("Face3DPointsDisplay", "Canvas size: ${viewWidth}x${viewHeight}")

        // 在这个独立区域内绘制3D关键点
        drawFace3DPoints(
            faceMesh = faceMesh,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            isBackCamera = isBackCamera
        )
    }
}


/**
 * 在图片上绘制人脸关键点
 */
private fun DrawScope.drawFacePoints(
    faceMesh: FaceMesh,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    isBackCamera: Boolean,
    showAllKeypoints: Boolean,
    showContours: Boolean,
    showPalaceMarkers: Boolean,
    showThreeCourtFiveEye: Boolean,
    threeCourtFiveEyeResult: ThreeCourtFiveEyeResult?
) {
    android.util.Log.d("FaceOverlay", "drawFacePoints - Using provided scale and offset")

    // 绘制所有绿色关键点（仅在启用时显示）
    if (showAllKeypoints) {
        val points = faceMesh.allPoints.map { point ->
            val x = offsetX + point.position.x * scaleX
            val y = offsetY + point.position.y * scaleY
            Offset(x, y)
        }
        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = Color.Green.copy(alpha = 0.8f),
            strokeWidth = 3.dp.toPx()
        )
    }

    // 绘制轮廓（面部轮廓 + 五官轮廓）
    if (showContours) {
        drawFaceContour(
            faceMesh = faceMesh,
            scaleX = scaleX,
            scaleY = scaleY,
            offsetX = offsetX,
            offsetY = offsetY,
            isBackCamera = isBackCamera
        )
        drawFacialFeatures(
            faceMesh = faceMesh,
            scaleX = scaleX,
            scaleY = scaleY,
            offsetX = offsetX,
            offsetY = offsetY,
            isBackCamera = isBackCamera
        )
    }

    // 绘制十二宫位置标注
    if (showPalaceMarkers) {
        drawTwelvePalaces(
            faceMesh = faceMesh,
            scaleX = scaleX,
            scaleY = scaleY,
            offsetX = offsetX,
            offsetY = offsetY,
            isBackCamera = isBackCamera
        )
    }
   // 绘制三庭五眼线
   if (showThreeCourtFiveEye && threeCourtFiveEyeResult != null) {
       drawThreeCourtFiveEyeLines(
           threeCourtFiveEyeResult = threeCourtFiveEyeResult,
           scaleX = scaleX,
           scaleY = scaleY,
           offsetX = offsetX,
           offsetY = offsetY
       )
   }
}

/**
 * 在独立区域绘制3D人脸关键点
 */
private fun DrawScope.drawFace3DPoints(
    faceMesh: FaceMesh,
    viewWidth: Float,
    viewHeight: Float,
    isBackCamera: Boolean
) {
    // 获取人脸边界框
    val boundingBox = faceMesh.boundingBox
    val faceWidth = boundingBox.width().toFloat()
    val faceHeight = boundingBox.height().toFloat()

    // 计算缩放比例，使人脸关键点适合显示区域
    val scaleX = viewWidth * 1.08f / faceWidth
    val scaleY = viewHeight * 1.08f / faceHeight
    val scale = scaleX.coerceAtMost(scaleY)

    // 计算居中偏移
    val scaledWidth = faceWidth * scale
    val scaledHeight = faceHeight * scale
    val offsetX = (viewWidth - scaledWidth) / 2f
    val offsetY = (viewHeight - scaledHeight) / 2f

    // 绘制关键点
    val points = faceMesh.allPoints.map { point ->
        val x = if (isBackCamera) {
            offsetX + (point.position.x - boundingBox.left) * scale
        } else {
            // 前置摄像头需要水平翻转
            offsetX + (boundingBox.right - point.position.x) * scale
        }
        val y = offsetY + (point.position.y - boundingBox.top) * scale
        Offset(x, y)
    }

    drawPoints(
        points = points,
        pointMode = PointMode.Points,
        color = Color.Cyan.copy(alpha = 0.9f),
        strokeWidth = 2.dp.toPx()
    )
}

/**
 * 绘制真实的人脸轮廓，基于关键点构成平滑曲线
 */
private fun DrawScope.drawFaceContour(
    faceMesh: FaceMesh,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    isBackCamera: Boolean
) {
    // MediaPipe Face Mesh 面部轮廓关键点索引
    // 这些是构成面部外轮廓的关键点，按顺序连接形成闭合轮廓
    val faceOvalIndices = listOf(
        // 右脸颊到下巴 (从右耳前开始)
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
        // 下巴部分
        397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127,
        // 左脸颊 (到左耳前)
        162, 21, 54, 103, 67, 109, 10
    )

    // 获取轮廓点的坐标
    val allPoints = faceMesh.allPoints
    val contourPoints = faceOvalIndices.mapNotNull { index ->
        if (index < allPoints.size) {
            val point = allPoints[index]
            // 对于拍照后的图像，坐标已经是正确的，不需要镜像
            val x = offsetX + point.position.x * scaleX
            val y = offsetY + point.position.y * scaleY
            Offset(x, y)
        } else null
    }

    if (contourPoints.size >= 3) {
        // 使用Path绘制平滑的轮廓线
        val path = Path()

        // 移动到第一个点
        path.moveTo(contourPoints[0].x, contourPoints[0].y)

        // 使用二次贝塞尔曲线连接各点，创建平滑效果
        for (i in 1 until contourPoints.size) {
            val currentPoint = contourPoints[i]
            val previousPoint = contourPoints[i - 1]

            // 计算控制点（在两点之间）
            val controlX = (previousPoint.x + currentPoint.x) / 2f
            val controlY = (previousPoint.y + currentPoint.y) / 2f

            // 绘制平滑曲线
            path.quadraticTo(
                controlX, controlY,
                currentPoint.x, currentPoint.y
            )
        }

        // 闭合路径
        path.close()

        // 绘制轮廓
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.7f),
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * 绘制五官轮廓（眉毛、眼睛、鼻子、嘴巴）
 */
private fun DrawScope.drawFacialFeatures(
    faceMesh: FaceMesh,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    isBackCamera: Boolean
) {
    val allPoints = faceMesh.allPoints
    if (allPoints.isEmpty()) return

    // 将关键点索引转换为屏幕坐标
    fun getPoint(index: Int): Offset? {
        if (index < allPoints.size) {
            val point = allPoints[index]
            return Offset(
                x = offsetX + point.position.x * scaleX,
                y = offsetY + point.position.y * scaleY
            )
        }
        return null
    }

    // 定义要绘制为闭合区域的五官（眉毛）
    val polygons = listOf(
        // 左眉 (更完整的轮廓)
        listOf(46, 53, 52, 65, 55, 107, 66, 105, 63, 70),
        // 右眉 (更完整的轮廓)
        listOf(276, 283, 282, 295, 285, 336, 296, 334, 293, 300)
    )

    // 定义要绘制为线条的五官（眼睛、嘴巴、鼻子）
    val lines = listOf(
        // 鼻子
        listOf(168, 6, 197, 195, 5, 4, 1, 2, 98, 97),
        // 左眼
        listOf(33, 160, 158, 133, 153, 144, 163, 7, 33),
        // 右眼
        listOf(263, 387, 385, 362, 380, 373, 390, 249, 263),
        // 嘴唇外圈
        listOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409, 270, 269, 267, 0, 37, 39, 40, 185, 61),
        // 嘴唇内圈
        listOf(78, 95, 88, 178, 87, 14, 317, 402, 318, 324, 308, 415, 310, 311, 312, 13, 82, 81, 80, 191, 78)
    )

    // 绘制闭合区域
    polygons.forEach { polygonIndices ->
        val points = polygonIndices.mapNotNull { getPoint(it) }
        if (points.size > 2) {
            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point ->
                path.lineTo(point.x, point.y)
            }
            path.close() // 闭合路径
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }

    // 绘制线条
    lines.forEach { connection ->
        for (i in 0 until connection.size - 1) {
            val startPoint = getPoint(connection[i])
            val endPoint = getPoint(connection[i + 1])
            if (startPoint != null && endPoint != null) {
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = startPoint,
                    end = endPoint,
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}


/**
 * 绘制十二宫位置标注
 */
private fun DrawScope.drawTwelvePalaces(
    faceMesh: FaceMesh,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    isBackCamera: Boolean
) {
    // 获取十二宫位置和每个宫位的点数
    val (palacePositions, counts) = TwelvePalaces.calculatePalacePositions(
        allPoints = faceMesh.allPoints
    )

    if (palacePositions.isEmpty()) return

    var positionIndex = 0
    // 遍历每个宫位（由counts列表定义）
    counts.forEachIndexed { palaceIndex, count ->
        if (count == 0) return@forEachIndexed

        val circledNumber = TwelvePalaces.getCircledNumber(palaceIndex)
        val positionToDraw: Offset?

        when {
            // 中轴线宫位
            count == 1 -> {
                positionToDraw = palacePositions[positionIndex]
            }
            // 对称宫位
            count >= 2 -> {
                val leftPosition = palacePositions[positionIndex]
                val rightPosition = palacePositions[positionIndex + 1]
                // 偶数序号在左，奇数序号在右
                positionToDraw = if (palaceIndex % 2 == 0) leftPosition else rightPosition
            }
            else -> {
                positionToDraw = null
            }
        }

        positionToDraw?.let { position ->
            // 坐标转换
            val x = offsetX + position.x * scaleX
            val y = offsetY + position.y * scaleY

            android.util.Log.d("FaceOverlay", "Palace $palaceIndex ($circledNumber): original=(${position.x}, ${position.y}), display=($x, $y)")

            // 直接绘制带圆圈的数字符号（①②③等），更简洁
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = Color.White.copy(alpha = 0.95f).toArgb()
                    textSize = 16.sp.toPx() // 稍微增大字体以确保清晰度
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val textHeight = paint.descent() - paint.ascent()
                val textOffset = textHeight / 2 - paint.descent()

                drawText(
                    circledNumber,
                    x,
                    y + textOffset,
                    paint
                )
            }
        }
        // 移动指针到下一个宫位
        positionIndex += count
    }
}

/**
* 绘制三庭五眼辅助线
*/
private fun DrawScope.drawThreeCourtFiveEyeLines(
    threeCourtFiveEyeResult: ThreeCourtFiveEyeResult,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float
) {
    val lightBlue = Color(0xFF81D4FA).copy(alpha = 0.8f) // 浅蓝色
    val lightGreen = Color(0xFFA5D6A7).copy(alpha = 0.8f) // 浅绿色

    // 绘制三庭线（水平）
    threeCourtFiveEyeResult.threeCourt.lineYCoordinates.forEach { y ->
        val scaledY = offsetY + y * scaleY
        drawLine(
            color = lightBlue,
            start = Offset(0f, scaledY),
            end = Offset(size.width, scaledY),
            strokeWidth = 2.dp.toPx()
        )
    }

    // 绘制五眼线（垂直）
    threeCourtFiveEyeResult.fiveEye.lineXCoordinates.forEach { x ->
        val scaledX = offsetX + x * scaleX
        drawLine(
            color = lightGreen,
            start = Offset(scaledX, 0f),
            end = Offset(scaledX, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }
}
