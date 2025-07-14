package com.wendy.face

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.wendy.face.analyzer.FaceAnalyzer
import com.wendy.face.analyzer.PalaceAnalysisResult
import com.wendy.face.detection.FaceDetectorManager
import com.wendy.face.ui.components.CameraView
import com.wendy.face.ui.components.FaceOverlay
import com.wendy.face.ui.screens.FaceAnalysisScreen
import com.wendy.face.ui.screens.SettingsScreen
import com.wendy.face.ui.theme.FaceTheme
import com.wendy.face.ui.components.FaceDetectionLoading
import com.wendy.face.ui.components.DetectionState
import com.wendy.face.utils.FaceExtractionUtils
import com.wendy.face.utils.ImageUtils
import com.wendy.face.utils.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper


/**
 * 主Activity - 重构后的简化版本
 * 负责权限管理和UI状态协调
 */
class MainActivity : ComponentActivity() {

    // 权限请求启动器
    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { entry ->
                val permission = entry.key
                val isGranted = entry.value
                if (isGranted) {
                    Log.d("MainActivity", "Permission granted: $permission")
                } else {
                    Log.w("MainActivity", "Permission denied: $permission")
                }
            }
        }

    // 用于处理图片选择的回调
    private var onImageSelected: ((Uri) -> Unit)? = null

    // 图片选择启动器
    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                Log.d("MainActivity", "Image selected from gallery: $it")
                onImageSelected?.invoke(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全面屏模式
        setupFullScreenMode()

        // 检查并请求权限
        val permissionsToRequest = PermissionUtils.getPermissionsToRequest(this)
        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest)
        }

        setContent {
            FaceTheme {
                FaceDetectionApp(
                    onGalleryClick = {
                        selectImageLauncher.launch("image/*")
                    },
                    setOnImageSelectedListener = { listener ->
                        this.onImageSelected = listener
                    }
                )
            }
        }
    }

    /**
     * 设置全面屏模式，隐藏状态栏和导航栏
     */
    private fun setupFullScreenMode() {
        try {
            // 启用边到边显示
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ 使用新的WindowInsetsController
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                // Android 10及以下版本
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
            }

            // 设置窗口标志
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to setup full screen mode", e)
            // 如果全面屏设置失败，至少隐藏状态栏
            try {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
            } catch (ex: Exception) {
                Log.e("MainActivity", "Failed to hide status bar", ex)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放人脸检测相关资源
        try {
            FaceExtractionUtils.release()
            Log.d("MainActivity", "FaceExtractionUtils resources released")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error releasing FaceExtractionUtils resources", e)
        }
    }
}

/**
 * 人脸检测应用主界面
 */
@Composable
fun FaceDetectionApp(
    onGalleryClick: () -> Unit,
    setOnImageSelectedListener: (listener: (Uri) -> Unit) -> Unit
) {
    // 状态管理
    var faceMeshes by remember { mutableStateOf<List<FaceMesh>>(emptyList()) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }
    // 暂时移除复杂的检测结果状态，专注于稳定化
    var isBackCamera by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedFaceMeshes by remember { mutableStateOf<List<FaceMesh>>(emptyList()) }
    var capturedImageWidth by remember { mutableStateOf(0) }
    var capturedImageHeight by remember { mutableStateOf(0) }
    var showCamera by remember { mutableStateOf(true) }
    val faceDetectorManager by remember { mutableStateOf(FaceDetectorManager()) }
    var showCroppedImage by remember { mutableStateOf(false) }
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 新增状态
    val faceAnalyzer by remember { mutableStateOf(FaceAnalyzer()) }
    var analysisResults by remember { mutableStateOf<List<PalaceAnalysisResult>>(emptyList()) }
    var showAnalysisScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Loading状态管理
    var isDetectionLoading by remember { mutableStateOf(false) }
    var detectionState by remember { mutableStateOf(DetectionState.DETECTING) }
    var detectionProgress by remember { mutableStateOf(0f) }



    val context = androidx.compose.ui.platform.LocalContext.current

    // 设置图片选择监听器
    LaunchedEffect(Unit) {
        setOnImageSelectedListener { uri ->
            // 从相册选择图片后的处理逻辑
            val bitmap = ImageUtils.getBitmapFromUri(context.contentResolver, uri)
            if (bitmap != null) {
                Log.d("FaceDetectionApp", "Bitmap loaded from gallery: ${bitmap.width}x${bitmap.height}")
                capturedImageUri = uri
                capturedBitmap = bitmap
                showCamera = false
                // 先显示Loading，不立即显示分析界面
                isDetectionLoading = true
                detectionState = DetectionState.PREPROCESSING
                detectionProgress = 0f
            } else {
                Log.e("FaceDetectionApp", "Failed to load bitmap from URI: $uri")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isDetectionLoading && capturedBitmap != null -> {
                // 显示Loading界面并执行检测
                LaunchedEffect(capturedBitmap) {
                    val bmp = capturedBitmap ?: return@LaunchedEffect
                    Log.d("FaceDetectionApp", "Starting enhanced face detection analysis")

                    try {
                        // 阶段1: 图像预处理
                        detectionState = DetectionState.PREPROCESSING
                        detectionProgress = 0.1f
                        kotlinx.coroutines.delay(300) // 给用户看到预处理状态的时间

                        val preprocessedBitmap = ImageUtils.preprocessForFaceDetection(bmp, enhanceQuality = true)
                        detectionProgress = 0.3f

                        // 阶段2: 人脸检测
                        detectionState = DetectionState.DETECTING
                        kotlinx.coroutines.delay(200)

                        // 使用智能人脸检测
                        FaceExtractionUtils.smartFaceDetection(
                            bitmap = preprocessedBitmap,
                            onSuccess = { detectionResult ->
                                Log.d("FaceDetectionApp", "Enhanced detection successful: ${detectionResult.faceMeshes.size} faces found using ${detectionResult.strategy}")

                                // 更新检测进度
                                detectionProgress = 0.7f

                                // 阶段3: 面相分析
                                detectionState = DetectionState.ANALYZING

                                // 更新检测结果
                                capturedFaceMeshes = detectionResult.faceMeshes
                                capturedImageWidth = detectionResult.imageWidth
                                capturedImageHeight = detectionResult.imageHeight

                                // 进行面相分析
                                if (detectionResult.faceMeshes.isNotEmpty()) {
                                    analysisResults = faceAnalyzer.analyze(detectionResult.faceMeshes.first())
                                    Log.d("FaceDetectionApp", "Face analysis completed with ${analysisResults.size} palace results")
                                } else {
                                    analysisResults = emptyList()
                                }

                                // 记录检测质量
                                val quality = FaceExtractionUtils.evaluateDetectionQuality(detectionResult)
                                Log.d("FaceDetectionApp", "Detection quality score: $quality")

                                // 完成检测
                                detectionProgress = 1f
                                detectionState = DetectionState.COMPLETED

                                // 延迟一下再切换到分析界面，让用户看到完成状态
                                Handler(Looper.getMainLooper()).postDelayed({
                                    isDetectionLoading = false
                                    showAnalysisScreen = true
                                }, 500)

                                // 回收预处理的bitmap（如果不是原始bitmap）
                                if (preprocessedBitmap != bmp) {
                                    preprocessedBitmap.recycle()
                                }
                            },
                            onFailure = { e ->
                                Log.e("FaceDetectionApp", "Enhanced detection failed", e)

                                // 回退到传统检测方法
                                Log.d("FaceDetectionApp", "Falling back to traditional detection")
                                detectionProgress = 0.5f

                                val inputImage = InputImage.fromBitmap(bmp, 0)
                                faceDetectorManager.detectFaces(
                                    inputImage = inputImage,
                                    onSuccess = { detectedFaceMeshes, width, height ->
                                        capturedFaceMeshes = detectedFaceMeshes
                                        capturedImageWidth = width
                                        capturedImageHeight = height
                                        Log.d("FaceDetectionApp", "Fallback detection: ${detectedFaceMeshes.size} faces found")

                                        // 阶段3: 面相分析
                                        detectionState = DetectionState.ANALYZING
                                        detectionProgress = 0.8f

                                        if (detectedFaceMeshes.isNotEmpty()) {
                                            analysisResults = faceAnalyzer.analyze(detectedFaceMeshes.first())
                                        } else {
                                            analysisResults = emptyList()
                                        }

                                        // 完成检测
                                        detectionProgress = 1f
                                        detectionState = DetectionState.COMPLETED
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            isDetectionLoading = false
                                            showAnalysisScreen = true
                                        }, 500)
                                    },
                                    onFailure = { fallbackError ->
                                        Log.e("FaceDetectionApp", "Fallback detection also failed", fallbackError)
                                        capturedFaceMeshes = emptyList()
                                        analysisResults = emptyList()

                                        // 检测失败，仍然进入分析界面但显示无人脸状态
                                        detectionProgress = 1f
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            isDetectionLoading = false
                                            showAnalysisScreen = true
                                        }, 500)
                                    }
                                )

                                // 回收预处理的bitmap
                                if (preprocessedBitmap != bmp) {
                                    preprocessedBitmap.recycle()
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("FaceDetectionApp", "Detection process failed", e)
                        // 异常情况下也要结束Loading状态
                        isDetectionLoading = false
                        showAnalysisScreen = true
                    }
                }

                // 显示Loading界面
                FaceDetectionLoading(
                    capturedBitmap = capturedBitmap!!,
                    detectionState = detectionState,
                    progress = detectionProgress
                )
            }
            showAnalysisScreen && capturedBitmap != null -> {
                FaceAnalysisScreen(
                    capturedImageUri = capturedImageUri!!,
                    capturedBitmap = capturedBitmap!!,
                    faceMeshes = capturedFaceMeshes,
                    analysisResults = analysisResults,
                    isBackCamera = isBackCamera,
                    onBack = {
                        // 重置状态以返回相机
                        showCamera = true
                        showAnalysisScreen = false
                        isDetectionLoading = false
                        capturedImageUri = null
                        capturedBitmap = null
                        capturedFaceMeshes = emptyList()
                        analysisResults = emptyList()
                        detectionProgress = 0f
                    },
                    onReanalyze = {
                        // 重新分析当前照片的逻辑保持不变
                        capturedBitmap?.let { bmp ->
                            Log.d("FaceDetectionApp", "Manual reanalysis triggered")
                            val inputImage = InputImage.fromBitmap(bmp, 0)
                            faceDetectorManager.detectFaces(
                                inputImage = inputImage,
                                onSuccess = { detectedFaceMeshes, width, height ->
                                    capturedFaceMeshes = detectedFaceMeshes
                                    capturedImageWidth = width
                                    capturedImageHeight = height
                                    if (detectedFaceMeshes.isNotEmpty()) {
                                        analysisResults = faceAnalyzer.analyze(detectedFaceMeshes.first())
                                    } else {
                                        analysisResults = emptyList()
                                    }
                                },
                                onFailure = { e ->
                                    Log.e("FaceDetectionApp", "Manual reanalysis failed", e)
                                    analysisResults = emptyList()
                                }
                            )
                        }
                    }
                )
            }
            showSettingsScreen -> {
                SettingsScreen(onBack = { showSettingsScreen = false })
            }
            showCamera -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraView(
                        isBackCamera = isBackCamera,
                        onFacesDetected = { detectedFaceMeshes, width, height ->
                            faceMeshes = detectedFaceMeshes
                            imageWidth = width
                            imageHeight = height
                        },
                        onCameraSwitch = { isBackCamera = !isBackCamera },
                        onImageCaptured = { uri, bitmap ->
                            capturedImageUri = uri
                            showCamera = false
                            bitmap?.let { bmp ->
                                val processedBitmap = if (bmp.width > 640 || bmp.height > 640) {
                                    val scale = minOf(640f / bmp.width, 640f / bmp.height)
                                    val newWidth = (bmp.width * scale).toInt()
                                    val newHeight = (bmp.height * scale).toInt()
                                    Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true)
                                } else {
                                    bmp
                                }
                                capturedBitmap = processedBitmap
                                // 开始Loading流程
                                isDetectionLoading = true
                                detectionState = DetectionState.PREPROCESSING
                                detectionProgress = 0f
                            }
                        },
                        onGalleryClick = onGalleryClick
                    )

                    IconButton(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }
            else -> {
                // Fallback: 显示拍摄的照片，处理分析前或失败的场景
                capturedImageUri?.let { uri ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 直接从相册URI加载显示图片，不进行镜像（镜像在坐标转换时处理）
                        AsyncImage(
                            model = uri,
                            contentDescription = "Captured Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // 在照片上叠加人脸框，确保它在Image组件之上
                        // This fallback view is complex to calculate scale for.
                        // Since the main path is the FaceAnalysisScreen, we can simplify this.
                        // For now, we don't draw the overlay here to avoid calculation complexity.
                        // The main analysis screen handles the overlay correctly.

                        // 添加调试信息显示
                        if (capturedFaceMeshes.isNotEmpty()) {
                            Text(
                                text = "检测到 ${capturedFaceMeshes.size} 张人脸\n图片尺寸: ${capturedImageWidth}x${capturedImageHeight}\n从相册加载显示",
                                color = Color.Yellow,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(8.dp)
                            )
                        }
                    }
                }
                // 提供返回按钮
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // 重置状态以返回相机
                            showCamera = true
                            capturedImageUri = null
                            capturedBitmap = null
                            capturedFaceMeshes = emptyList()
                            analysisResults = emptyList()
                        }
                    ) {
                        Text("返回摄像头")
                    }

                    // 添加测试按钮
                    Button(
                        onClick = {
                            // 强制重新检测人脸
                            capturedBitmap?.let { bmp ->
                                Log.d("FaceDetectionApp", "Manual face detection triggered")
                                val inputImage = InputImage.fromBitmap(bmp, 0)
                                faceDetectorManager.detectFaces(
                                    inputImage = inputImage,
                                    onSuccess = { detectedFaceMeshes, width, height ->
                                        capturedFaceMeshes = detectedFaceMeshes
                                        capturedImageWidth = width
                                        capturedImageHeight = height
                                        Log.d("FaceDetectionApp", "Manual detection: ${detectedFaceMeshes.size} faces found")
                                        if (detectedFaceMeshes.isNotEmpty()) {
                                            analysisResults = faceAnalyzer.analyze(detectedFaceMeshes.first())
                                            showAnalysisScreen = true
                                        }
                                    },
                                    onFailure = { e ->
                                        Log.e("FaceDetectionApp", "Manual face detection failed", e)
                                    }
                                )
                            }
                        }
                    ) {
                        Text("重新检测")
                    }
                }
            }
        }

        // 人脸检测覆盖层 (仅在相机预览时显示)
        if (showCamera) {
            // For the live camera preview, we also need to calculate the scale and offset.
            // We'll assume the CameraView (which contains the PreviewView) fills the whole screen for this calculation.
            BoxWithConstraints {
                if (imageWidth > 0 && imageHeight > 0) {
                    val viewWidth = constraints.maxWidth.toFloat()
                    val viewHeight = constraints.maxHeight.toFloat()

                    val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
                    val viewAspectRatio = viewWidth / viewHeight

                    val scale: Float
                    val offsetX: Float
                    val offsetY: Float

                    if (imageAspectRatio > viewAspectRatio) {
                        scale = viewHeight / imageHeight.toFloat()
                        val scaledImageWidth = imageWidth * scale
                        offsetX = (viewWidth - scaledImageWidth) / 2f
                        offsetY = 0f
                    } else {
                        scale = viewWidth / imageWidth.toFloat()
                        val scaledImageHeight = imageHeight * scale
                        offsetX = 0f
                        offsetY = (viewHeight - scaledImageHeight) / 2f
                    }

                    FaceOverlay(
                        faceMeshes = faceMeshes,
                        scaleX = scale,
                        scaleY = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        isBackCamera = isBackCamera,
                        isPreviewMode = true,
                        show3DPoints = true
                    )
                }
            }
        }

        if (showPasswordDialog) {
            var password by remember { mutableStateOf("") }
            var showError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                title = { Text("请输入密码") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            visualTransformation = PasswordVisualTransformation(),
                            isError = showError
                        )
                        if (showError) {
                            Text("密码错误", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val sharedPreferences = context.getSharedPreferences("face_app_settings", Context.MODE_PRIVATE)
                            val savedPassword = sharedPreferences.getString("settings_password", "123456") ?: "123456"
                            if (password == savedPassword) {
                                showPasswordDialog = false
                                showSettingsScreen = true
                            } else {
                                showError = true
                            }
                        }
                    ) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    Button(onClick = { showPasswordDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
