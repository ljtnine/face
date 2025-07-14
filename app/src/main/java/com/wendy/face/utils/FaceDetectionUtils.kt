package com.wendy.face.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.pow

/**
 * 人脸检测工具类
 * 提供图像预处理、坐标转换和辅助功能
 */
object FaceDetectionUtils {
    
    private const val TAG = "FaceDetectionUtils"
    
    /**
     * 坐标转换结果
     */
    data class CoordinateTransform(
        val scaleX: Float,
        val scaleY: Float,
        val offsetX: Float,
        val offsetY: Float,
        val mirrorX: Boolean = false
    )
    
    /**
     * 检查图像是否适合进行人脸检测
     * @param bitmap 要检查的图像
     * @return 是否适合检测
     */
    fun isImageSuitableForDetection(bitmap: Bitmap?): Boolean {
        if (bitmap == null) {
            Log.w(TAG, "Bitmap is null")
            return false
        }
        
        if (bitmap.isRecycled) {
            Log.w(TAG, "Bitmap is recycled")
            return false
        }
        
        val minSize = 100 // 最小尺寸
        if (bitmap.width < minSize || bitmap.height < minSize) {
            Log.w(TAG, "Image too small: ${bitmap.width}x${bitmap.height}")
            return false
        }
        
        val maxSize = 4096 // 最大尺寸
        if (bitmap.width > maxSize || bitmap.height > maxSize) {
            Log.w(TAG, "Image too large: ${bitmap.width}x${bitmap.height}")
            return false
        }
        
        return true
    }
    
    /**
     * 预处理图像以提高检测成功率
     * @param bitmap 原始图像
     * @return 预处理后的图像
     */
    fun preprocessImageForDetection(bitmap: Bitmap): Bitmap {
        Log.d(TAG, "Preprocessing image for detection - Original size: ${bitmap.width}x${bitmap.height}")
        
        // 如果图像过大，缩放到合适的尺寸以提高检测速度和成功率
        val maxDetectionSize = 1280
        return if (bitmap.width > maxDetectionSize || bitmap.height > maxDetectionSize) {
            val scale = minOf(maxDetectionSize.toFloat() / bitmap.width, maxDetectionSize.toFloat() / bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            
            Log.d(TAG, "Scaling image for detection: ${newWidth}x${newHeight} (scale: $scale)")
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            Log.d(TAG, "Image size is suitable for detection, no scaling needed")
            bitmap
        }
    }
    
    /**
     * 创建用于检测的InputImage，确保正确处理旋转
     * @param bitmap 图像
     * @param rotationDegrees 旋转角度
     * @return InputImage对象
     */
    fun createInputImageForDetection(bitmap: Bitmap, rotationDegrees: Int = 0): InputImage? {
        return try {
            Log.d(TAG, "Creating InputImage - Size: ${bitmap.width}x${bitmap.height}, Rotation: $rotationDegrees")
            InputImage.fromBitmap(bitmap, rotationDegrees)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create InputImage", e)
            null
        }
    }
    
    /**
     * 验证人脸检测结果的有效性
     * @param faceMeshes 检测结果
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 过滤后的有效人脸列表
     */
    fun validateDetectionResults(
        faceMeshes: List<FaceMesh>,
        imageWidth: Int,
        imageHeight: Int
    ): List<FaceMesh> {
        Log.d(TAG, "Validating ${faceMeshes.size} detected faces")
        
        val validFaces = faceMeshes.filter { faceMesh ->
            val boundingBox = faceMesh.boundingBox
            
            // 检查边界框是否在图像范围内
            val isInBounds = boundingBox.left >= 0 && 
                           boundingBox.top >= 0 && 
                           boundingBox.right <= imageWidth && 
                           boundingBox.bottom <= imageHeight
            
            // 检查人脸尺寸是否合理
            val minFaceSize = minOf(imageWidth, imageHeight) * 0.1f // 至少占图像的10%
            val faceSize = minOf(boundingBox.width(), boundingBox.height())
            val isSizeValid = faceSize >= minFaceSize
            
            // 检查关键点数量
            val hasEnoughPoints = faceMesh.allPoints.size >= 400 // ML Kit通常返回468个点
            
            val isValid = isInBounds && isSizeValid && hasEnoughPoints
            
            Log.d(TAG, "Face validation - Bounds: ${boundingBox.width()}x${boundingBox.height()}, " +
                      "InBounds: $isInBounds, SizeValid: $isSizeValid, Points: ${faceMesh.allPoints.size}, Valid: $isValid")
            
            isValid
        }
        
        Log.d(TAG, "Validation complete - Valid faces: ${validFaces.size}/${faceMeshes.size}")
        return validFaces
    }
    
    /**
     * 计算人脸检测的置信度分数
     * @param faceMesh 人脸网格
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 置信度分数 (0.0 - 1.0)
     */
    fun calculateConfidenceScore(faceMesh: FaceMesh, imageWidth: Int, imageHeight: Int): Float {
        val boundingBox = faceMesh.boundingBox
        val pointCount = faceMesh.allPoints.size
        
        // 基于人脸大小的分数
        val faceArea = boundingBox.width() * boundingBox.height()
        val imageArea = imageWidth * imageHeight
        val sizeScore = minOf(1.0f, (faceArea.toFloat() / imageArea) * 10) // 人脸占图像面积的比例
        
        // 基于关键点数量的分数
        val pointScore = minOf(1.0f, pointCount / 468.0f) // ML Kit标准点数
        
        // 基于边界框位置的分数（中心位置得分更高）
        val centerX = boundingBox.centerX()
        val centerY = boundingBox.centerY()
        val imageCenterX = imageWidth / 2f
        val imageCenterY = imageHeight / 2f
        
        val distanceFromCenter = kotlin.math.sqrt(
            ((centerX - imageCenterX) / imageCenterX).toDouble().pow(2.0) +
            ((centerY - imageCenterY) / imageCenterY).toDouble().pow(2.0)
        ).toFloat()
        
        val positionScore = maxOf(0.0f, 1.0f - distanceFromCenter)
        
        // 综合分数
        val confidence = (sizeScore * 0.4f + pointScore * 0.4f + positionScore * 0.2f)
        
        Log.d(TAG, "Confidence calculation - Size: $sizeScore, Points: $pointScore, Position: $positionScore, Final: $confidence")
        
        return confidence
    }
    
    /**
     * 选择最佳的人脸检测结果
     * @param faceMeshes 所有检测到的人脸
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 最佳的人脸，如果没有合适的则返回null
     */
    fun selectBestFace(faceMeshes: List<FaceMesh>, imageWidth: Int, imageHeight: Int): FaceMesh? {
        if (faceMeshes.isEmpty()) return null
        
        val validFaces = validateDetectionResults(faceMeshes, imageWidth, imageHeight)
        if (validFaces.isEmpty()) return null
        
        // 如果只有一个有效人脸，直接返回
        if (validFaces.size == 1) return validFaces.first()
        
        // 选择置信度最高的人脸
        val bestFace = validFaces.maxByOrNull { faceMesh ->
            calculateConfidenceScore(faceMesh, imageWidth, imageHeight)
        }
        
        Log.d(TAG, "Selected best face from ${validFaces.size} valid faces")
        return bestFace
    }
    
    /**
     * 计算从检测坐标系到显示坐标系的转换参数
     * @param detectionImageWidth 检测时使用的图像宽度
     * @param detectionImageHeight 检测时使用的图像高度
     * @param displayImageWidth 显示时的图像宽度
     * @param displayImageHeight 显示时的图像高度
     * @param viewWidth 视图宽度
     * @param viewHeight 视图高度
     * @param isFrontCamera 是否为前置摄像头
     * @param isPreviewMode 是否为预览模式
     * @return 坐标转换参数
     */
    fun calculateCoordinateTransform(
        detectionImageWidth: Int,
        detectionImageHeight: Int,
        displayImageWidth: Int,
        displayImageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        isFrontCamera: Boolean = false,
        isPreviewMode: Boolean = false
    ): CoordinateTransform {
        Log.d(TAG, "Calculating coordinate transform:")
        Log.d(TAG, "  Detection image: ${detectionImageWidth}x${detectionImageHeight}")
        Log.d(TAG, "  Display image: ${displayImageWidth}x${displayImageHeight}")
        Log.d(TAG, "  View: ${viewWidth}x${viewHeight}")
        Log.d(TAG, "  Front camera: $isFrontCamera, Preview: $isPreviewMode")
        
        // 1. 从检测坐标系到显示图像坐标系的缩放
        val detectionToDisplayScaleX = displayImageWidth.toFloat() / detectionImageWidth.toFloat()
        val detectionToDisplayScaleY = displayImageHeight.toFloat() / detectionImageHeight.toFloat()
        
        // 2. 计算图像在视图中的显示区域（ContentScale.Crop模式）
        val imageAspectRatio = displayImageWidth.toFloat() / displayImageHeight.toFloat()
        val viewAspectRatio = viewWidth / viewHeight
        
        val displayToViewScale: Float
        val offsetX: Float
        val offsetY: Float
        
        if (imageAspectRatio > viewAspectRatio) {
            // 图片比视图更宽，高度填满视图，宽度裁剪
            displayToViewScale = viewHeight / displayImageHeight.toFloat()
            val scaledImageWidth = displayImageWidth * displayToViewScale
            offsetX = (viewWidth - scaledImageWidth) / 2f
            offsetY = 0f
        } else {
            // 图片比视图更高，宽度填满视图，高度裁剪
            displayToViewScale = viewWidth / displayImageWidth.toFloat()
            val scaledImageHeight = displayImageHeight * displayToViewScale
            offsetX = 0f
            offsetY = (viewHeight - scaledImageHeight) / 2f
        }
        
        // 3. 最终的缩放比例
        val finalScaleX = detectionToDisplayScaleX * displayToViewScale
        val finalScaleY = detectionToDisplayScaleY * displayToViewScale
        
        // 4. 前置摄像头镜像处理
        val shouldMirror = isFrontCamera && isPreviewMode
        
        Log.d(TAG, "Transform result:")
        Log.d(TAG, "  ScaleX: $finalScaleX, ScaleY: $finalScaleY")
        Log.d(TAG, "  OffsetX: $offsetX, OffsetY: $offsetY")
        Log.d(TAG, "  Mirror: $shouldMirror")
        
        return CoordinateTransform(
            scaleX = finalScaleX,
            scaleY = finalScaleY,
            offsetX = offsetX,
            offsetY = offsetY,
            mirrorX = shouldMirror
        )
    }
    
    /**
     * 应用坐标转换到人脸关键点
     * @param faceMeshPoints 原始关键点列表
     * @param transform 坐标转换参数
     * @param viewWidth 视图宽度（用于镜像计算）
     * @return 转换后的关键点列表
     */
    fun transformFaceMeshPoints(
        faceMeshPoints: List<FaceMeshPoint>,
        transform: CoordinateTransform,
        viewWidth: Float
    ): List<Pair<Float, Float>> {
        return faceMeshPoints.map { point ->
            var x = point.position.x * transform.scaleX + transform.offsetX
            val y = point.position.y * transform.scaleY + transform.offsetY
            
            // 应用镜像变换
            if (transform.mirrorX) {
                x = viewWidth - x
            }
            
            Pair(x, y)
        }
    }
    
    /**
     * 优化图像用于特定设备
     * @param bitmap 原始图像
     * @param deviceInfo 设备信息（可选）
     * @return 优化后的图像
     */
    fun optimizeImageForDevice(bitmap: Bitmap, deviceInfo: String? = null): Bitmap {
        // 根据设备性能调整图像处理策略
        val isLowEndDevice = isLowEndDevice()
        
        return if (isLowEndDevice) {
            Log.d(TAG, "Optimizing image for low-end device")
            // 对于低端设备，进一步降低分辨率
            val maxSize = 800
            if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false) // 使用更快的缩放
            } else {
                bitmap
            }
        } else {
            Log.d(TAG, "Using standard image processing for high-end device")
            preprocessImageForDetection(bitmap)
        }
    }
    
    /**
     * 检测是否为低端设备
     * @return 是否为低端设备
     */
    private fun isLowEndDevice(): Boolean {
        // 简单的设备性能检测，可以根据需要扩展
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024) // MB
        
        Log.d(TAG, "Device max memory: ${maxMemory}MB")
        
        // 如果可用内存小于512MB，认为是低端设备
        return maxMemory < 512
    }
    
    /**
     * 创建用于不同检测策略的图像变体
     * @param originalBitmap 原始图像
     * @return 图像变体列表（原始、预处理、缩放）
     */
    fun createImageVariants(originalBitmap: Bitmap): List<Bitmap> {
        val variants = mutableListOf<Bitmap>()
        
        // 变体1：原始图像
        variants.add(originalBitmap)
        
        // 变体2：预处理图像
        try {
            val preprocessed = preprocessImageForDetection(originalBitmap)
            if (preprocessed != originalBitmap) {
                variants.add(preprocessed)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create preprocessed variant", e)
        }
        
        // 变体3：小尺寸图像
        try {
            val targetSize = 640
            val scale = minOf(targetSize.toFloat() / originalBitmap.width, targetSize.toFloat() / originalBitmap.height)
            if (scale < 1.0f) {
                val scaledWidth = (originalBitmap.width * scale).toInt()
                val scaledHeight = (originalBitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
                variants.add(scaled)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create scaled variant", e)
        }
        
        Log.d(TAG, "Created ${variants.size} image variants for detection")
        return variants
    }
}
