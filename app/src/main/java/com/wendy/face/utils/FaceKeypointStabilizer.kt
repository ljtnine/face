package com.wendy.face.utils

import android.util.Log
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.*

/**
 * 关键点稳定化数据类
 */
data class StabilizedKeypoint(
    val x: Float,
    val y: Float,
    val confidence: Float,
    val isStable: Boolean
)

/**
 * 关键点质量评估结果
 */
data class KeypointQuality(
    val overallScore: Float,
    val stabilityScore: Float,
    val consistencyScore: Float,
    val precisionScore: Float,
    val outlierCount: Int
)

/**
 * 关键点历史记录
 */
private data class KeypointHistory(
    val points: MutableList<Pair<Float, Float>> = mutableListOf(),
    val timestamps: MutableList<Long> = mutableListOf(),
    val maxSize: Int = 5
) {
    fun add(x: Float, y: Float, timestamp: Long) {
        points.add(Pair(x, y))
        timestamps.add(timestamp)
        
        if (points.size > maxSize) {
            points.removeAt(0)
            timestamps.removeAt(0)
        }
    }
    
    fun clear() {
        points.clear()
        timestamps.clear()
    }
    
    fun getAverage(): Pair<Float, Float>? {
        if (points.isEmpty()) return null
        val avgX = points.map { it.first }.average().toFloat()
        val avgY = points.map { it.second }.average().toFloat()
        return Pair(avgX, avgY)
    }
    
    fun getVariance(): Float {
        if (points.size < 2) return 0f
        val avg = getAverage() ?: return 0f
        val variance = points.map { point ->
            val dx = point.first - avg.first
            val dy = point.second - avg.second
            dx * dx + dy * dy
        }.average().toFloat()
        return sqrt(variance)
    }
}

/**
 * 人脸关键点稳定化器
 * 提供关键点平滑、异常检测、质量评估等功能
 */
object FaceKeypointStabilizer {
    
    private const val TAG = "FaceKeypointStabilizer"
    
    // 稳定化参数
    private const val SMOOTHING_FACTOR = 0.7f // 平滑系数
    private const val OUTLIER_THRESHOLD = 15.0f // 异常点阈值（像素）
    private const val MIN_CONFIDENCE = 0.3f // 最小置信度
    private const val STABILITY_THRESHOLD = 5.0f // 稳定性阈值
    private const val HISTORY_TIMEOUT_MS = 2000L // 历史记录超时时间
    
    // 关键点历史记录
    private val keypointHistories = mutableMapOf<Int, KeypointHistory>()
    private var lastUpdateTime = 0L
    
    /**
     * 稳定化人脸关键点
     * @param faceMesh 原始人脸网格
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 稳定化后的关键点列表
     */
    fun stabilizeFaceKeypoints(
        faceMesh: FaceMesh,
        imageWidth: Int,
        imageHeight: Int
    ): List<StabilizedKeypoint> {
        val currentTime = System.currentTimeMillis()
        
        // 清理过期的历史记录
        if (currentTime - lastUpdateTime > HISTORY_TIMEOUT_MS) {
            clearHistory()
        }
        lastUpdateTime = currentTime
        
        val allPoints = faceMesh.allPoints
        val stabilizedPoints = mutableListOf<StabilizedKeypoint>()
        
        Log.d(TAG, "Stabilizing ${allPoints.size} keypoints")
        
        allPoints.forEachIndexed { index, point ->
            val stabilized = stabilizeKeypoint(
                index = index,
                point = point,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                timestamp = currentTime
            )
            stabilizedPoints.add(stabilized)
        }
        
        return stabilizedPoints
    }
    
    /**
     * 稳定化单个关键点
     */
    private fun stabilizeKeypoint(
        index: Int,
        point: FaceMeshPoint,
        imageWidth: Int,
        imageHeight: Int,
        timestamp: Long
    ): StabilizedKeypoint {
        val currentX = point.position.x
        val currentY = point.position.y
        
        // 获取或创建历史记录
        val history = keypointHistories.getOrPut(index) { KeypointHistory() }
        
        // 边界检查
        val isInBounds = currentX >= 0 && currentX <= imageWidth && 
                        currentY >= 0 && currentY <= imageHeight
        
        if (!isInBounds) {
            Log.w(TAG, "Keypoint $index out of bounds: ($currentX, $currentY)")
            return StabilizedKeypoint(
                x = currentX.coerceIn(0f, imageWidth.toFloat()),
                y = currentY.coerceIn(0f, imageHeight.toFloat()),
                confidence = 0.1f,
                isStable = false
            )
        }
        
        // 异常检测
        val isOutlier = detectOutlier(history, currentX, currentY)
        
        if (isOutlier) {
            Log.d(TAG, "Outlier detected for keypoint $index: ($currentX, $currentY)")
            // 对于异常点，使用历史平均值或当前值的加权平均
            val avgPoint = history.getAverage()
            if (avgPoint != null) {
                val smoothedX = avgPoint.first * 0.8f + currentX * 0.2f
                val smoothedY = avgPoint.second * 0.8f + currentY * 0.2f
                
                history.add(smoothedX, smoothedY, timestamp)
                
                return StabilizedKeypoint(
                    x = smoothedX,
                    y = smoothedY,
                    confidence = 0.5f,
                    isStable = false
                )
            }
        }
        
        // 平滑处理
        val smoothedPoint = if (history.points.isNotEmpty()) {
            val lastPoint = history.points.last()
            val smoothedX = lastPoint.first * SMOOTHING_FACTOR + currentX * (1 - SMOOTHING_FACTOR)
            val smoothedY = lastPoint.second * SMOOTHING_FACTOR + currentY * (1 - SMOOTHING_FACTOR)
            Pair(smoothedX, smoothedY)
        } else {
            Pair(currentX, currentY)
        }
        
        // 更新历史记录
        history.add(smoothedPoint.first, smoothedPoint.second, timestamp)
        
        // 计算稳定性
        val variance = history.getVariance()
        val isStable = variance < STABILITY_THRESHOLD
        val confidence = calculateConfidence(variance, history.points.size)
        
        return StabilizedKeypoint(
            x = smoothedPoint.first,
            y = smoothedPoint.second,
            confidence = confidence,
            isStable = isStable
        )
    }
    
    /**
     * 检测异常点
     */
    private fun detectOutlier(history: KeypointHistory, x: Float, y: Float): Boolean {
        if (history.points.size < 2) return false
        
        val avgPoint = history.getAverage() ?: return false
        val distance = sqrt((x - avgPoint.first).pow(2) + (y - avgPoint.second).pow(2))
        
        return distance > OUTLIER_THRESHOLD
    }
    
    /**
     * 计算关键点置信度
     */
    private fun calculateConfidence(variance: Float, historySize: Int): Float {
        val stabilityScore = (STABILITY_THRESHOLD / (variance + 1f)).coerceIn(0f, 1f)
        val historyScore = (historySize / 5f).coerceIn(0f, 1f)
        
        return (stabilityScore * 0.7f + historyScore * 0.3f).coerceAtLeast(MIN_CONFIDENCE)
    }
    
    /**
     * 评估关键点质量
     */
    fun evaluateKeypointQuality(stabilizedKeypoints: List<StabilizedKeypoint>): KeypointQuality {
        if (stabilizedKeypoints.isEmpty()) {
            return KeypointQuality(0f, 0f, 0f, 0f, 0)
        }
        
        val stableCount = stabilizedKeypoints.count { it.isStable }
        val stabilityScore = stableCount.toFloat() / stabilizedKeypoints.size
        
        val avgConfidence = stabilizedKeypoints.map { it.confidence }.average().toFloat()
        
        val outlierCount = stabilizedKeypoints.count { it.confidence < MIN_CONFIDENCE }
        val consistencyScore = 1f - (outlierCount.toFloat() / stabilizedKeypoints.size)
        
        // 计算精度分数（基于关键点分布的合理性）
        val precisionScore = calculatePrecisionScore(stabilizedKeypoints)
        
        val overallScore = (stabilityScore * 0.3f + avgConfidence * 0.3f + 
                           consistencyScore * 0.2f + precisionScore * 0.2f)
        
        return KeypointQuality(
            overallScore = overallScore,
            stabilityScore = stabilityScore,
            consistencyScore = consistencyScore,
            precisionScore = precisionScore,
            outlierCount = outlierCount
        )
    }
    
    /**
     * 计算精度分数
     */
    private fun calculatePrecisionScore(keypoints: List<StabilizedKeypoint>): Float {
        if (keypoints.size < 10) return 0.5f
        
        // 检查关键点是否形成合理的面部结构
        // 这里简化为检查关键点的分布范围
        val xValues = keypoints.map { it.x }
        val yValues = keypoints.map { it.y }
        
        val xRange = (xValues.maxOrNull() ?: 0f) - (xValues.minOrNull() ?: 0f)
        val yRange = (yValues.maxOrNull() ?: 0f) - (yValues.minOrNull() ?: 0f)
        
        // 合理的面部应该有一定的宽高比
        val aspectRatio = if (yRange > 0) xRange / yRange else 0f
        val aspectScore = if (aspectRatio in 0.6f..1.4f) 1f else 0.5f
        
        return aspectScore
    }
    
    /**
     * 清理历史记录
     */
    fun clearHistory() {
        keypointHistories.clear()
        Log.d(TAG, "Keypoint history cleared")
    }
    
    /**
     * 获取稳定化统计信息
     */
    fun getStabilizationStats(): Map<String, Any> {
        val totalKeypoints = keypointHistories.size
        val stableKeypoints = keypointHistories.values.count { it.getVariance() < STABILITY_THRESHOLD }
        
        return mapOf(
            "totalKeypoints" to totalKeypoints,
            "stableKeypoints" to stableKeypoints,
            "stabilityRatio" to if (totalKeypoints > 0) stableKeypoints.toFloat() / totalKeypoints else 0f,
            "historySize" to keypointHistories.values.map { it.points.size }.average()
        )
    }
}
