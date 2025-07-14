package com.wendy.face.utils

import android.util.Log
import kotlin.math.*

/**
 * 精确坐标转换参数
 */
data class PreciseTransform(
    val scaleX: Double,
    val scaleY: Double,
    val offsetX: Double,
    val offsetY: Double,
    val mirrorX: Boolean = false,
    val rotationDegrees: Double = 0.0
) {
    /**
     * 应用变换到点
     */
    fun transformPoint(x: Float, y: Float): Pair<Float, Float> {
        var transformedX = x.toDouble()
        var transformedY = y.toDouble()
        
        // 1. 旋转变换
        if (rotationDegrees != 0.0) {
            val radians = Math.toRadians(rotationDegrees)
            val cos = cos(radians)
            val sin = sin(radians)
            
            val newX = transformedX * cos - transformedY * sin
            val newY = transformedX * sin + transformedY * cos
            
            transformedX = newX
            transformedY = newY
        }
        
        // 2. 缩放变换
        transformedX *= scaleX
        transformedY *= scaleY
        
        // 3. 平移变换
        transformedX += offsetX
        transformedY += offsetY
        
        // 4. 镜像变换
        if (mirrorX) {
            // 需要知道镜像轴的位置，这里假设是视图中心
            // 实际使用时需要传入视图宽度
            // transformedX = viewWidth - transformedX
        }
        
        return Pair(transformedX.toFloat(), transformedY.toFloat())
    }
    
    /**
     * 批量变换点
     */
    fun transformPoints(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        return points.map { transformPoint(it.first, it.second) }
    }
}

/**
 * 坐标转换质量评估
 */
data class TransformQuality(
    val precisionScore: Float,
    val consistencyScore: Float,
    val errorEstimate: Float
)

/**
 * 精确坐标转换工具
 * 提供高精度、低累积误差的坐标转换功能
 */
object PreciseCoordinateTransform {
    
    private const val TAG = "PreciseCoordinateTransform"
    
    /**
     * 创建从检测坐标系到显示坐标系的精确变换
     * @param sourceWidth 源图像宽度
     * @param sourceHeight 源图像高度
     * @param targetWidth 目标显示宽度
     * @param targetHeight 目标显示高度
     * @param viewWidth 视图宽度
     * @param viewHeight 视图高度
     * @param contentScale 内容缩放模式（Crop或Fit）
     * @param isFrontCamera 是否前置摄像头
     * @return 精确变换参数
     */
    fun createPreciseTransform(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        contentScale: ContentScaleMode = ContentScaleMode.CROP,
        isFrontCamera: Boolean = false
    ): PreciseTransform {
        
        Log.d(TAG, "Creating precise transform:")
        Log.d(TAG, "  Source: ${sourceWidth}x${sourceHeight}")
        Log.d(TAG, "  Target: ${targetWidth}x${targetHeight}")
        Log.d(TAG, "  View: ${viewWidth}x${viewHeight}")
        Log.d(TAG, "  Scale mode: $contentScale, Front camera: $isFrontCamera")
        
        // 1. 计算从源到目标的基础缩放
        val baseScaleX = targetWidth.toDouble() / sourceWidth.toDouble()
        val baseScaleY = targetHeight.toDouble() / sourceHeight.toDouble()
        
        // 2. 计算显示缩放和偏移
        val targetAspectRatio = targetWidth.toDouble() / targetHeight.toDouble()
        val viewAspectRatio = viewWidth.toDouble() / viewHeight.toDouble()
        
        val displayScale: Double
        val offsetX: Double
        val offsetY: Double
        
        when (contentScale) {
            ContentScaleMode.CROP -> {
                if (targetAspectRatio > viewAspectRatio) {
                    // 目标图像更宽，高度填满视图
                    displayScale = viewHeight.toDouble() / targetHeight.toDouble()
                    val scaledWidth = targetWidth * displayScale
                    offsetX = (viewWidth.toDouble() - scaledWidth) / 2.0
                    offsetY = 0.0
                } else {
                    // 目标图像更高，宽度填满视图
                    displayScale = viewWidth.toDouble() / targetWidth.toDouble()
                    val scaledHeight = targetHeight * displayScale
                    offsetX = 0.0
                    offsetY = (viewHeight.toDouble() - scaledHeight) / 2.0
                }
            }
            ContentScaleMode.FIT -> {
                val scale = min(
                    viewWidth.toDouble() / targetWidth.toDouble(),
                    viewHeight.toDouble() / targetHeight.toDouble()
                )
                displayScale = scale
                val scaledWidth = targetWidth * scale
                val scaledHeight = targetHeight * scale
                offsetX = (viewWidth.toDouble() - scaledWidth) / 2.0
                offsetY = (viewHeight.toDouble() - scaledHeight) / 2.0
            }
        }
        
        // 3. 组合变换
        val finalScaleX = baseScaleX * displayScale
        val finalScaleY = baseScaleY * displayScale
        
        Log.d(TAG, "Transform result:")
        Log.d(TAG, "  Final scale: ${finalScaleX}x${finalScaleY}")
        Log.d(TAG, "  Offset: (${offsetX}, ${offsetY})")
        
        return PreciseTransform(
            scaleX = finalScaleX,
            scaleY = finalScaleY,
            offsetX = offsetX,
            offsetY = offsetY,
            mirrorX = isFrontCamera
        )
    }
    
    // 暂时移除复杂的统一变换功能，专注于核心稳定化
    
    /**
     * 评估坐标转换质量
     */
    fun evaluateTransformQuality(
        transform: PreciseTransform,
        sourcePoints: List<Pair<Float, Float>>,
        expectedRange: Pair<Float, Float>
    ): TransformQuality {
        
        val transformedPoints = transform.transformPoints(sourcePoints)
        
        // 计算精度分数（基于变换后点的分布）
        val xValues = transformedPoints.map { it.first }
        val yValues = transformedPoints.map { it.second }
        
        val xRange = (xValues.maxOrNull() ?: 0f) - (xValues.minOrNull() ?: 0f)
        val yRange = (yValues.maxOrNull() ?: 0f) - (yValues.minOrNull() ?: 0f)
        
        val precisionScore = if (xRange > 0 && yRange > 0) {
            val aspectRatio = xRange / yRange
            if (aspectRatio in 0.5f..2.0f) 1.0f else 0.5f
        } else 0.0f
        
        // 计算一致性分数（检查是否有异常的大幅变换）
        val scaleRatio = abs(transform.scaleX / transform.scaleY - 1.0).toFloat()
        val consistencyScore = (1.0f - scaleRatio.coerceAtMost(1.0f)).coerceAtLeast(0.0f)
        
        // 估算误差
        val errorEstimate = (scaleRatio * 10f).coerceAtMost(50f)
        
        return TransformQuality(
            precisionScore = precisionScore,
            consistencyScore = consistencyScore,
            errorEstimate = errorEstimate
        )
    }
}

/**
 * 内容缩放模式
 */
enum class ContentScaleMode {
    CROP,  // 裁剪模式（填满视图）
    FIT    // 适应模式（完整显示）
}
