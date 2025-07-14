package com.wendy.face.detection

import android.util.Log
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 人脸检测结果平滑处理器
 * 用于解决相机预览时画面抖动导致的关键点不贴合问题
 */
class FaceDetectionSmoother {
    
    companion object {
        private const val TAG = "FaceDetectionSmoother"
        private const val MAX_HISTORY_SIZE = 5 // 保留最近5帧的检测结果
        private const val SMOOTHING_FACTOR = 0.3f // 平滑因子，值越小越平滑
        private const val MAX_MOVEMENT_THRESHOLD = 50f // 最大移动阈值，超过此值认为是新的人脸
        private const val MIN_CONFIDENCE_THRESHOLD = 0.5f // 最小置信度阈值
    }
    
    private val mutex = Mutex()
    private val detectionHistory = mutableListOf<DetectionFrame>()
    private var lastValidDetection: DetectionFrame? = null
    
    /**
     * 检测帧数据
     */
    private data class DetectionFrame(
        val timestamp: Long,
        val faceMeshes: List<FaceMesh>,
        val imageWidth: Int,
        val imageHeight: Int
    )
    
    /**
     * 平滑处理检测结果
     * @param faceMeshes 当前帧检测到的人脸网格
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 平滑处理后的人脸网格列表
     */
    suspend fun smoothDetectionResults(
        faceMeshes: List<FaceMesh>,
        imageWidth: Int,
        imageHeight: Int
    ): List<FaceMesh> = mutex.withLock {
        val currentTime = System.currentTimeMillis()
        val currentFrame = DetectionFrame(currentTime, faceMeshes, imageWidth, imageHeight)

        // 添加到历史记录
        detectionHistory.add(currentFrame)

        // 保持历史记录大小
        while (detectionHistory.size > MAX_HISTORY_SIZE) {
            detectionHistory.removeAt(0)
        }

        // 如果没有检测到人脸，返回空列表
        if (faceMeshes.isEmpty()) {
            Log.d(TAG, "No faces detected in current frame")
            return emptyList()
        }

        // 如果是第一次检测或历史记录不足，直接返回当前结果
        if (detectionHistory.size < 2 || lastValidDetection == null) {
            lastValidDetection = currentFrame
            Log.d(TAG, "First detection or insufficient history, returning current result")
            return faceMeshes
        }

        // 检查人脸是否稳定（通过边界框变化判断）
        val isStable = isDetectionStable(faceMeshes)

        if (isStable) {
            // 如果检测稳定，直接返回当前结果
            lastValidDetection = currentFrame
            Log.d(TAG, "Detection is stable, returning current result")
            return faceMeshes
        } else {
            // 如果检测不稳定，返回上一次稳定的结果
            val lastValid = lastValidDetection
            if (lastValid != null && (currentTime - lastValid.timestamp) < 500) { // 500ms内的结果有效
                Log.d(TAG, "Detection is unstable, returning last valid result")
                return lastValid.faceMeshes
            } else {
                // 如果上次结果太旧，还是返回当前结果
                lastValidDetection = currentFrame
                Log.d(TAG, "Last valid result too old, returning current result")
                return faceMeshes
            }
        }
    }
    
    /**
     * 检查检测结果是否稳定
     */
    private fun isDetectionStable(currentFaces: List<FaceMesh>): Boolean {
        val lastDetection = lastValidDetection ?: return true
        val lastFaces = lastDetection.faceMeshes

        // 如果人脸数量不同，认为不稳定
        if (currentFaces.size != lastFaces.size) {
            return false
        }

        // 检查每个人脸的稳定性
        for (i in currentFaces.indices) {
            if (i >= lastFaces.size) break

            val currentFace = currentFaces[i]
            val lastFace = lastFaces[i]

            // 通过边界框变化判断稳定性
            if (isFaceMovedTooMuch(currentFace, lastFace)) {
                return false
            }

            // 检查边界框大小变化
            val currentBounds = currentFace.boundingBox
            val lastBounds = lastFace.boundingBox

            val sizeChangeRatio = abs(currentBounds.width() - lastBounds.width()).toFloat() / lastBounds.width()
            if (sizeChangeRatio > 0.2f) { // 大小变化超过20%认为不稳定
                return false
            }
        }

        return true
    }
    
    /**
     * 检查人脸是否移动过大
     */
    private fun isFaceMovedTooMuch(currentFace: FaceMesh, lastFace: FaceMesh): Boolean {
        val currentBounds = currentFace.boundingBox
        val lastBounds = lastFace.boundingBox
        
        val centerXDiff = abs(currentBounds.centerX() - lastBounds.centerX()).toDouble()
        val centerYDiff = abs(currentBounds.centerY() - lastBounds.centerY()).toDouble()
        val distance = sqrt(centerXDiff * centerXDiff + centerYDiff * centerYDiff).toFloat()
        
        return distance > MAX_MOVEMENT_THRESHOLD
    }
    
    /**
     * 清除历史记录
     */
    suspend fun clearHistory() = mutex.withLock {
        detectionHistory.clear()
        lastValidDetection = null
        Log.d(TAG, "Detection history cleared")
    }
    
    /**
     * 获取当前历史记录大小
     */
    suspend fun getHistorySize(): Int = mutex.withLock {
        detectionHistory.size
    }
}
