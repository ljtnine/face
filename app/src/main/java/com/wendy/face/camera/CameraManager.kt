package com.wendy.face.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.wendy.face.detection.FaceDetectorManager
import com.wendy.face.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * 相机管理器
 * 负责相机的初始化、配置和拍照功能
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "CameraManager"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    private val faceDetectorManager = FaceDetectorManager()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var currentIsBackCamera: Boolean = true

    /**
     * 绑定相机到PreviewView
     * @param previewView 预览视图
     * @param isBackCamera 是否使用后置摄像头
     * @param onFacesDetected 人脸网格检测回调
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun bindCamera(
        previewView: PreviewView,
        isBackCamera: Boolean,
        onFacesDetected: (List<FaceMesh>, Int, Int) -> Unit
    ) {
        Log.d(TAG, "bindCamera called, isBackCamera: $isBackCamera")
        currentIsBackCamera = isBackCamera // 记录当前摄像头类型
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "CameraProvider obtained")

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    // 设置目标分辨率以避免过大的图片导致检测失败
                    .setTargetResolution(android.util.Size(1280, 720))
                    .build()

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    // 设置目标分辨率，降低处理负担
                    .setTargetResolution(android.util.Size(640, 480))
                    .build()
                    .also {
                        // 添加处理状态跟踪
                        var isProcessing = false
                        var lastProcessTime = 0L
                        val minProcessInterval = 100L // 最小处理间隔100ms，限制帧率

                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            val currentTime = System.currentTimeMillis()

                            // 如果正在处理或距离上次处理时间太短，跳过此帧
                            if (isProcessing || (currentTime - lastProcessTime) < minProcessInterval) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            isProcessing = true
                            lastProcessTime = currentTime

                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            val image = imageProxy.image
                            if (image != null) {
                                val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
                                faceDetectorManager.detectFaces(
                                    inputImage,
                                    onSuccess = { faceMeshes, width, height ->
                                        onFacesDetected(faceMeshes, width, height)
                                        isProcessing = false
                                        imageProxy.close()
                                    },
                                    onFailure = { e ->
                                        Log.e(TAG, "Face mesh detection failed during preview", e)
                                        isProcessing = false
                                        imageProxy.close()
                                    }
                                )
                            } else {
                                isProcessing = false
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
                Log.d(TAG, "Camera bound successfully")
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 拍照
     * @param onImageCaptured 图片捕获回调
     */
    fun takePicture(
        onImageCaptured: (Uri?, Bitmap?) -> Unit
    ) {
        val capture = imageCapture
        if (capture == null) {
            Log.w(TAG, "ImageCapture is null")
            onImageCaptured(null, null)
            return
        }

        Log.d(TAG, "Taking picture...")

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    Log.d(TAG, "Photo capture success in memory.")
                    try {
                        // 1. 将ImageProxy转换为Bitmap
                        val sourceBitmap = ImageUtils.imageProxyToBitmap(image)

                        // 2. 分步进行变换：先旋转，再镜像，确保坐标系正确
                        // 步骤 2.1: 旋转
                        val rotationMatrix = android.graphics.Matrix().apply {
                            postRotate(image.imageInfo.rotationDegrees.toFloat())
                        }
                        val rotatedBitmap = Bitmap.createBitmap(
                            sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, rotationMatrix, true
                        )
                        if (rotatedBitmap != sourceBitmap) {
                            sourceBitmap.recycle()
                        }

                        // 步骤 2.2: 如果是前置摄像头，在旋转后的图片上进行镜像
                        val finalBitmap = if (!currentIsBackCamera) {
                            val mirrorMatrix = android.graphics.Matrix().apply {
                                postScale(-1f, 1f, rotatedBitmap.width / 2f, rotatedBitmap.height / 2f)
                            }
                            val mirroredBitmap = Bitmap.createBitmap(
                                rotatedBitmap, 0, 0, rotatedBitmap.width, rotatedBitmap.height, mirrorMatrix, true
                            )
                            if (mirroredBitmap != rotatedBitmap) {
                                rotatedBitmap.recycle()
                            }
                            mirroredBitmap
                        } else {
                            rotatedBitmap
                        }

                        // 4. 保存修正后的Bitmap到相册
                        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                            .format(System.currentTimeMillis())
                        val savedUri = ImageUtils.saveBitmapToGallery(context, finalBitmap, name)

                        // 5. 在主线程回调
                        ContextCompat.getMainExecutor(context).execute {
                            if (savedUri != null) {
                                Log.d(TAG, "Photo saved and processed successfully: $savedUri")
                                onImageCaptured(savedUri, finalBitmap)
                            } else {
                                Log.e(TAG, "Failed to save processed photo to gallery.")
                                // 即使保存失败，也返回处理后的Bitmap用于显示
                                onImageCaptured(null, finalBitmap)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing captured image", e)
                        ContextCompat.getMainExecutor(context).execute {
                            onImageCaptured(null, null)
                        }
                    } finally {
                        image.close() // 确保关闭ImageProxy
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    ContextCompat.getMainExecutor(context).execute {
                        onImageCaptured(null, null)
                    }
                }
            }
        )
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            cameraProvider?.unbindAll()
            faceDetectorManager.release()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera resources", e)
        }
    }
}
