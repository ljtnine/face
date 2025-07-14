package com.wendy.face.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 人脸提取信息数据类
 * 记录人脸提取过程中的关键信息，用于后续的坐标转换
 */
data class ExtractedFaceInfo(
    val originalBitmap: Bitmap,
    val extractedBitmap: Bitmap,
    val extractionRect: Rect,
    val scaleFactor: Float,
    val faceMeshes: List<FaceMesh>
)

/**
 * 检测策略枚举
 */
enum class DetectionStrategy {
    ENHANCED_TWO_STEP,    // 二步增强检测
    DIRECT_MESH,          // 直接网格检测
    MULTI_RESOLUTION,     // 多分辨率检测
    PREPROCESSED          // 预处理后检测
}

/**
 * 检测结果数据类
 */
data class DetectionResult(
    val faceMeshes: List<FaceMesh>,
    val imageWidth: Int,
    val imageHeight: Int,
    val strategy: DetectionStrategy,
    val extractedFaceInfo: ExtractedFaceInfo? = null,
    val stabilizedKeypoints: List<StabilizedKeypoint>? = null,
    val keypointQuality: KeypointQuality? = null
)

/**
 * 人脸提取工具类
 * 实现多种检测策略，提高人脸检测的成功率和准确性
 */
object FaceExtractionUtils {
    
    private const val TAG = "FaceExtractionUtils"
    
    // 基础人脸检测器配置
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.08f) // 降低最小人脸尺寸阈值
        .enableTracking()
        .build()
    
    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)
    
    // 面部网格检测器配置
    private val faceMeshDetectorOptions = FaceMeshDetectorOptions.Builder()
        .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
        .build()
    
    private val faceMeshDetector = FaceMeshDetection.getClient(faceMeshDetectorOptions)
    
    /**
     * 智能人脸检测方法
     * 尝试多种检测策略，直到成功或所有策略都失败
     * 
     * @param bitmap 原始图像
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    suspend fun smartFaceDetection(
        bitmap: Bitmap,
        onSuccess: (DetectionResult) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            Log.d(TAG, "Starting smart face detection on ${bitmap.width}x${bitmap.height} image")
            
            // 策略1: 增强二步检测
            try {
                val result = enhancedTwoStepDetection(bitmap)
                if (result.faceMeshes.isNotEmpty()) {
                    Log.d(TAG, "Enhanced two-step detection successful")
                    onSuccess(result)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Enhanced two-step detection failed", e)
            }
            
            // 策略2: 直接网格检测
            try {
                val result = directMeshDetection(bitmap)
                if (result.faceMeshes.isNotEmpty()) {
                    Log.d(TAG, "Direct mesh detection successful")
                    onSuccess(result)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct mesh detection failed", e)
            }
            
            // 策略3: 多分辨率检测
            try {
                val result = multiResolutionDetection(bitmap)
                if (result.faceMeshes.isNotEmpty()) {
                    Log.d(TAG, "Multi-resolution detection successful")
                    onSuccess(result)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Multi-resolution detection failed", e)
            }
            
            // 策略4: 预处理后检测
            try {
                val result = preprocessedDetection(bitmap)
                if (result.faceMeshes.isNotEmpty()) {
                    Log.d(TAG, "Preprocessed detection successful")
                    onSuccess(result)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Preprocessed detection failed", e)
            }
            
            // 所有策略都失败
            Log.e(TAG, "All detection strategies failed")
            onFailure(Exception("All face detection strategies failed"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Smart face detection failed", e)
            onFailure(e)
        }
    }
    
    /**
     * 增强的二步检测策略
     */
    private suspend fun enhancedTwoStepDetection(bitmap: Bitmap): DetectionResult {
        Log.d(TAG, "Trying enhanced two-step detection")
        
        // 第一步：使用基础人脸检测获取边界框
        val faces = detectBasicFaces(bitmap)
        
        if (faces.isEmpty()) {
            throw Exception("No faces detected in basic detection")
        }
        
        Log.d(TAG, "Basic detection found ${faces.size} faces")
        
        // 选择最佳人脸
        val bestFace = selectBestFace(faces, bitmap.width, bitmap.height)
        
        // 第二步：提取人脸区域并扩展
        val extractedInfo = extractFaceRegion(bitmap, bestFace)
        
        // 第三步：在提取的区域上进行精确的面部网格检测
        val faceMeshes = detectFaceMeshInExtractedRegion(extractedInfo.extractedBitmap)
        
        if (faceMeshes.isEmpty()) {
            throw Exception("Face mesh detection failed in extracted region")
        }
        
        val finalInfo = extractedInfo.copy(faceMeshes = faceMeshes)

        // 应用关键点稳定化
        val stabilizedKeypoints = FaceKeypointStabilizer.stabilizeFaceKeypoints(
            faceMeshes.first(), bitmap.width, bitmap.height
        )
        val keypointQuality = FaceKeypointStabilizer.evaluateKeypointQuality(stabilizedKeypoints)

        Log.d(TAG, "Keypoint quality - Overall: ${keypointQuality.overallScore}, Stability: ${keypointQuality.stabilityScore}")

        return DetectionResult(
            faceMeshes = faceMeshes,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            strategy = DetectionStrategy.ENHANCED_TWO_STEP,
            extractedFaceInfo = finalInfo,
            stabilizedKeypoints = stabilizedKeypoints,
            keypointQuality = keypointQuality
        )
    }
    
    /**
     * 直接网格检测策略
     */
    private suspend fun directMeshDetection(bitmap: Bitmap): DetectionResult {
        Log.d(TAG, "Trying direct mesh detection")
        
        val faceMeshes = detectFaceMeshDirect(bitmap)
        
        if (faceMeshes.isEmpty()) {
            throw Exception("Direct mesh detection failed")
        }

        // 应用关键点稳定化
        val stabilizedKeypoints = FaceKeypointStabilizer.stabilizeFaceKeypoints(
            faceMeshes.first(), bitmap.width, bitmap.height
        )
        val keypointQuality = FaceKeypointStabilizer.evaluateKeypointQuality(stabilizedKeypoints)

        return DetectionResult(
            faceMeshes = faceMeshes,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            strategy = DetectionStrategy.DIRECT_MESH,
            stabilizedKeypoints = stabilizedKeypoints,
            keypointQuality = keypointQuality
        )
    }
    
    /**
     * 多分辨率检测策略
     */
    private suspend fun multiResolutionDetection(bitmap: Bitmap): DetectionResult {
        Log.d(TAG, "Trying multi-resolution detection")
        
        val resolutions = listOf(1.0f, 0.8f, 1.2f, 0.6f, 1.5f)
        
        for (scale in resolutions) {
            try {
                val scaledBitmap = if (scale != 1.0f) {
                    val newWidth = (bitmap.width * scale).toInt()
                    val newHeight = (bitmap.height * scale).toInt()
                    Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                } else {
                    bitmap
                }
                
                val faceMeshes = detectFaceMeshDirect(scaledBitmap)
                
                if (faceMeshes.isNotEmpty()) {
                    Log.d(TAG, "Multi-resolution detection successful at scale $scale")
                    
                    // 如果使用了缩放，需要回收临时bitmap
                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                    
                    return DetectionResult(
                        faceMeshes = faceMeshes,
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height,
                        strategy = DetectionStrategy.MULTI_RESOLUTION
                    )
                }
                
                // 回收临时bitmap
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Multi-resolution detection failed at scale $scale", e)
            }
        }
        
        throw Exception("Multi-resolution detection failed at all scales")
    }
    
    /**
     * 预处理后检测策略
     */
    private suspend fun preprocessedDetection(bitmap: Bitmap): DetectionResult {
        Log.d(TAG, "Trying preprocessed detection")

        val preprocessedBitmap = FaceDetectionUtils.preprocessImageForDetection(bitmap)
        val faceMeshes = detectFaceMeshDirect(preprocessedBitmap)

        // 回收预处理后的bitmap（如果不是原始bitmap）
        if (preprocessedBitmap != bitmap) {
            preprocessedBitmap.recycle()
        }

        if (faceMeshes.isEmpty()) {
            throw Exception("Preprocessed detection failed")
        }

        return DetectionResult(
            faceMeshes = faceMeshes,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            strategy = DetectionStrategy.PREPROCESSED
        )
    }

    /**
     * 基础人脸检测
     * 使用ML Kit的基础人脸检测获取边界框
     */
    private suspend fun detectBasicFaces(bitmap: Bitmap): List<Face> = suspendCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    Log.d(TAG, "Basic face detection completed: ${faces.size} faces")
                    continuation.resume(faces)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Basic face detection failed", e)
                    continuation.resume(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in basic face detection", e)
            continuation.resume(emptyList())
        }
    }

    /**
     * 直接面部网格检测
     */
    private suspend fun detectFaceMeshDirect(bitmap: Bitmap): List<FaceMesh> = suspendCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            faceMeshDetector.process(inputImage)
                .addOnSuccessListener { faceMeshes ->
                    Log.d(TAG, "Direct face mesh detection completed: ${faceMeshes.size} faces")
                    continuation.resume(faceMeshes)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Direct face mesh detection failed", e)
                    continuation.resume(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in direct face mesh detection", e)
            continuation.resume(emptyList())
        }
    }

    /**
     * 在提取区域检测面部网格
     */
    private suspend fun detectFaceMeshInExtractedRegion(bitmap: Bitmap): List<FaceMesh> = suspendCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            faceMeshDetector.process(inputImage)
                .addOnSuccessListener { faceMeshes ->
                    Log.d(TAG, "Face mesh detection in extracted region completed: ${faceMeshes.size} faces")
                    continuation.resume(faceMeshes)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face mesh detection in extracted region failed", e)
                    continuation.resume(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in face mesh detection in extracted region", e)
            continuation.resume(emptyList())
        }
    }

    /**
     * 选择最佳人脸
     * 基于人脸大小、位置和置信度选择最适合的人脸
     */
    private fun selectBestFace(faces: List<Face>, imageWidth: Int, imageHeight: Int): Face {
        return faces.maxByOrNull { face ->
            val boundingBox = face.boundingBox
            val faceArea = boundingBox.width() * boundingBox.height()
            val imageArea = imageWidth * imageHeight
            val sizeScore = faceArea.toFloat() / imageArea

            // 位置分数：越靠近中心得分越高
            val centerX = boundingBox.centerX()
            val centerY = boundingBox.centerY()
            val imageCenterX = imageWidth / 2f
            val imageCenterY = imageHeight / 2f

            val distanceFromCenter = sqrt(
                ((centerX - imageCenterX) / imageCenterX).toDouble().pow(2.0) +
                ((centerY - imageCenterY) / imageCenterY).toDouble().pow(2.0)
            ).toFloat()

            val positionScore = max(0f, 1f - distanceFromCenter)

            // 综合分数
            sizeScore * 0.7f + positionScore * 0.3f
        } ?: faces.first()
    }

    /**
     * 提取人脸区域
     * 基于检测到的人脸边界框，智能扩展并提取人脸区域
     */
    private fun extractFaceRegion(bitmap: Bitmap, face: Face): ExtractedFaceInfo {
        val boundingBox = face.boundingBox
        val faceWidth = boundingBox.width()
        val faceHeight = boundingBox.height()

        // 智能扩展策略
        val topExpansion = (faceHeight * 0.8f).toInt()      // 向上扩展80%包含额头
        val bottomExpansion = (faceHeight * 0.3f).toInt()   // 向下扩展30%包含下巴
        val horizontalExpansion = (faceWidth * 0.4f).toInt() // 水平扩展40%

        // 计算扩展后的区域
        val expandedLeft = max(0, boundingBox.left - horizontalExpansion)
        val expandedTop = max(0, boundingBox.top - topExpansion)
        val expandedRight = min(bitmap.width, boundingBox.right + horizontalExpansion)
        val expandedBottom = min(bitmap.height, boundingBox.bottom + bottomExpansion)

        val extractionRect = Rect(expandedLeft, expandedTop, expandedRight, expandedBottom)
        val extractedWidth = extractionRect.width()
        val extractedHeight = extractionRect.height()

        Log.d(TAG, "Extracting face region: ${extractionRect.width()}x${extractionRect.height()} from ${bitmap.width}x${bitmap.height}")

        // 提取区域
        val extractedBitmap = Bitmap.createBitmap(
            bitmap,
            extractionRect.left,
            extractionRect.top,
            extractedWidth,
            extractedHeight
        )

        // 自适应缩放：如果提取的区域太小，放大以提高检测精度
        val minSize = 512
        val scaleFactor = if (extractedWidth < minSize || extractedHeight < minSize) {
            val scale = max(minSize.toFloat() / extractedWidth, minSize.toFloat() / extractedHeight)
            val newWidth = (extractedWidth * scale).toInt()
            val newHeight = (extractedHeight * scale).toInt()

            val scaledBitmap = Bitmap.createScaledBitmap(extractedBitmap, newWidth, newHeight, true)
            extractedBitmap.recycle() // 回收原始提取的bitmap

            Log.d(TAG, "Scaled extracted region from ${extractedWidth}x${extractedHeight} to ${newWidth}x${newHeight}")

            ExtractedFaceInfo(
                originalBitmap = bitmap,
                extractedBitmap = scaledBitmap,
                extractionRect = extractionRect,
                scaleFactor = scale,
                faceMeshes = emptyList()
            )
        } else {
            ExtractedFaceInfo(
                originalBitmap = bitmap,
                extractedBitmap = extractedBitmap,
                extractionRect = extractionRect,
                scaleFactor = 1.0f,
                faceMeshes = emptyList()
            )
        }

        return scaleFactor
    }

    /**
     * 将提取区域的关键点坐标转换为原图坐标
     * 由于ML Kit的FaceMeshPoint是不可变的，我们返回坐标对列表
     * @param extractedFaceInfo 提取信息
     * @param points 提取区域中的关键点
     * @return 转换为原图坐标的坐标对列表
     */
    fun transformPointsToOriginalCoordinates(
        extractedFaceInfo: ExtractedFaceInfo,
        points: List<FaceMeshPoint>
    ): List<Pair<Float, Float>> {
        val extractionRect = extractedFaceInfo.extractionRect
        val scaleFactor = extractedFaceInfo.scaleFactor

        return points.map { point ->
            // 先反向缩放
            val unscaledX = point.position.x / scaleFactor
            val unscaledY = point.position.y / scaleFactor

            // 再转换到原图坐标系
            val originalX = unscaledX + extractionRect.left
            val originalY = unscaledY + extractionRect.top

            Pair(originalX, originalY)
        }
    }

    /**
     * 获取转换后的关键点坐标（已转换为原图坐标）
     * @param detectionResult 检测结果
     * @return 坐标转换后的关键点坐标列表
     */
    fun getTransformedFacePoints(detectionResult: DetectionResult): List<List<Pair<Float, Float>>> {
        val extractedFaceInfo = detectionResult.extractedFaceInfo

        return if (extractedFaceInfo != null && detectionResult.strategy == DetectionStrategy.ENHANCED_TWO_STEP) {
            // 对于二步检测，需要进行坐标转换
            detectionResult.faceMeshes.map { faceMesh ->
                transformPointsToOriginalCoordinates(extractedFaceInfo, faceMesh.allPoints)
            }
        } else {
            // 对于其他检测策略，直接转换原始坐标
            detectionResult.faceMeshes.map { faceMesh ->
                faceMesh.allPoints.map { point ->
                    Pair(point.position.x, point.position.y)
                }
            }
        }
    }

    /**
     * 检查是否需要坐标转换
     * @param detectionResult 检测结果
     * @return 是否需要坐标转换
     */
    fun needsCoordinateTransform(detectionResult: DetectionResult): Boolean {
        return detectionResult.extractedFaceInfo != null &&
               detectionResult.strategy == DetectionStrategy.ENHANCED_TWO_STEP
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            faceDetector.close()
            faceMeshDetector.close()
            Log.d(TAG, "FaceExtractionUtils resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing FaceExtractionUtils resources", e)
        }
    }

    /**
     * 验证检测结果的质量
     * @param detectionResult 检测结果
     * @return 质量分数 (0.0 - 1.0)
     */
    fun evaluateDetectionQuality(detectionResult: DetectionResult): Float {
        if (detectionResult.faceMeshes.isEmpty()) return 0.0f

        val faceMesh = detectionResult.faceMeshes.first()
        val pointCount = faceMesh.allPoints.size
        val boundingBox = faceMesh.boundingBox

        // 基于关键点数量的分数
        val pointScore = min(1.0f, pointCount / 468.0f) // ML Kit标准点数

        // 基于人脸大小的分数
        val faceArea = boundingBox.width() * boundingBox.height()
        val imageArea = detectionResult.imageWidth * detectionResult.imageHeight
        val sizeScore = min(1.0f, (faceArea.toFloat() / imageArea) * 10)

        // 基于检测策略的分数（二步检测通常更准确）
        val strategyScore = when (detectionResult.strategy) {
            DetectionStrategy.ENHANCED_TWO_STEP -> 1.0f
            DetectionStrategy.DIRECT_MESH -> 0.8f
            DetectionStrategy.MULTI_RESOLUTION -> 0.7f
            DetectionStrategy.PREPROCESSED -> 0.6f
        }

        val qualityScore = (pointScore * 0.4f + sizeScore * 0.4f + strategyScore * 0.2f)

        Log.d(TAG, "Detection quality evaluation - Points: $pointScore, Size: $sizeScore, Strategy: $strategyScore, Final: $qualityScore")

        return qualityScore
    }

    /**
     * 获取检测策略的描述
     */
    fun getStrategyDescription(strategy: DetectionStrategy): String {
        return when (strategy) {
            DetectionStrategy.ENHANCED_TWO_STEP -> "增强二步检测：先基础检测获取边界框，再在提取区域进行精确检测"
            DetectionStrategy.DIRECT_MESH -> "直接网格检测：直接在原图上进行面部网格检测"
            DetectionStrategy.MULTI_RESOLUTION -> "多分辨率检测：在不同分辨率下尝试检测"
            DetectionStrategy.PREPROCESSED -> "预处理检测：对图像进行预处理后再检测"
        }
    }
}
