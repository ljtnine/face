package com.wendy.face.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import android.content.ContentResolver

/**
 * 图片处理工具类
 * 负责图片的加载、旋转、缩放等操作
 */
object ImageUtils {

    private const val TAG = "ImageUtils"

    /**
     * 从URI安全地加载Bitmap，处理旋转和内存问题
     */
    fun getBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        return try {
            // 使用输入流和BitmapFactory来解码图片
            contentResolver.openInputStream(uri)?.use { inputStream ->
                // 使用BitmapFactory.Options来检查图片尺寸，避免OOM
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                // 重置输入流
                contentResolver.openInputStream(uri)?.use { freshInputStream ->
                    // 计算采样率以缩放图片
                    options.inSampleSize = calculateInSampleSize(options, 800, 800) // 限制最大尺寸
                    options.inJustDecodeBounds = false
                    BitmapFactory.decodeStream(freshInputStream, null, options)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading bitmap from URI: $uri", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory error loading bitmap from URI: $uri", e)
            null
        }
    }

    /**
     * 从URI加载图片并自动处理旋转和镜像
     * @param context 上下文
     * @param uri 图片URI
     * @param needMirror 是否需要镜像翻转（前置摄像头需要）
     * @return 处理后的Bitmap，如果失败返回null
     */
    fun loadAndRotateBitmap(context: Context, uri: Uri, isFrontCamera: Boolean = false): Bitmap? {
        return try {
            // 1. 加载Bitmap
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                return null
            }

            // 2. 读取Exif方向信息
            val exifInputStream = context.contentResolver.openInputStream(uri)
            val exif = exifInputStream?.let { ExifInterface(it) }
            exifInputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            // 3. 计算旋转和镜像
            val matrix = Matrix()
            var rotationAngle = 0f
            var flipHorizontal = false
            var flipVertical = false

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotationAngle = 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> rotationAngle = 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> rotationAngle = 270f
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipHorizontal = true
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipVertical = true
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    rotationAngle = 90f
                    flipHorizontal = true
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    rotationAngle = 270f
                    flipHorizontal = true
                }
            }

            // 对于前置摄像头，默认会有一个水平镜像，Exif可能不会记录。
            // 如果Exif已经要求水平翻转，我们就不再处理，避免双重翻转。
            if (isFrontCamera && !flipHorizontal) {
                flipHorizontal = true
            }

            Log.d(TAG, "Exif Orientation: $orientation, Rotation: $rotationAngle, FlipH: $flipHorizontal, FlipV: $flipVertical, IsFront: $isFrontCamera")

            // 4. 应用变换
            if (rotationAngle != 0f) {
                matrix.postRotate(rotationAngle)
            }
            if (flipHorizontal || flipVertical) {
                matrix.postScale(
                    if (flipHorizontal) -1f else 1f,
                    if (flipVertical) -1f else 1f,
                    bitmap.width / 2f,
                    bitmap.height / 2f
                )
            }

            if (rotationAngle != 0f || flipHorizontal || flipVertical) {
                val transformedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (transformedBitmap != bitmap) {
                    bitmap.recycle()
                }
                Log.d(TAG, "Bitmap transformed. New size: ${transformedBitmap.width}x${transformedBitmap.height}")
                transformedBitmap
            } else {
                Log.d(TAG, "No transformation needed.")
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading and transforming bitmap", e)
            null
        }
    }

    /**
     * 计算BitmapFactory的inSampleSize值
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 创建测试图片
     * @return 测试用的Bitmap
     */
    fun createTestBitmap(): Bitmap {
        // 创建一个简单的测试图片（纯色图片用于测试坐标系统）
        val testBitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        testBitmap.eraseColor(android.graphics.Color.BLUE)

        // 在图片上绘制一些测试图形
        val canvas = android.graphics.Canvas(testBitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.RED
            strokeWidth = 5f
            style = android.graphics.Paint.Style.STROKE
        }

        // 绘制一个矩形作为"假人脸"用于测试坐标
        canvas.drawRect(100f, 75f, 300f, 225f, paint)

        // 绘制一些圆点作为"假关键点"
        val pointPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.YELLOW
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(150f, 125f, 8f, pointPaint) // 左眼
        canvas.drawCircle(250f, 125f, 8f, pointPaint) // 右眼
        canvas.drawCircle(200f, 150f, 8f, pointPaint) // 鼻子
        canvas.drawCircle(200f, 175f, 8f, pointPaint) // 嘴巴

        Log.d(TAG, "Created test bitmap: ${testBitmap.width} x ${testBitmap.height}")
        return testBitmap
    }

    /**
     * 计算图片在视图中的显示区域（考虑ContentScale.Fit）
     * @param imageWidth 图片宽度
     * @param imageHeight 图片高度
     * @param viewWidth 视图宽度
     * @param viewHeight 视图高度
     * @return Array<Float> [displayWidth, displayHeight, offsetX, offsetY]
     */
    fun calculateDisplayBounds(
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float
    ): Array<Float> {
        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val viewAspectRatio = viewWidth / viewHeight

        return if (imageAspectRatio > viewAspectRatio) {
            // 图片更宽，以宽度为准
            val displayWidth = viewWidth
            val displayHeight = viewWidth / imageAspectRatio
            val offsetY = (viewHeight - displayHeight) / 2f
            arrayOf(displayWidth, displayHeight, 0f, offsetY)
        } else {
            // 图片更高，以高度为准
            val displayHeight = viewHeight
            val displayWidth = viewHeight * imageAspectRatio
            val offsetX = (viewWidth - displayWidth) / 2f
            arrayOf(displayWidth, displayHeight, offsetX, 0f)
        }
    }

    /**
     * 根据人脸检测结果裁剪头部区域
     * @param bitmap 原始图片
     * @param faceBoundingBoxes 检测到的人脸边界框列表
     * @return 裁剪后的头部图片，如果没有检测到人脸则返回原图
     */
    fun cropHeadRegion(bitmap: Bitmap, faceBoundingBoxes: List<Rect>): Bitmap {
        if (faceBoundingBoxes.isEmpty()) {
            Log.w(TAG, "No faces detected, returning original bitmap")
            return bitmap
        }

        try {
            // 仅处理检测到的第一个人脸
            val boundingBox = faceBoundingBoxes.first()

            val faceWidth = boundingBox.width()
            val faceHeight = boundingBox.height()

            // 定义扩展因子
            val topExpansion = (faceHeight * 0.5f).toInt()      // 向上扩展50%以包含头发
            val bottomExpansion = (faceHeight * 0.1f).toInt()   // 向下扩展10%以包含下巴
            val horizontalExpansion = (faceWidth * 0.2f).toInt()// 水平扩展20%

            // 基于人脸边界框进行扩展，计算新的裁剪区域
            val cropLeft = max(0, boundingBox.left - horizontalExpansion)
            val cropTop = max(0, boundingBox.top - topExpansion)
            val cropRight = min(bitmap.width, boundingBox.right + horizontalExpansion)
            val cropBottom = min(bitmap.height, boundingBox.bottom + bottomExpansion)

            val cropWidth = cropRight - cropLeft
            val cropHeight = cropBottom - cropTop

            if (cropWidth <= 0 || cropHeight <= 0) {
                Log.w(TAG, "Invalid crop dimensions, returning original bitmap")
                return bitmap
            }

            Log.d(TAG, "Original bitmap: ${bitmap.width}x${bitmap.height}")
            Log.d(TAG, "Face BBox: ${boundingBox}")
            Log.d(TAG, "Crop region: ($cropLeft, $cropTop) to ($cropRight, $cropBottom)")
            Log.d(TAG, "Crop size: ${cropWidth}x${cropHeight}")

            // 执行裁剪
            return Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)

        } catch (e: Exception) {
            Log.e(TAG, "Error cropping head region", e)
            return bitmap
        }
    }

    /**
     * 将ImageProxy转换为Bitmap
     * @param image ImageProxy对象
     * @return 转换后的Bitmap
     */
    fun imageProxyToBitmap(image: androidx.camera.core.ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * 增强图像质量用于人脸检测
     * @param bitmap 原始图像
     * @return 增强后的图像
     */
    fun enhanceImageForFaceDetection(bitmap: Bitmap): Bitmap {
        return try {
            Log.d(TAG, "Enhancing image for face detection: ${bitmap.width}x${bitmap.height}")

            // 创建ColorMatrix用于调整对比度和亮度
            val colorMatrix = ColorMatrix().apply {
                // 增加对比度 (1.2倍)
                val contrast = 1.2f
                val brightness = 10f // 轻微增加亮度

                set(floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
            }

            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            val enhancedBitmap = Bitmap.createBitmap(
                bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(enhancedBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            Log.d(TAG, "Image enhancement completed")
            enhancedBitmap

        } catch (e: Exception) {
            Log.e(TAG, "Failed to enhance image", e)
            bitmap // 返回原图
        }
    }

    /**
     * 智能调整图像分辨率用于人脸检测
     * @param bitmap 原始图像
     * @param targetSize 目标尺寸（较长边）
     * @return 调整后的图像
     */
    fun smartResizeForFaceDetection(bitmap: Bitmap, targetSize: Int = 1024): Bitmap {
        return try {
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height
            val maxDimension = maxOf(originalWidth, originalHeight)

            Log.d(TAG, "Smart resize: original=${originalWidth}x${originalHeight}, target=$targetSize")

            // 如果图像已经合适，直接返回
            if (maxDimension <= targetSize && maxDimension >= targetSize * 0.8) {
                Log.d(TAG, "Image size is already optimal")
                return bitmap
            }

            // 计算缩放比例
            val scale = targetSize.toFloat() / maxDimension
            val newWidth = (originalWidth * scale).toInt()
            val newHeight = (originalHeight * scale).toInt()

            // 使用高质量缩放
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            Log.d(TAG, "Image resized to ${newWidth}x${newHeight}")
            resizedBitmap

        } catch (e: Exception) {
            Log.e(TAG, "Failed to resize image", e)
            bitmap // 返回原图
        }
    }

    /**
     * 设备兼容性优化的图像处理
     * @param bitmap 原始图像
     * @return 优化后的图像
     */
    fun optimizeForDeviceCompatibility(bitmap: Bitmap): Bitmap {
        return try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / (1024 * 1024) // MB
            val isLowEndDevice = maxMemory < 512

            Log.d(TAG, "Device memory: ${maxMemory}MB, isLowEnd: $isLowEndDevice")

            if (isLowEndDevice) {
                // 低端设备：更激进的优化
                val maxSize = 800
                val optimizedBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    smartResizeForFaceDetection(bitmap, maxSize)
                } else {
                    bitmap
                }

                // 转换为RGB_565格式以节省内存
                if (optimizedBitmap.config != Bitmap.Config.RGB_565) {
                    val rgb565Bitmap = optimizedBitmap.copy(Bitmap.Config.RGB_565, false)
                    if (rgb565Bitmap != optimizedBitmap && optimizedBitmap != bitmap) {
                        optimizedBitmap.recycle()
                    }
                    rgb565Bitmap
                } else {
                    optimizedBitmap
                }
            } else {
                // 高端设备：标准优化
                smartResizeForFaceDetection(bitmap, 1280)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to optimize for device compatibility", e)
            bitmap
        }
    }

    /**
     * 综合图像预处理用于人脸检测
     * @param bitmap 原始图像
     * @param enhanceQuality 是否增强图像质量
     * @return 预处理后的图像
     */
    fun preprocessForFaceDetection(bitmap: Bitmap, enhanceQuality: Boolean = true): Bitmap {
        return try {
            Log.d(TAG, "Starting comprehensive image preprocessing")

            // 步骤1: 设备兼容性优化（调整分辨率和格式）
            var processedBitmap = optimizeForDeviceCompatibility(bitmap)

            // 步骤2: 图像质量增强（如果需要）
            if (enhanceQuality) {
                val enhancedBitmap = enhanceImageForFaceDetection(processedBitmap)
                if (enhancedBitmap != processedBitmap && processedBitmap != bitmap) {
                    processedBitmap.recycle()
                }
                processedBitmap = enhancedBitmap
            }

            Log.d(TAG, "Image preprocessing completed: ${processedBitmap.width}x${processedBitmap.height}")
            processedBitmap

        } catch (e: Exception) {
            Log.e(TAG, "Failed to preprocess image", e)
            bitmap
        }
    }

    /**
     * 将Bitmap保存到相册
     * @param context 上下文
     * @param bitmap 要保存的Bitmap
     * @param displayName 文件显示名称
     * @return 保存后的图片URI，失败则返回null
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FaceDetection")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        var uri: Uri? = null
        try {
            uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it).use { outputStream ->
                    if (outputStream == null) {
                        throw java.io.IOException("Failed to get output stream.")
                    }
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw java.io.IOException("Failed to save bitmap.")
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                Log.d(TAG, "Bitmap saved to gallery: $it")
            }
            return uri
        } catch (e: Exception) {
            uri?.let {
                // 如果发生错误，删除不完整的条目
                resolver.delete(it, null, null)
            }
            Log.e(TAG, "Failed to save bitmap to gallery", e)
            return null
        }
    }
}
